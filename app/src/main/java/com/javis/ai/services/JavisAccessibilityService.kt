package com.javis.ai.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class JavisAccessibilityService : AccessibilityService() {

    private var currentPackage: String = ""

    companion object {
        var instance: JavisAccessibilityService? = null
        var onAppChanged: ((String) -> Unit)? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
        }
        Log.d("JavisAccessibility", "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString() ?: return
            if (pkg != currentPackage && pkg != "com.javis.ai") {
                currentPackage = pkg
                onAppChanged?.invoke(pkg)
                Log.d("JavisAccessibility", "Active app changed to: $pkg")
            }
        }
    }

    fun getCurrentApp(): String = currentPackage

    fun findNodeWithText(text: String): AccessibilityNodeInfo? {
        return rootInActiveWindow?.findAccessibilityNodeInfosByText(text)?.firstOrNull()
    }

    fun findNodeById(viewId: String): AccessibilityNodeInfo? {
        return rootInActiveWindow?.findAccessibilityNodeInfosByViewId(viewId)?.firstOrNull()
    }

    fun clickNode(node: AccessibilityNodeInfo?): Boolean {
        return node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
    }

    fun typeText(node: AccessibilityNodeInfo?, text: String): Boolean {
        if (node == null) return false
        val args = android.os.Bundle()
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun performGlobalBack() = performGlobalAction(GLOBAL_ACTION_BACK)
    fun performGlobalHome() = performGlobalAction(GLOBAL_ACTION_HOME)

    override fun onInterrupt() {
        Log.d("JavisAccessibility", "Accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
