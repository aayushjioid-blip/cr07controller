package com.controller.cricket07.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import com.controller.cricket07.model.ControllerButton
import com.controller.cricket07.model.ControllerPreset
import com.controller.cricket07.model.PresetRepository
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.hypot

/**
 * Pure AMOLED Black Surface with floating semi-transparent circular buttons.
 * Thread-safe multi-touch pointer tracking.
 */
class ControllerCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var activePreset: ControllerPreset = PresetRepository.CRICKET_07_PRESET

    // Thread-safe pointer tracking
    private val activePointerMap = ConcurrentHashMap<Int, ControllerButton>()

    private val buttonNormalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(45, 255, 255, 255)
        style = Paint.Style.FILL
    }

    private val buttonPressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(130, 255, 255, 255)
        style = Paint.Style.FILL
    }

    private val borderNormalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(90, 200, 200, 200)
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
        textSize = 40f
        isFakeBoldText = true
    }

    private val subLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(160, 200, 200, 200)
        textAlign = Paint.Align.CENTER
        textSize = 18f
    }

    var onStateChanged: ((modifierMask: Byte, pressedKeys: Set<Byte>) -> Unit)? = null

    fun setPreset(preset: ControllerPreset) {
        activePreset = preset
        activePointerMap.clear()
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK)

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        if (viewWidth <= 0 || viewHeight <= 0) return

        val pressedSet = activePointerMap.values.map { it.id }.toSet()

        for (btn in activePreset.buttons) {
            val cx = btn.xPercent * viewWidth
            val cy = btn.yPercent * viewHeight
            val radius = dpToPx(btn.radiusDp)

            val isPressed = pressedSet.contains(btn.id)

            canvas.drawCircle(cx, cy, radius, if (isPressed) buttonPressedPaint else buttonNormalPaint)
            canvas.drawCircle(cx, cy, radius, if (isPressed) borderPressedPaint else borderNormalPaint)

            val textY = cy - ((labelPaint.descent() + labelPaint.ascent()) / 2)
            canvas.drawText(btn.label, cx, textY, labelPaint)

            if (btn.subLabel.isNotEmpty() && radius >= dpToPx(30f)) {
                canvas.drawText(btn.subLabel, cx, cy + radius + dpToPx(14f), subLabelPaint)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        try {
            val actionMasked = event.actionMasked
            val actionIndex = event.actionIndex

            when (actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    val pointerId = event.getPointerId(actionIndex)
                    val x = event.getX(actionIndex)
                    val y = event.getY(actionIndex)
                    findButtonAt(x, y)?.let { button ->
                        activePointerMap[pointerId] = button
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        dispatchHidUpdate()
                        postInvalidate()
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    var changed = false
                    for (i in 0 until event.pointerCount) {
                        val pointerId = event.getPointerId(i)
                        val x = event.getX(i)
                        val y = event.getY(i)
                        val currentButton = activePointerMap[pointerId]
                        val newButton = findButtonAt(x, y)

                        if (currentButton != newButton) {
                            if (newButton != null) {
                                activePointerMap[pointerId] = newButton
                            } else {
                                activePointerMap.remove(pointerId)
                            }
                            changed = true
                        }
                    }
                    if (changed) {
                        dispatchHidUpdate()
                        postInvalidate()
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    val pointerId = event.getPointerId(actionIndex)
                    if (activePointerMap.remove(pointerId) != null) {
                        dispatchHidUpdate()
                        postInvalidate()
                    }
                }

                MotionEvent.ACTION_CANCEL -> {
                    activePointerMap.clear()
                    dispatchHidUpdate()
                    postInvalidate()
                }
            }
        } catch (_: Exception) {
            // Guard against rare pointer-index out-of-range edge cases
        }
        return true
    }

    private fun findButtonAt(x: Float, y: Float): ControllerButton? {
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        if (viewWidth <= 0 || viewHeight <= 0) return null

        for (btn in activePreset.buttons) {
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

        for (btn in activePointerMap.values) {
            if (btn.isModifier) {
                modifierMask = (modifierMask.toInt() or btn.modifierMask.toInt()).toByte()
            } else if (btn.hidKeyCode != 0.toByte()) {
                pressedKeys.add(btn.hidKeyCode)
            }
        }

        onStateChanged?.invoke(modifierMask, pressedKeys)
    }

    private fun dpToPx(dp: Float): Float = dp * resources.displayMetrics.density
}
