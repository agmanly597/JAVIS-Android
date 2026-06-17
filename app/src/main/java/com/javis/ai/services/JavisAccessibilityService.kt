package com.javis.ai.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class JavisAccessibilityService : AccessibilityService() {

    private var currentPackage: String = ""

    companion object {
        var instance: JavisAccessibilityService? = null
        var onAppChanged: ((String) -> Unit)? = null
        const val TAG = "JavisA11y"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_SCROLLED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE
            notificationTimeout = 100
        }
        Log.d(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString() ?: return
            if (pkg != currentPackage && pkg != "com.javis.ai") {
                currentPackage = pkg
                onAppChanged?.invoke(pkg)
                Log.d(TAG, "Active app: $pkg")
            }
        }
    }

    // ─── Reading ─────────────────────────────────────────────────────────────

    fun getCurrentApp(): String = currentPackage

    fun readVisibleText(): String {
        val root = rootInActiveWindow ?: return ""
        return collectText(root, StringBuilder()).toString().trim().take(500)
    }

    private fun collectText(node: AccessibilityNodeInfo?, sb: StringBuilder): StringBuilder {
        if (node == null) return sb
        val text = node.text?.toString()
        val contentDesc = node.contentDescription?.toString()
        val hint = node.hintText?.toString()
        val value = text ?: contentDesc ?: hint
        if (!value.isNullOrBlank() && value.length > 1) {
            sb.append(value).append(". ")
        }
        for (i in 0 until node.childCount) {
            collectText(node.getChild(i), sb)
        }
        return sb
    }

    // ─── Finding nodes ───────────────────────────────────────────────────────

    fun findNodeWithText(text: String): AccessibilityNodeInfo? =
        rootInActiveWindow?.findAccessibilityNodeInfosByText(text)?.firstOrNull()

    fun findNodeById(viewId: String): AccessibilityNodeInfo? =
        rootInActiveWindow?.findAccessibilityNodeInfosByViewId(viewId)?.firstOrNull()

    fun findEditableNode(): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        return findEditable(root)
    }

    private fun findEditable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val found = findEditable(node.getChild(i))
            if (found != null) return found
        }
        return null
    }

    // ─── Actions ─────────────────────────────────────────────────────────────

    fun clickNode(node: AccessibilityNodeInfo?): Boolean =
        node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false

    fun typeText(node: AccessibilityNodeInfo?, text: String): Boolean {
        if (node == null) return false
        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        val args = android.os.Bundle()
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun typeIntoCurrentField(text: String): Boolean {
        val node = findEditableNode() ?: return false
        return typeText(node, text)
    }

    fun clickOnText(text: String): Boolean {
        val node = findNodeWithText(text) ?: return false
        return clickNode(node)
    }

    // ─── Navigation ──────────────────────────────────────────────────────────

    fun performGlobalBack() = performGlobalAction(GLOBAL_ACTION_BACK)
    fun performGlobalHome() = performGlobalAction(GLOBAL_ACTION_HOME)
    fun performGlobalRecents() = performGlobalAction(GLOBAL_ACTION_RECENTS)
    fun performGlobalNotifications() = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    fun performGlobalQuickSettings() = performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)

    // Screenshot — API 28+
    fun takeScreenshot(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        } else false
    }

    // ─── Scrolling ───────────────────────────────────────────────────────────

    fun performScroll(up: Boolean) {
        val root = rootInActiveWindow ?: return
        val scrollable = findScrollable(root)
        if (scrollable != null) {
            val action = if (up) AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                         else   AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            scrollable.performAction(action)
        } else {
            // Fallback: gesture swipe
            swipeScreen(up)
        }
    }

    private fun findScrollable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val found = findScrollable(node.getChild(i))
            if (found != null) return found
        }
        return null
    }

    private fun swipeScreen(up: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val display = resources.displayMetrics
            val w = display.widthPixels / 2f
            val startY = if (up) display.heightPixels * 0.7f else display.heightPixels * 0.3f
            val endY   = if (up) display.heightPixels * 0.3f else display.heightPixels * 0.7f
            val path = Path().apply {
                moveTo(w, startY)
                lineTo(w, endY)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0, 300)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            dispatchGesture(gesture, null, null)
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
