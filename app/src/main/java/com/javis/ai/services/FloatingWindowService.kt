package com.javis.ai.services

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import kotlin.math.abs

class FloatingWindowService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatView: FloatingButtonView
    private lateinit var params: WindowManager.LayoutParams

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    companion object {
        const val ACTION_SHOW = "com.javis.ai.FLOAT_SHOW"
        const val ACTION_HIDE = "com.javis.ai.FLOAT_HIDE"
        const val ACTION_PULSE = "com.javis.ai.FLOAT_PULSE"
        const val ACTION_STOP_PULSE = "com.javis.ai.FLOAT_STOP_PULSE"
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createFloatingButton()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_HIDE -> { hideButton(); stopSelf() }
            ACTION_PULSE -> floatView.startPulse()
            ACTION_STOP_PULSE -> floatView.stopPulse()
        }
        return START_STICKY
    }

    private fun createFloatingButton() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            148, 148,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = resources.displayMetrics.widthPixels - 160
            y = resources.displayMetrics.heightPixels / 3
        }

        floatView = FloatingButtonView(this)
        floatView.setOnTouchListener(createTouchListener())

        windowManager.addView(floatView, params)

        // Entrance animation
        floatView.alpha = 0f
        floatView.scaleX = 0.3f
        floatView.scaleY = 0.3f
        floatView.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(350)
            .setInterpolator(OvershootInterpolator())
            .start()
    }

    private fun createTouchListener() = View.OnTouchListener { _, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = params.x
                initialY = params.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                floatView.animate().scaleX(0.88f).scaleY(0.88f).setDuration(100).start()
                true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - initialTouchX
                val dy = event.rawY - initialTouchY
                if (!isDragging && (abs(dx) > 8f || abs(dy) > 8f)) {
                    isDragging = true
                }
                if (isDragging) {
                    params.x = (initialX + dx).toInt()
                    params.y = (initialY + dy).toInt()
                    windowManager.updateViewLayout(floatView, params)
                }
                true
            }
            MotionEvent.ACTION_UP -> {
                floatView.animate().scaleX(1f).scaleY(1f).setDuration(150)
                    .setInterpolator(OvershootInterpolator()).start()
                if (!isDragging) {
                    onButtonTapped()
                } else {
                    snapToEdge()
                }
                true
            }
            else -> false
        }
    }

    private fun onButtonTapped() {
        floatView.startPulse()
        val intent = Intent(this, JavisAssistantService::class.java).apply {
            action = JavisAssistantService.ACTION_ACTIVATE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun snapToEdge() {
        val screenWidth = resources.displayMetrics.widthPixels
        val targetX = if (params.x + 74 < screenWidth / 2) 12 else screenWidth - 160
        val animator = ValueAnimator.ofInt(params.x, targetX).apply {
            duration = 250
            interpolator = OvershootInterpolator(0.8f)
            addUpdateListener {
                params.x = it.animatedValue as Int
                windowManager.updateViewLayout(floatView, params)
            }
        }
        animator.start()
    }

    private fun hideButton() {
        floatView.animate()
            .alpha(0f)
            .scaleX(0.2f)
            .scaleY(0.2f)
            .setDuration(200)
            .withEndAction {
                runCatching { windowManager.removeView(floatView) }
            }
            .start()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        runCatching { windowManager.removeView(floatView) }
    }
}

class FloatingButtonView(context: Context) : View(context) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        color = Color.parseColor("#00D4FF")
        alpha = 180
    }
    private val glowRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = Color.parseColor("#00FFE5")
        alpha = 100
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 42f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        letterSpacing = -0.03f
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00D4FF")
    }

    private var pulseAnimator: ValueAnimator? = null
    private var pulseScale = 1f
    private var pulseAlpha = 255

    fun startPulse() {
        pulseAnimator?.cancel()
        pulseAnimator = ValueAnimator.ofFloat(1f, 1.25f, 1f).apply {
            duration = 700
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                pulseScale = it.animatedValue as Float
                pulseAlpha = (255 * (1.25f - pulseScale) / 0.25f * 0.6f).toInt().coerceIn(0, 255)
                invalidate()
            }
        }
        pulseAnimator?.start()
    }

    fun stopPulse() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        pulseScale = 1f
        pulseAlpha = 0
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = minOf(cx, cy) - 8f

        // Pulse ring
        if (pulseAnimator != null) {
            glowRingPaint.alpha = pulseAlpha
            canvas.drawCircle(cx, cy, r * pulseScale, glowRingPaint)
        }

        // Background gradient
        bgPaint.shader = RadialGradient(
            cx, cy - r * 0.2f, r,
            intArrayOf(
                Color.parseColor("#1A2A3A"),
                Color.parseColor("#0D1520")
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r, bgPaint)

        // Outer ring
        canvas.drawCircle(cx, cy, r - 1f, ringPaint)

        // Inner ring accent
        glowRingPaint.alpha = 70
        canvas.drawCircle(cx, cy, r - 5f, glowRingPaint)

        // J letter
        canvas.drawText("J", cx, cy + 15f, textPaint)

        // Three small dots at bottom (status indicator)
        val dotY = cy + r - 18f
        val dotSpacing = 9f
        dotPaint.alpha = 200
        canvas.drawCircle(cx - dotSpacing, dotY, 3f, dotPaint)
        dotPaint.alpha = 140
        canvas.drawCircle(cx, dotY, 3f, dotPaint)
        dotPaint.alpha = 80
        canvas.drawCircle(cx + dotSpacing, dotY, 3f, dotPaint)
    }
}
