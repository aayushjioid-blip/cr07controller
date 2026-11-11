package com.controller.cricket07.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import com.controller.cricket07.hid.HidConsts
import com.controller.cricket07.model.ControllerButton
import com.controller.cricket07.model.ControllerPreset
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * Pure AMOLED Black Surface with:
 * 1. 8-Way Analog Thumbstick Dial (for diagonal & cardinal shot selection)
 * 2. Floating semi-transparent circular buttons matching Cricket 07 reference layout.
 */
class ControllerCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Analog Joystick configuration
    private val joystickBaseXPercent = 0.12f
    private val joystickBaseYPercent = 0.72f
    private val joystickBaseRadiusDp = 62f
    private val joystickKnobRadiusDp = 30f

    private var joystickPointerId: Int? = null
    private var knobOffsetX = 0f
    private var knobOffsetY = 0f
    private val activeJoystickKeys = HashSet<Byte>()

    // Standard Buttons (Ctrl, Space, Esc, Enter, Shift, Alt, W, A, S, D)
    private val buttons = listOf(
        // Top Left
        ControllerButton("ctrl", "Ctrl", "", 0.06f, 0.12f, 30f, isModifier = true, modifierMask = HidConsts.MOD_LEFT_CTRL),
        ControllerButton("space", "Space", "", 0.06f, 0.28f, 30f, HidConsts.KEY_SPACE),

        // Top Center
        ControllerButton("esc", "Esc", "", 0.46f, 0.08f, 24f, HidConsts.KEY_ESCAPE),
        ControllerButton("enter", "Enter", "", 0.54f, 0.08f, 24f, HidConsts.KEY_ENTER),

        // Top Right
        ControllerButton("shift", "Shift", "", 0.88f, 0.14f, 38f, isModifier = true, modifierMask = HidConsts.MOD_LEFT_SHIFT),
        ControllerButton("alt", "Alt", "", 0.88f, 0.33f, 28f, isModifier = true, modifierMask = HidConsts.MOD_LEFT_ALT),

        // Action Diamond (Right Thumb)
        ControllerButton("btn_w", "W", "", 0.83f, 0.58f, 30f, HidConsts.KEY_W),
        ControllerButton("btn_a", "A", "", 0.74f, 0.72f, 30f, HidConsts.KEY_A),
        ControllerButton("btn_d", "D", "", 0.92f, 0.72f, 30f, HidConsts.KEY_D),
        ControllerButton("btn_s", "S", "", 0.83f, 0.86f, 30f, HidConsts.KEY_S)
    )

    private val activePointerMap = ConcurrentHashMap<Int, ControllerButton>()

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

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 38f
        isFakeBoldText = true
    }

    // Joystick specific paints
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

    var onStateChanged: ((modifierMask: Byte, pressedKeys: Set<Byte>) -> Unit)? = null

    fun setPreset(preset: ControllerPreset) {
        // Active layout matches Cricket 07 reference design
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK)

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        if (viewWidth <= 0 || viewHeight <= 0) return

        // 1. Draw Analog Joystick Dial
        drawAnalogJoystick(canvas, viewWidth, viewHeight)

        // 2. Draw Floating Circular Buttons
        val pressedSet = activePointerMap.values.map { it.id }.toSet()

        for (btn in buttons) {
            val cx = btn.xPercent * viewWidth
            val cy = btn.yPercent * viewHeight
            val radius = dpToPx(btn.radiusDp)

            val isPressed = pressedSet.contains(btn.id)

            canvas.drawCircle(cx, cy, radius, if (isPressed) buttonPressedPaint else buttonNormalPaint)
            canvas.drawCircle(cx, cy, radius, if (isPressed) borderPressedPaint else borderNormalPaint)

            val textY = cy - ((labelPaint.descent() + labelPaint.ascent()) / 2)
            canvas.drawText(btn.label, cx, textY, labelPaint)
        }
    }

    private fun drawAnalogJoystick(canvas: Canvas, viewWidth: Float, viewHeight: Float) {
        val baseX = joystickBaseXPercent * viewWidth
        val baseY = joystickBaseYPercent * viewHeight
        val baseRadius = dpToPx(joystickBaseRadiusDp)
        val knobRadius = dpToPx(joystickKnobRadiusDp)

        // Outer circular base
        canvas.drawCircle(baseX, baseY, baseRadius, joystickBasePaint)
        canvas.drawCircle(baseX, baseY, baseRadius, joystickBaseBorderPaint)

        // Draw 4 directional arrows around the outer perimeter (Up, Down, Left, Right)
        val arrowSize = dpToPx(9f)
        val arrowDistance = baseRadius + dpToPx(8f)

        // UP Triangle
        drawTriangle(canvas, baseX, baseY - arrowDistance, arrowSize, 0f)
        // DOWN Triangle
        drawTriangle(canvas, baseX, baseY + arrowDistance, arrowSize, 180f)
        // LEFT Triangle
        drawTriangle(canvas, baseX - arrowDistance, baseY, arrowSize, 270f)
        // RIGHT Triangle
        drawTriangle(canvas, baseX + arrowDistance, baseY, arrowSize, 90f)

        // Inner movable knob
        val knobX = baseX + knobOffsetX
        val knobY = baseY + knobOffsetY
        val isActive = joystickPointerId != null

        canvas.drawCircle(knobX, knobY, knobRadius, if (isActive) joystickKnobActivePaint else joystickKnobPaint)
        canvas.drawCircle(knobX, knobY, knobRadius, if (isActive) borderPressedPaint else borderNormalPaint)

        // Center ring inside knob
        canvas.drawCircle(knobX, knobY, knobRadius * 0.45f, borderNormalPaint)
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
            val jBaseX = joystickBaseXPercent * viewWidth
            val jBaseY = joystickBaseYPercent * viewHeight
            val jBaseRadius = dpToPx(joystickBaseRadiusDp)

            when (actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    val x = event.getX(actionIndex)
                    val y = event.getY(actionIndex)

                    // Check if touched the Analog Joystick dial
                    if (hypot(x - jBaseX, y - jBaseY) <= jBaseRadius * 1.6f && joystickPointerId == null) {
                        joystickPointerId = pointerId
                        updateJoystickPosition(x - jBaseX, y - jBaseY, jBaseRadius)
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    } else {
                        // Check standard buttons
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

                        if (pId == joystickPointerId) {
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
                    activePointerMap.clear()
                    dispatchHidUpdate()
                    postInvalidate()
                }
            }
        } catch (_: Exception) {}
        return true
    }

    /**
     * Maps 360-degree analog drag to 8 directional sectors:
     * - Cardinals: Up, Down, Left, Right
     * - Diagonals: Up-Left, Up-Right, Down-Left, Down-Right
     */
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

        // 25% deadzone for neutral position
        val deadzone = maxRadius * 0.25f
        if (distance >= deadzone) {
            // Angle in degrees from -180 to +180
            val angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

            // 8 Sectors (each sector 45 degrees):
            // Up-Left: -157.5° to -112.5°
            // Up: -112.5° to -67.5°
            // Up-Right: -67.5° to -22.5°
            // Right: -22.5° to +22.5°
            // Down-Right: +22.5° to +67.5°
            // Down: +67.5° to +112.5°
            // Down-Left: +112.5° to +157.5°
            // Left: abs(angle) > 157.5°

            when {
                angleDeg in -157.5f..-112.5f -> {
                    // DIAGONAL: UP + LEFT (Cover Drive / Off-side advance)
                    activeJoystickKeys.add(HidConsts.KEY_UP_ARROW)
                    activeJoystickKeys.add(HidConsts.KEY_LEFT_ARROW)
                }
                angleDeg in -112.5f..-67.5f -> {
                    // STRAIGHT: UP
                    activeJoystickKeys.add(HidConsts.KEY_UP_ARROW)
                }
                angleDeg in -67.5f..-22.5f -> {
                    // DIAGONAL: UP + RIGHT (On Drive / Midwicket loft)
                    activeJoystickKeys.add(HidConsts.KEY_UP_ARROW)
                    activeJoystickKeys.add(HidConsts.KEY_RIGHT_ARROW)
                }
                angleDeg in -22.5f..22.5f -> {
                    // STRAIGHT: RIGHT
                    activeJoystickKeys.add(HidConsts.KEY_RIGHT_ARROW)
                }
                angleDeg in 22.5f..67.5f -> {
                    // DIAGONAL: DOWN + RIGHT (Leg Glance / Fine Leg / Hook)
                    activeJoystickKeys.add(HidConsts.KEY_DOWN_ARROW)
                    activeJoystickKeys.add(HidConsts.KEY_RIGHT_ARROW)
                }
                angleDeg in 67.5f..112.5f -> {
                    // STRAIGHT: DOWN
                    activeJoystickKeys.add(HidConsts.KEY_DOWN_ARROW)
                }
                angleDeg in 112.5f..157.5f -> {
                    // DIAGONAL: DOWN + LEFT (Late Cut / Backward Point)
                    activeJoystickKeys.add(HidConsts.KEY_DOWN_ARROW)
                    activeJoystickKeys.add(HidConsts.KEY_LEFT_ARROW)
                }
                else -> {
                    // STRAIGHT: LEFT
                    activeJoystickKeys.add(HidConsts.KEY_LEFT_ARROW)
                }
            }
        }
        dispatchHidUpdate()
    }

    private fun findButtonAt(x: Float, y: Float): ControllerButton? {
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        if (viewWidth <= 0 || viewHeight <= 0) return null

        for (btn in buttons) {
            val cx = btn.xPercent * viewWidth
            val cy = btn.yPercent * viewHeight
            val radius = dpToPx(btn.radiusDp) * 1.25f
            if (hypot(x - cx, y - cy) <= radius) {
                return btn
            }
        }
        return null
    }

    private fun dispatchHidUpdate() {
        var modifierMask: Byte = 0
        val pressedKeys = HashSet<Byte>()

        // 1. Collect standard buttons
        for (btn in activePointerMap.values) {
            if (btn.isModifier) {
                modifierMask = (modifierMask.toInt() or btn.modifierMask.toInt()).toByte()
            } else if (btn.hidKeyCode != 0.toByte()) {
                pressedKeys.add(btn.hidKeyCode)
            }
        }

        // 2. Collect 8-way joystick directional keys (Up, Down, Left, Right or simultaneous diagonals!)
        pressedKeys.addAll(activeJoystickKeys)

        onStateChanged?.invoke(modifierMask, pressedKeys)
    }

    private fun dpToPx(dp: Float): Float = dp * resources.displayMetrics.density
}
