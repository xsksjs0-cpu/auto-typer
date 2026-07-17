package com.example.autotyper

import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var xInput: EditText
    private lateinit var yInput: EditText
    private lateinit var startDelayInput: EditText
    private lateinit var afterTypeDelayInput: EditText
    private lateinit var afterTapDelayInput: EditText
    private lateinit var maxValueInput: EditText
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
    }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 64, 32, 64)
        }

        fun label(text: String) = TextView(this).apply {
            this.text = text
            setPadding(0, 24, 0, 8)
        }

        fun field(hint: String, default: String) = EditText(this).apply {
            this.hint = hint
            setText(default)
        }

        xInput = field("X координата кнопки подтверждения", "540")
        yInput = field("Y координата кнопки подтверждения", "1800")
        startDelayInput = field("Задержка перед стартом, мс", "3000")
        afterTypeDelayInput = field("Задержка после ввода текста, мс", "400")
        afterTapDelayInput = field("Задержка после нажатия кнопки, мс", "600")
        maxValueInput = field("Максимальное значение цифры (до какой цифры считать)", "9")

        statusText = TextView(this).apply { text = "Статус: ожидание" }

        val btnAccessibility = Button(this).apply {
            text = "1. Включить службу в Спец. возможностях"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }

        val btnOverlayPermission = Button(this).apply {
            text = "2. Разрешить показ поверх экрана"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                    )
                } else {
                    Toast.makeText(this@MainActivity, "Разрешение уже есть", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val btnTestPoint = Button(this).apply {
            text = "Проверить точку (метка на 2 сек)"
            setOnClickListener { showTestMarker() }
        }

        val btnStart = Button(this).apply {
            text = "СТАРТ"
            setOnClickListener { startSequence() }
        }

        val btnStop = Button(this).apply {
            text = "СТОП"
            setOnClickListener {
                AutoTyperAccessibilityService.instance?.stopSequence()
                statusText.text = "Статус: остановлено"
            }
        }

        listOf(
            btnAccessibility,
            btnOverlayPermission,
            label("3. Координаты кнопки подтверждения в игре"),
            xInput, yInput, btnTestPoint,
            label("4. Задержки (подберите под скорость игры)"),
            startDelayInput, afterTypeDelayInput, afterTapDelayInput, maxValueInput,
            label("5. Откройте игру, поставьте курсор в поле ввода,\nсверните это приложение и нажмите СТАРТ —\nбудет время переключиться обратно в игру"),
            btnStart, btnStop,
            statusText
        ).forEach { root.addView(it) }

        return ScrollView(this).apply { addView(root) }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices =
            am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any { it.id.contains(packageName) }
    }

    private fun startSequence() {
        if (!isAccessibilityServiceEnabled() || AutoTyperAccessibilityService.instance == null) {
            Toast.makeText(this, "Сначала включите службу в Спец. возможностях", Toast.LENGTH_LONG).show()
            return
        }
        val x = xInput.text.toString().toIntOrNull()
        val y = yInput.text.toString().toIntOrNull()
        if (x == null || y == null) {
            Toast.makeText(this, "Укажите корректные координаты", Toast.LENGTH_SHORT).show()
            return
        }
        val startDelay = startDelayInput.text.toString().toLongOrNull() ?: 3000L
        val afterType = afterTypeDelayInput.text.toString().toLongOrNull() ?: 400L
        val afterTap = afterTapDelayInput.text.toString().toLongOrNull() ?: 600L
        val maxValue = maxValueInput.text.toString().toIntOrNull() ?: 9

        statusText.text = "Статус: старт через ${startDelay / 1000} сек — переключитесь в игру"
        AutoTyperAccessibilityService.instance?.startSequence(
            x, y, startDelay, afterType, afterTap, maxValue
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showTestMarker() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Нет разрешения на показ поверх экрана", Toast.LENGTH_SHORT).show()
            return
        }
        val x = xInput.text.toString().toIntOrNull() ?: return
        val y = yInput.text.toString().toIntOrNull() ?: return

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val marker = View(this).apply { setBackgroundColor(Color.RED) }
        val size = 40
        val params = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x - size / 2
            this.y = y - size / 2
        }
        wm.addView(marker, params)
        Handler(Looper.getMainLooper()).postDelayed({ wm.removeView(marker) }, 2000)
    }
}
