package com.grand.admhelper

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var isPanelShown = false
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundNotification()

        // ЕСЛИ ШТОРКА УЖЕ ВИСИТ - НЕ ПЕРЕСОЗДАЕМ, ПРОСТО ДЕРЖИМ СЕРВИС
        if (isPanelShown) {
            return START_STICKY
        }

        // Если интент пустой (Андроид перезапустил сервис) - просто показываем панель без захвата экрана
        if (intent == null) {
            setupFloatingPanel()
            return START_STICKY
        }

        val resultCode = intent.getIntExtra("RESULT_CODE", Activity.RESULT_CANCELED)
        @Suppress("DEPRECATION")
        val data = intent.getParcelableExtra<Intent>("DATA")

        if (resultCode != Activity.RESULT_CANCELED && data != null) {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)
            setupVirtualDisplay()
        }
        setupFloatingPanel()
        return START_STICKY
    }

    private fun startForegroundNotification() {
        val channelId = "adm_helper_channel"
        val channel = NotificationChannel(
            channelId,
            "Grand AdmHelper",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Grand AdmHelper работает")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .build()
        try {
            startForeground(1, notification)
        } catch (e: Exception) { }
    }

    private fun setupVirtualDisplay() {
        val metrics = resources.displayMetrics
        imageReader = ImageReader.newInstance(
            metrics.widthPixels,
            metrics.heightPixels,
            PixelFormat.RGBA_8888,
            2
        )
        mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            metrics.widthPixels,
            metrics.heightPixels,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )
    }

    private fun setupFloatingPanel() {
        if (isPanelShown) return
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#CC1E1E1E"))
            setPadding(12, 12, 12, 12)
        }

        val btnFollow = Button(this).apply {
            text = "Слежу"
            setOnClickListener { captureAndCopy("Здравствуйте, работаю по вашей жалобе!") }
        }
        val btnOfftop = Button(this).apply {
            text = "Не оффтоп"
            setOnClickListener { captureAndCopy("Не оффтопьте в репорт.") }
        }
        val btnAnswer = Button(this).apply {
            text = "Ответ на репорт"
            setOnClickListener { captureAndCopy("Помощь в пути") }
        }

        panel.addView(btnFollow)
        panel.addView(btnOfftop)
        panel.addView(btnAnswer)
        overlayView = panel

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 24
            y = 180
        }
        try {
            windowManager.addView(overlayView, params)
            isPanelShown = true
        } catch (e: Exception) { }
    }

    private fun captureAndCopy(replyText: String) {
        val image = imageReader?.acquireLatestImage() ?: return
        val plane = image.planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * image.width

        val fullBitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        fullBitmap.copyPixelsFromBuffer(buffer)
        image.close()

        val chatArea = Bitmap.createBitmap(fullBitmap, 0, 0, fullBitmap.width / 2, fullBitmap.height / 2)
        val inputImage = InputImage.fromBitmap(chatArea, 0)

        recognizer.process(inputImage)
            .addOnSuccessListener { result ->
                val id = extractLatestId(result.text)
                if (id != null) {
                    val command = "/ans $id $replyText"
                    copyToClipboard(command)
                    Toast.makeText(this, "Скопировано: $command", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "ID не распознан", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun extractLatestId(text: String): String? {
        val regex = Regex("""[\[\(](\d{1,4})[\]\)]""")
        return regex.findAll(text).lastOrNull()?.groupValues?.get(1)
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("adm_ans", text))
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (::overlayView.isInitialized && isPanelShown) {
                windowManager.removeView(overlayView)
            }
        } catch (e: Exception) { }
        isPanelShown = false
        mediaProjection?.stop()
    }
}
