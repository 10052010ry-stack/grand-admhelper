package com.grand.admhelper

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : Activity() {

    private val REQUEST_CODE_SCREEN_CAPTURE = 1000
    private val REQUEST_CODE_NOTIFICATION = 1001
    private lateinit var projectionManager: MediaProjectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 2. ВОТ ОН, ЗАПРОС НА УВЕДОМЛЕНИЯ - ОН УЖЕ ВСТАВЛЕН
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE_NOTIFICATION)
            }
        }

        projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 100, 60, 100)
        }

        fun btn(text: String, action: () -> Unit): Button {
            return Button(this).apply {
                this.text = text
                textSize = 20f
                setOnClickListener { action() }
            }
        }

        layout.addView(btn("1. РАЗРЕШИТЬ ПОВЕРХ ДРУГИХ") {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.data = android.net.Uri.parse("package:$packageName")
            startActivity(intent)
        })

        layout.addView(btn("2. ЗАПУСТИТЬ ОВЕРЛЕЙ / ШТОРКУ") {
            val captureIntent = projectionManager.createScreenCaptureIntent()
            startActivityForResult(captureIntent, REQUEST_CODE_SCREEN_CAPTURE)
        })

        layout.addView(btn("3. ПРОВЕРИТЬ УВЕДОМЛЕНИЯ") {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            startActivity(intent)
        })

        setContentView(layout)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
            if (resultCode == RESULT_OK && data != null) {
                val serviceIntent = Intent(this, OverlayService::class.java).apply {
                    putExtra("RESULT_CODE", resultCode)
                    putExtra("DATA", data)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
                Toast.makeText(this, "Запущено! Свайпни шторку вниз", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Ты не нажал Транслировать", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_NOTIFICATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Уведомления разрешены!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Ты запретил уведомления, шторки не будет!", Toast.LENGTH_LONG).show()
            }
        }
    }
}
