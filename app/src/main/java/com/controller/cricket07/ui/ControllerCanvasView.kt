package com.controller.cricket07.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.controller.cricket07.model.ControllerButton
import com.controller.cricket07.model.ControllerPreset
import com.controller.cricket07.model.PresetRepository
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

/**
 * High-performance AMOLED Gamepad & Keyboard Canvas:
 * 1. 8-Way 360° Analog Thumbstick Dial (supports full analog axes + cardinal/diagonal shots)
 * 2. Multi-touch floating buttons matching EA Sports Cricket 07 and Gamepad configurations
 * 3. Simultaneous Gamepad DirectInput reporting & Boot Keyboard reporting
 * 4. Interactive Drag-and-Drop + Pinch-to-Resize layout editor with persistent coordinates in SharedPreferences
 */
class ControllerCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val prefs: SharedPreferences = context.getSharedPreferences("controller_layouts", Context.MODE_PRIVATE)

    var activePreset: ControllerPreset = PresetRepository.createCricket07Preset()
        private set

    // Edit Mode state
    var isEditMode: Boolean = false
        set(value) {
            field = value
            activePointerMap.clear()
            joystickPointerId = null
            draggedButton = null
            isDraggingJoystick = false
            editPointerId = null
            knobOffsetX = 0f
            knobOffsetY = 0f
            activeJoystickKeys.clear()
            activeHatSwitch = 0
            if (!value) {
                dispatchHidUpdate()
            }
            postInvalidate()
        }

    // Drag-and-Drop & Pinch-Resize tracking in Edit Mode
    private var draggedButton: ControllerButton? = null
    private var isDraggingJoystick: Boolean = false
    private var editPointerId: Int? = null

    // Analog Joystick Configuration
    var joystickBaseRadiusDp = 62f
    val defaultJoystickBaseRadiusDp = 62f
    private val joystickKnobRadiusDp = 30f

    private var joystickPointerId: Int? = null
    private var knobOffsetX = 0f
    private var knobOffsetY = 0f
    private var activeHatSwitch: Byte = 8.toByte() // 8 = Null State (Released/Centered)
    private val activeJoystickKeys = HashSet<Byte>()

    private val activePointerMap = ConcurrentHashMap<Int, ControllerButton>()

    // Pinch-to-Resize Detector for Edit Mode
    private val scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (!isEditMode) return false
            val factor = detector.scaleFactor
            if (factor <= 0.01f || factor >= 10f) return false

            draggedButton?.let { btn ->
                val newRadius = (btn.radiusDp * factor).coerceIn(16f, 65f)
                btn.radiusDp = newRadius
                postInvalidate()
                return true
            }

            if (isDraggingJoystick && activePreset.hasAnalogStick) {
                joystickBaseRadiusDp = (joystickBaseRadiusDp * factor).coerceIn(40f, 110f)
                postInvalidate()
                return true
            }

            // Target by focus point if not already tracked
            val fx = detector.focusX
            val fy = detector.focusY
            findButtonAt(fx, fy)?.let { btn ->
                draggedButton = btn
                val newRadius = (btn.radiusDp * factor).coerceIn(16f, 65f)
                btn.radiusDp = newRadius
                postInvalidate()
                return true
            }

            if (activePreset.hasAnalogStick) {
                val jBaseX = activePreset.joystickXPercent * width
                val jBaseY = activePreset.joystickYPercent * height
                val jRadius = dpToPx(joystickBaseRadiusDp) * 1.5f
                if (hypot(fx - jBaseX, fy - jBaseY) <= jRadius) {
                    isDraggingJoystick = true
                    joystickBaseRadiusDp = (joystickBaseRadiusDp * factor).coerceIn(40f, 110f)
                    postInvalidate()
                    return true
                }
            }

            return false
        }
    })

    // Paints
    private val buttonNormalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(45, 255, 255, 255)
        style = Paint.Style.FILL
    }

    private val buttonPressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(130, 255, 255, 255)
        style = Paint.Style.FILL
    }

    private val borderNormalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(85, 200, 200, 200)
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val borderPressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    private val borderEditPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF") // Vibrant Cyan for Edit Mode
        style = Paint.Style.STROKE
        strokeWidth = 4f
        pathEffect = DashPathEffect(floatArrayOf(12f, 10f), 0f)
    }

    private val editGuidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(70, 0, 229, 255)
        style = Paint.Style.STROKE
        strokeWidth = 2f
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 34f
        isFakeBoldText = true
    }

    private val subLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B0BEC5")
        textAlign = Paint.Align.CENTER
        textSize = 20f
    }

    private val editCoordPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF")
        textAlign = Paint.Align.CENTER
        textSize = 24f
        isFakeBoldText = true
    }

    private val joystickBasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(35, 255, 255, 255)
        style = Paint.Style.FILL
    }

    private val joystickBaseBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(70, 200, 200, 200)
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val joystickKnobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(110, 255, 255, 255)
        style = Paint.Style.FILL
    }

    private val joystickKnobActivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(160, 255, 255, 255)
        style = Paint.Style.FILL
    }

    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(160, 220, 220, 220)
        style = Paint.Style.FILL
    }

    // Callbacks for HID Transmission
    var onGamepadStateChanged: ((leftStickX: Byte, leftStickY: Byte, hatSwitch: Byte, buttonsMask: Short) -> Unit)? = null
    var onKeyboardStateChanged: ((modifierMask: Byte, pressedKeys: Set<Byte>) -> Unit)? = null

    init {
        loadCustomPositions()
    }

    fun setPreset(preset: ControllerPreset) {
        activePreset = preset
        joystickBaseRadiusDp = defaultJoystickBaseRadiusDp
        loadCustomPositions()
        activePointerMap.clear()
        joystickPointerId = null
        draggedButton = null
        isDraggingJoystick = false
        editPointerId = null
        knobOffsetX = 0f
        knobOffsetY = 0f
        activeJoystickKeys.clear()
        activeHatSwitch = 0
        dispatchHidUpdate()
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK)

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        if (viewWidth <= 0 || viewHeight <= 0) return

        // 1. Draw Analog Joystick (if enabled for this preset)
        if (activePreset.hasAnalogStick) {
            drawAnalogJoystick(canvas, viewWidth, viewHeight)
        }

        // 2. Draw Floating Circular Buttons
        val pressedSet = activePointerMap.values.map { it.id }.toSet()

        for (btn in activePreset.buttons) {
            val cx = btn.xPercent * viewWidth
            val cy = btn.yPercent * viewHeight
            val radius = dpToPx(btn.radiusDp)

            val isSelectedInEdit = isEditMode && draggedButton?.id == btn.id
            val isPressed = pressedSet.contains(btn.id) || isSelectedInEdit

            canvas.drawCircle(cx, cy, radius, if (isPressed) buttonPressedPaint else buttonNormalPaint)

            // Draw border: cyan dashed if in edit mode, solid otherwise
            if (isEditMode) {
                canvas.drawCircle(cx, cy, radius + dpToPx(3f), borderEditPaint)
                if (isSelectedInEdit) {
                    // Draw alignment crosshairs in Edit Mode
                    canvas.drawLine(0f, cy, viewWidth, cy, editGuidePaint)
                    canvas.drawLine(cx, 0f, cx, viewHeight, editGuidePaint)
                    // Draw coordinates and size indicator
                    val coordText = "${(btn.xPercent * 100).toInt()}%, ${(btn.yPercent * 100).toInt()}% • Size: ${btn.radiusDp.toInt()}dp"
                    canvas.drawText(coordText, cx, cy + radius + dpToPx(16f), editCoordPaint)
                }
            } else {
                canvas.drawCircle(cx, cy, radius, if (isPressed) borderPressedPaint else borderNormalPaint)
            }

            // Draw Labels (centered with room for sub-label)
            if (btn.subLabel.isNotEmpty() && !isEditMode) {
                val textY = cy - dpToPx(3f)
                canvas.drawText(btn.label, cx, textY, labelPaint)
                canvas.drawText(btn.subLabel, cx, textY + dpToPx(18f), subLabelPaint)
            } else {
                val textY = cy - ((labelPaint.descent() + labelPaint.ascent()) / 2)
                canvas.drawText(btn.label, cx, textY, labelPaint)
            }
        }
    }

    private fun drawAnalogJoystick(canvas: Canvas, viewWidth: Float, viewHeight: Float) {
        val baseX = activePreset.joystickXPercent * viewWidth
        val baseY = activePreset.joystickYPercent * viewHeight
        val baseRadius = dpToPx(joystickBaseRadiusDp)
        val knobRadius = dpToPx(joystickKnobRadiusDp)

        // Outer base
        canvas.drawCircle(baseX, baseY, baseRadius, joystickBasePaint)

        if (isEditMode) {
            canvas.drawCircle(baseX, baseY, baseRadius + dpToPx(4f), borderEditPaint)
            if (isDraggingJoystick) {
                canvas.drawLine(0f, baseY, viewWidth, baseY, editGuidePaint)
                canvas.drawLine(baseX, 0f, baseX, viewHeight, editGuidePaint)
                val coordText = "DIAL: ${(activePreset.joystickXPercent * 100).toInt()}%, ${(activePreset.joystickYPercent * 100).toInt()}% • Size: ${joystickBaseRadiusDp.toInt()}dp"
                canvas.drawText(coordText, baseX, baseY + baseRadius + dpToPx(20f), editCoordPaint)
            }
        } else {
            canvas.drawCircle(baseX, baseY, baseRadius, joystickBaseBorderPaint)
        }

        // 4 Directional Arrows
        val arrowSize = dpToPx(9f)
        val arrowDistance = baseRadius + dpToPx(8f)
        drawTriangle(canvas, baseX, baseY - arrowDistance, arrowSize, 0f)
        drawTriangle(canvas, baseX, baseY + arrowDistance, arrowSize, 180f)
        drawTriangle(canvas, baseX - arrowDistance, baseY, arrowSize, 270f)
        drawTriangle(canvas, baseX + arrowDistance, baseY, arrowSize, 90f)

        // Inner knob
        val knobX = baseX + (if (isEditMode) 0f else knobOffsetX)
        val knobY = baseY + (if (isEditMode) 0f else knobOffsetY)
        val isActive = joystickPointerId != null || (isEditMode && isDraggingJoystick)

        canvas.drawCircle(knobX, knobY, knobRadius, if (isActive) joystickKnobActivePaint else joystickKnobPaint)
        canvas.drawCircle(knobX, knobY, knobRadius, if (isActive) borderPressedPaint else borderNormalPaint)
        canvas.drawCircle(knobX, knobY, knobRadius * 0.45f, borderNormalPaint)

        // Joystick center label (e.g. SHOT, WASD, D-PAD)
        val textY = knobY - ((subLabelPaint.descent() + subLabelPaint.ascent()) / 2)
        canvas.drawText(activePreset.joystickLabel, knobX, textY, subLabelPaint)
    }

    private fun drawTriangle(canvas: Canvas, x: Float, y: Float, size: Float, rotationDegrees: Float) {
        val path = Path()
        path.moveTo(0f, -size)
        path.lineTo(-size * 0.7f, size * 0.7f)
        path.lineTo(size * 0.7f, size * 0.7f)
        path.close()

        canvas.save()
        canvas.translate(x, y)
        canvas.rotate(rotationDegrees)
        canvas.drawPath(path, arrowPaint)
        canvas.restore()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        try {
            val actionMasked = event.actionMasked
            val actionIndex = event.actionIndex
            val pointerId = event.getPointerId(actionIndex)

            val viewWidth = width.toFloat()
            val viewHeight = height.toFloat()

            // ==========================================
            // EDIT MODE: Smooth Drag & Drop Repositioning & Pinch-to-Resize
            // ==========================================
            if (isEditMode) {
                // Route to pinch-to-resize gesture detector
                scaleGestureDetector.onTouchEvent(event)

                if (scaleGestureDetector.isInProgress || event.pointerCount > 1) {
                    return true
                }

                when (actionMasked) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                        if (editPointerId == null) {
                            val x = event.getX(actionIndex)
                            val y = event.getY(actionIndex)

                            // 1. Check Joystick Base hit
                            if (activePreset.hasAnalogStick) {
                                val jBaseX = activePreset.joystickXPercent * viewWidth
                                val jBaseY = activePreset.joystickYPercent * viewHeight
                                val jRadius = dpToPx(joystickBaseRadiusDp) * 1.5f
                                if (hypot(x - jBaseX, y - jBaseY) <= jRadius) {
                                    isDraggingJoystick = true
                                    editPointerId = pointerId
                                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    postInvalidate()
                                    return true
                                }
                            }

                            // 2. Check Buttons hit
                            findButtonAt(x, y)?.let { button ->
                                draggedButton = button
                                editPointerId = pointerId
                                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                postInvalidate()
                                return true
                            }
                        }
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val activeId = editPointerId ?: return true
                        val pIndex = event.findPointerIndex(activeId)
                        if (pIndex >= 0) {
                            val x = event.getX(pIndex)
                            val y = event.getY(pIndex)

                            if (isDraggingJoystick && activePreset.hasAnalogStick) {
                                activePreset.joystickXPercent = (x / viewWidth).coerceIn(0.08f, 0.92f)
                                activePreset.joystickYPercent = (y / viewHeight).coerceIn(0.12f, 0.88f)
                                postInvalidate()
                            } else {
                                draggedButton?.let { button ->
                                    button.xPercent = (x / viewWidth).coerceIn(0.04f, 0.96f)
                                    button.yPercent = (y / viewHeight).coerceIn(0.12f, 0.88f)
                                    postInvalidate()
                                }
                            }
                        }
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                        if (pointerId == editPointerId) {
                            draggedButton = null
                            isDraggingJoystick = false
                            editPointerId = null
                            postInvalidate()
                        }
                    }

                    MotionEvent.ACTION_CANCEL -> {
                        draggedButton = null
                        isDraggingJoystick = false
                        editPointerId = null
                        postInvalidate()
                    }
                }
                return true
            }

            // ==========================================
            // PLAY MODE: Real-time Multi-Touch Controller
            // ==========================================
            val jBaseX = activePreset.joystickXPercent * viewWidth
            val jBaseY = activePreset.joystickYPercent * viewHeight
            val jBaseRadius = dpToPx(joystickBaseRadiusDp)

            when (actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    val x = event.getX(actionIndex)
                    val y = event.getY(actionIndex)

                    if (activePreset.hasAnalogStick && hypot(x - jBaseX, y - jBaseY) <= jBaseRadius * 1.6f && joystickPointerId == null) {
                        joystickPointerId = pointerId
                        updateJoystickPosition(x - jBaseX, y - jBaseY, jBaseRadius)
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    } else {
                        findButtonAt(x, y)?.let { button ->
                            activePointerMap[pointerId] = button
                            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            dispatchHidUpdate()
                        }
                    }
                    postInvalidate()
                }

                MotionEvent.ACTION_MOVE -> {
                    var changed = false
                    for (i in 0 until event.pointerCount) {
                        val pId = event.getPointerId(i)
                        val x = event.getX(i)
                        val y = event.getY(i)

                        if (pId == joystickPointerId && activePreset.hasAnalogStick) {
                            updateJoystickPosition(x - jBaseX, y - jBaseY, jBaseRadius)
                            changed = true
                        } else {
                            val currentButton = activePointerMap[pId]
                            val newButton = findButtonAt(x, y)
                            if (currentButton != newButton) {
                                if (newButton != null) {
                                    activePointerMap[pId] = newButton
                                } else {
                                    activePointerMap.remove(pId)
                                }
                                changed = true
                            }
                        }
                    }
                    if (changed) {
                        dispatchHidUpdate()
                        postInvalidate()
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    if (pointerId == joystickPointerId) {
                        joystickPointerId = null
                        knobOffsetX = 0f
                        knobOffsetY = 0f
                        activeJoystickKeys.clear()
                        activeHatSwitch = 8.toByte()
                        dispatchHidUpdate()
                    } else {
                        activePointerMap.remove(pointerId)
                        dispatchHidUpdate()
                    }
                    postInvalidate()
                }

                MotionEvent.ACTION_CANCEL -> {
                    joystickPointerId = null
                    knobOffsetX = 0f
                    knobOffsetY = 0f
                    activeJoystickKeys.clear()
                    activeHatSwitch = 8.toByte()
                    activePointerMap.clear()
                    dispatchHidUpdate()
                    postInvalidate()
                }
            }
        } catch (_: Exception) {}
        return true
    }

    private fun updateJoystickPosition(dx: Float, dy: Float, maxRadius: Float) {
        val distance = hypot(dx, dy)
        val clampedDistance = min(distance, maxRadius)

        if (distance > 0) {
            knobOffsetX = (dx / distance) * clampedDistance
            knobOffsetY = (dy / distance) * clampedDistance
        } else {
            knobOffsetX = 0f
            knobOffsetY = 0f
        }

        activeJoystickKeys.clear()

        val deadzone = maxRadius * 0.20f
        if (distance >= deadzone) {
            val angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

            val kUp = activePreset.joystickKeyUp
            val kDown = activePreset.joystickKeyDown
            val kLeft = activePreset.joystickKeyLeft
            val kRight = activePreset.joystickKeyRight

            when {
                angleDeg in -157.5f..-112.5f -> {
                    // UP + LEFT
                    activeJoystickKeys.add(kUp)
                    activeJoystickKeys.add(kLeft)
                    activeHatSwitch = 7.toByte() // 315 deg (Up-Left)
                }
                angleDeg in -112.5f..-67.5f -> {
                    // UP
                    activeJoystickKeys.add(kUp)
                    activeHatSwitch = 0.toByte() // 0 deg (Up)
                }
                angleDeg in -67.5f..-22.5f -> {
                    // UP + RIGHT
                    activeJoystickKeys.add(kUp)
                    activeJoystickKeys.add(kRight)
                    activeHatSwitch = 1.toByte() // 45 deg (Up-Right)
                }
                angleDeg in -22.5f..22.5f -> {
                    // RIGHT
                    activeJoystickKeys.add(kRight)
                    activeHatSwitch = 2.toByte() // 90 deg (Right)
                }
                angleDeg in 22.5f..67.5f -> {
                    // DOWN + RIGHT
                    activeJoystickKeys.add(kDown)
                    activeJoystickKeys.add(kRight)
                    activeHatSwitch = 3.toByte() // 135 deg (Down-Right)
                }
                angleDeg in 67.5f..112.5f -> {
                    // DOWN
                    activeJoystickKeys.add(kDown)
                    activeHatSwitch = 4.toByte() // 180 deg (Down)
                }
                angleDeg in 112.5f..157.5f -> {
                    // DOWN + LEFT
                    activeJoystickKeys.add(kDown)
                    activeJoystickKeys.add(kLeft)
                    activeHatSwitch = 5.toByte() // 225 deg (Down-Left)
                }
                else -> {
                    // LEFT
                    activeJoystickKeys.add(kLeft)
                    activeHatSwitch = 6.toByte() // 270 deg (Left)
                }
            }
        } else {
            activeHatSwitch = 8.toByte() // 8 = Null State (Released/Centered)
        }
        dispatchHidUpdate()
    }

    private fun findButtonAt(x: Float, y: Float): ControllerButton? {
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        if (viewWidth <= 0 || viewHeight <= 0) return null

        var closestBtn: ControllerButton? = null
        var minDistance = Float.MAX_VALUE

        for (btn in activePreset.buttons) {
            val cx = btn.xPercent * viewWidth
            val cy = btn.yPercent * viewHeight
            val radius = dpToPx(btn.radiusDp) * (if (isEditMode) 1.5f else 1.3f)
            val dist = hypot(x - cx, y - cy)
            if (dist <= radius && dist < minDistance) {
                minDistance = dist
                closestBtn = btn
            }
        }
        return closestBtn
    }

    private fun dispatchHidUpdate() {
        if (isEditMode) return

        // 1. GAMEPAD REPORT COMPUTATION (Report ID 2)
        val maxRadius = dpToPx(joystickBaseRadiusDp)
        val normX = if (maxRadius > 0) (knobOffsetX / maxRadius).coerceIn(-1f, 1f) else 0f
        val normY = if (maxRadius > 0) (knobOffsetY / maxRadius).coerceIn(-1f, 1f) else 0f

        val stickX = (128 + (normX * 127)).toInt().coerceIn(0, 255).toByte()
        val stickY = (128 + (normY * 127)).toInt().coerceIn(0, 255).toByte()

        var buttonsMask: Int = 0
        for (btn in activePointerMap.values) {
            for (btnIdx in btn.allGamepadButtonIndices) {
                if (btnIdx in 1..16) {
                    buttonsMask = buttonsMask or (1 shl (btnIdx - 1))
                }
            }
        }
        onGamepadStateChanged?.invoke(stickX, stickY, activeHatSwitch, buttonsMask.toShort())

        // 2. KEYBOARD REPORT COMPUTATION (Report ID 1)
        var modifierMask: Byte = 0
        val pressedKeys = HashSet<Byte>()

        for (btn in activePointerMap.values) {
            if (btn.isModifier) {
                modifierMask = (modifierMask.toInt() or btn.modifierMask.toInt()).toByte()
            } else if (btn.hidKeyCode != 0.toByte()) {
                pressedKeys.add(btn.hidKeyCode)
            }
        }

        pressedKeys.addAll(activeJoystickKeys)
        onKeyboardStateChanged?.invoke(modifierMask, pressedKeys)
    }

    // ==========================================
    // Persistence: Save / Load Custom Positions & Sizes
    // ==========================================
    fun saveCustomPositions() {
        val editor = prefs.edit()
        val prefix = activePreset.id

        for (btn in activePreset.buttons) {
            editor.putFloat("${prefix}_${btn.id}_x", btn.xPercent)
            editor.putFloat("${prefix}_${btn.id}_y", btn.yPercent)
            editor.putFloat("${prefix}_${btn.id}_radius", btn.radiusDp)
        }

        if (activePreset.hasAnalogStick) {
            editor.putFloat("${prefix}_joystick_x", activePreset.joystickXPercent)
            editor.putFloat("${prefix}_joystick_y", activePreset.joystickYPercent)
            editor.putFloat("${prefix}_joystick_radius", joystickBaseRadiusDp)
        }

        editor.apply()
    }

    private fun loadCustomPositions() {
        val prefix = activePreset.id

        for (btn in activePreset.buttons) {
            val savedX = prefs.getFloat("${prefix}_${btn.id}_x", btn.defaultXPercent)
            val savedY = prefs.getFloat("${prefix}_${btn.id}_y", btn.defaultYPercent)
            val savedRadius = prefs.getFloat("${prefix}_${btn.id}_radius", btn.defaultRadiusDp)
            btn.xPercent = savedX
            btn.yPercent = savedY
            btn.radiusDp = savedRadius
        }

        if (activePreset.hasAnalogStick) {
            activePreset.joystickXPercent = prefs.getFloat("${prefix}_joystick_x", activePreset.defaultJoystickX)
            activePreset.joystickYPercent = prefs.getFloat("${prefix}_joystick_y", activePreset.defaultJoystickY)
            joystickBaseRadiusDp = prefs.getFloat("${prefix}_joystick_radius", defaultJoystickBaseRadiusDp)
        }
    }

    fun resetToDefaults() {
        val editor = prefs.edit()
        val prefix = activePreset.id

        for (btn in activePreset.buttons) {
            btn.xPercent = btn.defaultXPercent
            btn.yPercent = btn.defaultYPercent
            btn.radiusDp = btn.defaultRadiusDp
            editor.remove("${prefix}_${btn.id}_x")
            editor.remove("${prefix}_${btn.id}_y")
            editor.remove("${prefix}_${btn.id}_radius")
        }

        if (activePreset.hasAnalogStick) {
            activePreset.joystickXPercent = activePreset.defaultJoystickX
            activePreset.joystickYPercent = activePreset.defaultJoystickY
            joystickBaseRadiusDp = defaultJoystickBaseRadiusDp
            editor.remove("${prefix}_joystick_x")
            editor.remove("${prefix}_joystick_y")
            editor.remove("${prefix}_joystick_radius")
        }

        editor.apply()
        postInvalidate()
    }

    private fun dpToPx(dp: Float): Float = dp * resources.displayMetrics.density
}
