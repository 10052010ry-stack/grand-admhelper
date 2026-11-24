package com.grand.admhelper

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.LinearLayout

class AccessibilityOverlayService : AccessibilityService() {

    private var overlayView: View? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Пока ничего не показываем, чтобы не мешать
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showOverlay()
        return START_STICKY
    }

    private fun showOverlay() {
        if (overlayView != null) return

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        fun btn(text: String, action: () -> Unit): Button {
            return Button(this).apply {
                this.text = text
                setOnClickListener { action() }
            }
        }

        layout.addView(btn("ОТВЕТ") {
            sendBroadcast(Intent("com.grand.admhelper.ACTION_REPLY"))
        })
        layout.addView(btn("ПРИНЯТЬ") {
            sendBroadcast(Intent("com.grand.admhelper.ACTION_ACCEPT"))
        })
        layout.addView(btn("ЗАКРЫТЬ") {
            hideOverlay()
        })

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 200
        }

        wm.addView(layout, params)
        overlayView = layout
    }

    private fun hideOverlay() {
        overlayView?.let {
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            wm.removeView(it)
            overlayView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
    }
}
