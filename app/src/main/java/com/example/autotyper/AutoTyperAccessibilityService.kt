package com.example.autotyper

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AutoTyperAccessibilityService : AccessibilityService() {

    companion object {
        var instance: AutoTyperAccessibilityService? = null
    }

    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        handler.removeCallbacksAndMessages(null)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    fun generateCombos(maxValue: Int = 9): List<String> {
        val result = mutableListOf<String>()
        for (value in 2..maxValue) {
            for (pos in 1..5) {
                val digits = IntArray(6) { 1 }
                digits[pos] = value
                result.add(digits.joinToString(""))
            }
        }
        return result
    }

    fun startSequence(
        x: Int,
        y: Int,
        startDelayMs: Long,
        afterTypeDelayMs: Long,
        afterTapDelayMs: Long,
        maxValue: Int
    ) {
        if (isRunning) return
        isRunning = true
        val combos = generateCombos(maxValue)
        handler.postDelayed({
            processCombo(combos, 0, x, y, afterTypeDelayMs, afterTapDelayMs)
        }, startDelayMs)
    }

    fun stopSequence() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
    }

    fun isSequenceRunning(): Boolean = isRunning

    private fun processCombo(
        combos: List<String>,
        index: Int,
        x: Int,
        y: Int,
        afterTypeDelayMs: Long,
        afterTapDelayMs: Long
    ) {
        if (!isRunning || index >= combos.size) {
            isRunning = false
            return
        }

        val ok = setTextInFocusedField(combos[index])
        if (!ok) {
            isRunning = false
            return
        }

        handler.postDelayed({
            dispatchTap(x, y) {
                handler.postDelayed({
                    processCombo(combos, index + 1, x, y, afterTypeDelayMs, afterTapDelayMs)
                }, afterTapDelayMs)
            }
        }, afterTypeDelayMs)
    }

    private fun setTextInFocusedField(text: String): Boolean {
        val node = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        val args = Bundle()
        args.putCharSequence(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
            text
        )
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    private fun dispatchTap(x: Int, y: Int, onDone: () -> Unit) {
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        val dispatched = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                onDone()
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                onDone()
            }
        }, null)
        if (!dispatched) onDone()
    }
}
