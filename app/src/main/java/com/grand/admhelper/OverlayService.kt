package com.grand.adminhelper

import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: LinearLayout? = null
    var lastDetectedId: String? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
            setBackgroundColor(Color.parseColor("#CC222222"))
        }

        val btnHello = Button(this).apply { text = "Приветствую, помощь в пути" }
        btnHello.setOnClickListener {
            val id = extractLatestId()
            if (id != null) copyText("/ans $id Приветствую, помощь в пути")
            else Toast.makeText(this, "Приложение не нашло ID", Toast.LENGTH_SHORT).show()
        }

        val btnDeny = Button(this).apply { text = "Отказано" }
        btnDeny.setOnClickListener {
            val id = extractLatestId()
            if (id != null) copyText("/ans $id Отказано. Нарушений нет.")
            else Toast.makeText(this, "Приложение не нашло ID", Toast.LENGTH_SHORT).show()
        }

        val btnWait = Button(this).apply { text = "Ожидайте" }
        btnWait.setOnClickListener {
            val id = extractLatestId()
            if (id != null) copyText("/ans $id Ожидайте, разбираюсь")
            else Toast.makeText(this, "Приложение не нашло ID", Toast.LENGTH_SHORT).show()
        }

        layout.addView(btnHello)
        layout.addView(btnDeny)
        layout.addView(btnWait)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 20
            y = 200
        }

        floatingView = layout
        windowManager.addView(layout, params)
    }

    fun extractLatestId(): String? {
        return lastDetectedId
    }

    fun copyText(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("ans", text))
        Toast.makeText(this, "Скопировано: $text", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let { windowManager.removeView(it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
