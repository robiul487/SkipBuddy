package com.example.service

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import com.example.MainActivity
import com.example.R
import com.example.model.FloatingSide
import com.example.model.FloatingVisibility
import com.example.model.SkipBuddyPreferences
import com.example.util.LocaleHelper
import com.example.util.PermissionChecker
import kotlin.math.abs

/**
 * Hosts the floating indicator button and the on-screen ad-wait banner for YouTube and YouTube Music.
 *
 * Strictly obeys:
 * - Visible ONLY while YouTube (com.google.android.youtube) or YouTube Music
 *   (com.google.android.apps.youtube.music, when enabled) is foreground; hidden immediately on leave.
 * - Visibility modes: SHOW / HIDDEN / AUTO_HIDE (collapses to ~12dp edge sliver after 3s idle).
 * - Lock Mode (default ON): single tap & drag are COMPLETELY IGNORED; deliberate 2-second hold shows
 *   current status ONLY (never pauses or mutates state).
 * - Unlocked Mode: single tap toggles Pause/Resume; drag snaps to left/right edge; 2s hold opens app.
 * - Independent of the Accessibility Service: HIDDEN removes the button completely while auto-skip
 *   continues working 100%.
 */
class FloatingOverlayController(
    private val context: Context,
    private val onToggleAutoSkip: () -> Unit,
    private val onPositionCommitted: (FloatingSide, Float) -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val density = context.resources.displayMetrics.density

    private var prefs: SkipBuddyPreferences = SkipBuddyPreferences()
    private var isSupportedAppForeground: Boolean = false
    private var isCollapsedToSliver: Boolean = false
    private var isFlashingCheckmark: Boolean = false
    private var isBannerRequested: Boolean = false
    private var bannerMessage: String = ""

    private var buttonView: FloatingIndicatorView? = null
    private var buttonParams: WindowManager.LayoutParams? = null
    private var bannerView: TextView? = null
    private var bannerParams: WindowManager.LayoutParams? = null

    private val autoHideRunnable = Runnable {
        if (prefs.floatingVisibility == FloatingVisibility.AUTO_HIDE && !isFlashingCheckmark) {
            isCollapsedToSliver = true
            applyButtonLayoutAndStyle()
        }
    }

    private val endFlashRunnable = Runnable {
        isFlashingCheckmark = false
        applyButtonLayoutAndStyle()
        scheduleAutoHideIfNeeded()
    }

    fun setSupportedAppForeground(inSupportedApp: Boolean) {
        mainHandler.post {
            if (isSupportedAppForeground == inSupportedApp) return@post
            isSupportedAppForeground = inSupportedApp
            if (!inSupportedApp) {
                cancelTimers()
                isFlashingCheckmark = false
                isBannerRequested = false
                removeButtonView()
                removeBannerView()
            } else {
                isCollapsedToSliver = false
                syncButtonVisibility()
                syncBannerVisibility()
                scheduleAutoHideIfNeeded()
            }
        }
    }

    fun updatePreferences(newPrefs: SkipBuddyPreferences) {
        mainHandler.post {
            val prevVisibility = prefs.floatingVisibility
            val prevSide = prefs.floatingSide
            val prevY = prefs.floatingYFraction
            prefs = newPrefs

            if (!isSupportedAppForeground) return@post

            if (prevVisibility != newPrefs.floatingVisibility) {
                isCollapsedToSliver = false
            }
            if (prevSide != newPrefs.floatingSide || abs(prevY - newPrefs.floatingYFraction) > 0.001f) {
                updateWindowPositionFromPrefs()
            }
            syncButtonVisibility()
            syncBannerVisibility()
            if (newPrefs.floatingVisibility == FloatingVisibility.AUTO_HIDE) {
                scheduleAutoHideIfNeeded()
            } else {
                mainHandler.removeCallbacks(autoHideRunnable)
                isCollapsedToSliver = false
                applyButtonLayoutAndStyle()
            }
        }
    }

    fun onConfigurationChanged() {
        mainHandler.post {
            if (!isSupportedAppForeground) return@post
            updateWindowPositionFromPrefs()
            applyButtonLayoutAndStyle()
            updateBannerLayout()
        }
    }

    fun triggerSkipCheckmarkFlash() {
        mainHandler.post {
            if (!isSupportedAppForeground) return@post
            if (prefs.floatingVisibility == FloatingVisibility.HIDDEN) return@post
            if (!PermissionChecker.canDrawOverlays(context)) return@post

            mainHandler.removeCallbacks(autoHideRunnable)
            mainHandler.removeCallbacks(endFlashRunnable)
            isCollapsedToSliver = false
            isFlashingCheckmark = true
            syncButtonVisibility()
            applyButtonLayoutAndStyle()
            mainHandler.postDelayed(endFlashRunnable, FLASH_DURATION_MS)
        }
    }

    fun setAdBannerVisible(visible: Boolean, message: String) {
        mainHandler.post {
            isBannerRequested = visible
            bannerMessage = message
            syncBannerVisibility()
        }
    }

    fun destroy() {
        mainHandler.post {
            cancelTimers()
            isSupportedAppForeground = false
            isBannerRequested = false
            removeButtonView()
            removeBannerView()
        }
    }

    private fun cancelTimers() {
        mainHandler.removeCallbacks(autoHideRunnable)
        mainHandler.removeCallbacks(endFlashRunnable)
    }

    private fun scheduleAutoHideIfNeeded() {
        mainHandler.removeCallbacks(autoHideRunnable)
        if (isSupportedAppForeground && prefs.floatingVisibility == FloatingVisibility.AUTO_HIDE) {
            mainHandler.postDelayed(autoHideRunnable, AUTO_HIDE_DELAY_MS)
        }
    }

    private fun syncButtonVisibility() {
        val shouldShow = isSupportedAppForeground &&
            prefs.floatingVisibility != FloatingVisibility.HIDDEN &&
            PermissionChecker.canDrawOverlays(context)

        if (!shouldShow) {
            removeButtonView()
            return
        }

        if (buttonView == null) {
            createAndAttachButtonView()
        }
        applyButtonLayoutAndStyle()
    }

    private fun syncBannerVisibility() {
        val shouldShow = isSupportedAppForeground &&
            isBannerRequested &&
            prefs.showAdBannerEnabled &&
            bannerMessage.isNotBlank() &&
            PermissionChecker.canDrawOverlays(context)

        if (!shouldShow) {
            removeBannerView()
            return
        }

        if (bannerView == null) {
            createAndAttachBannerView()
        }
        bannerView?.text = bannerMessage
        updateBannerLayout()
    }

    private fun overlayWindowType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private fun screenDimensions(): Pair<Int, Int> {
        val dm = context.resources.displayMetrics
        return dm.widthPixels to dm.heightPixels
    }

    private fun dpToPx(dp: Int): Int = (dp * density + 0.5f).toInt()

    private fun createAndAttachButtonView() {
        val (screenW, screenH) = screenDimensions()
        val sizePx = dpToPx(prefs.floatingSize.dp)
        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (prefs.floatingSide == FloatingSide.RIGHT) screenW - sizePx else 0
            y = ((screenH * prefs.floatingYFraction) - (sizePx / 2f)).toInt()
                .coerceIn(dpToPx(48), (screenH - sizePx - dpToPx(48)).coerceAtLeast(dpToPx(48)))
        }

        val view = FloatingIndicatorView(context)
        attachTouchHandling(view, params)

        runCatching {
            windowManager.addView(view, params)
            buttonView = view
            buttonParams = params
        }
    }

    private fun attachTouchHandling(view: FloatingIndicatorView, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var downRawX = 0f
        var downRawY = 0f
        var movedBeyondSlop = false
        var longPressTriggered = false
        val touchSlopPx = dpToPx(10)

        val longPressRunnable = Runnable {
            longPressTriggered = true
            if (prefs.floatingLocked) {
                // LOCK MODE: deliberate 2s hold shows STATUS ONLY — never pauses or opens app.
                isCollapsedToSliver = false
                applyButtonLayoutAndStyle()
                val localizedCtx = LocaleHelper.createLocalizedContext(context, prefs.appLanguage)
                val msg = if (prefs.autoSkipEnabled) {
                    localizedCtx.getString(R.string.toast_locked_status_active)
                } else {
                    localizedCtx.getString(R.string.toast_locked_status_paused)
                }
                Toast.makeText(localizedCtx, msg, Toast.LENGTH_SHORT).show()
                scheduleAutoHideIfNeeded()
            } else {
                // UNLOCKED MODE: 2s long-press opens SkipBuddy app.
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                runCatching { context.startActivity(launchIntent) }
            }
        }

        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    downRawX = event.rawX
                    downRawY = event.rawY
                    movedBeyondSlop = false
                    longPressTriggered = false
                    mainHandler.removeCallbacks(longPressRunnable)
                    mainHandler.postDelayed(longPressRunnable, LONG_PRESS_DURATION_MS)
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downRawX).toInt()
                    val dy = (event.rawY - downRawY).toInt()
                    if (abs(dx) > touchSlopPx || abs(dy) > touchSlopPx) {
                        movedBeyondSlop = true
                        mainHandler.removeCallbacks(longPressRunnable)
                    }
                    // When locked, dragging is COMPLETELY IGNORED so accidental touches cannot move it.
                    if (!prefs.floatingLocked && movedBeyondSlop && !longPressTriggered) {
                        if (isCollapsedToSliver) {
                            isCollapsedToSliver = false
                            applyButtonLayoutAndStyle()
                        }
                        mainHandler.removeCallbacks(autoHideRunnable)
                        val (screenW, screenH) = screenDimensions()
                        params.x = (initialX + dx).coerceIn(0, (screenW - params.width).coerceAtLeast(0))
                        params.y = (initialY + dy).coerceIn(
                            dpToPx(32),
                            (screenH - params.height - dpToPx(32)).coerceAtLeast(dpToPx(32))
                        )
                        updateButtonViewLayoutSafely()
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    mainHandler.removeCallbacks(longPressRunnable)
                    if (event.actionMasked == MotionEvent.ACTION_CANCEL || longPressTriggered) {
                        return@setOnTouchListener true
                    }

                    if (prefs.floatingLocked) {
                        // LOCK MODE: single tap does ABSOLUTELY NOTHING!
                        return@setOnTouchListener true
                    }

                    if (movedBeyondSlop) {
                        // Snap to nearest left or right screen edge & persist
                        val (screenW, screenH) = screenDimensions()
                        val centerX = params.x + params.width / 2
                        val targetSide = if (centerX < screenW / 2) FloatingSide.LEFT else FloatingSide.RIGHT
                        params.x = if (targetSide == FloatingSide.RIGHT) {
                            (screenW - params.width).coerceAtLeast(0)
                        } else {
                            0
                        }
                        updateButtonViewLayoutSafely()
                        val yFraction = ((params.y + params.height / 2f) / screenH.coerceAtLeast(1))
                            .coerceIn(0.12f, 0.88f)
                        onPositionCommitted(targetSide, yFraction)
                        scheduleAutoHideIfNeeded()
                    } else {
                        // UNLOCKED MODE: single tap toggles Pause/Resume auto-skip
                        isCollapsedToSliver = false
                        applyButtonLayoutAndStyle()
                        onToggleAutoSkip()
                        scheduleAutoHideIfNeeded()
                    }
                    true
                }

                else -> false
            }
        }
    }

    private fun updateWindowPositionFromPrefs() {
        val params = buttonParams ?: return
        val (screenW, screenH) = screenDimensions()
        val widthPx = if (isCollapsedToSliver) dpToPx(SLIVER_WIDTH_DP) else dpToPx(prefs.floatingSize.dp)
        val heightPx = if (isCollapsedToSliver) dpToPx(SLIVER_HEIGHT_DP) else dpToPx(prefs.floatingSize.dp)
        params.width = widthPx
        params.height = heightPx
        params.x = if (prefs.floatingSide == FloatingSide.RIGHT) {
            (screenW - widthPx).coerceAtLeast(0)
        } else {
            0
        }
        params.y = ((screenH * prefs.floatingYFraction) - (heightPx / 2f)).toInt()
            .coerceIn(dpToPx(40), (screenH - heightPx - dpToPx(40)).coerceAtLeast(dpToPx(40)))
        updateButtonViewLayoutSafely()
    }

    private fun applyButtonLayoutAndStyle() {
        val view = buttonView ?: return
        val params = buttonParams ?: return
        val (screenW, screenH) = screenDimensions()

        val collapsed = isCollapsedToSliver && !isFlashingCheckmark
        val widthPx = if (collapsed) dpToPx(SLIVER_WIDTH_DP) else dpToPx(prefs.floatingSize.dp)
        val heightPx = if (collapsed) dpToPx(SLIVER_HEIGHT_DP) else dpToPx(prefs.floatingSize.dp)

        params.width = widthPx
        params.height = heightPx
        params.x = if (prefs.floatingSide == FloatingSide.RIGHT) {
            (screenW - widthPx).coerceAtLeast(0)
        } else {
            0
        }
        params.y = ((screenH * prefs.floatingYFraction) - (heightPx / 2f)).toInt()
            .coerceIn(dpToPx(40), (screenH - heightPx - dpToPx(40)).coerceAtLeast(dpToPx(40)))

        view.alpha = when {
            isFlashingCheckmark -> 1.0f
            collapsed -> (prefs.floatingOpacity.alpha * 0.7f).coerceAtLeast(0.32f)
            else -> prefs.floatingOpacity.alpha
        }
        view.updateVisualState(
            isActive = prefs.autoSkipEnabled,
            isLocked = prefs.floatingLocked,
            isCollapsed = collapsed,
            isFlashing = isFlashingCheckmark,
            isOnRightEdge = prefs.floatingSide == FloatingSide.RIGHT
        )
        updateButtonViewLayoutSafely()
    }

    private fun updateButtonViewLayoutSafely() {
        val v = buttonView ?: return
        val p = buttonParams ?: return
        runCatching { windowManager.updateViewLayout(v, p) }
    }

    private fun removeButtonView() {
        val v = buttonView
        buttonView = null
        buttonParams = null
        if (v != null) {
            runCatching { windowManager.removeView(v) }
        }
    }

    private fun createAndAttachBannerView() {
        val textView = TextView(context).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dpToPx(16), dpToPx(10), dpToPx(16), dpToPx(10))
            maxWidth = dpToPx(300)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(20).toFloat()
                setColor(Color.argb(225, 20, 129, 66))
                setStroke(dpToPx(1), Color.argb(120, 255, 255, 255))
            }
            elevation = dpToPx(6).toFloat()
            text = bannerMessage
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dpToPx(56)
        }

        runCatching {
            windowManager.addView(textView, params)
            bannerView = textView
            bannerParams = params
        }
    }

    private fun updateBannerLayout() {
        val v = bannerView ?: return
        val p = bannerParams ?: return
        p.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        p.y = dpToPx(56)
        runCatching { windowManager.updateViewLayout(v, p) }
    }

    private fun removeBannerView() {
        val v = bannerView
        bannerView = null
        bannerParams = null
        if (v != null) {
            runCatching { windowManager.removeView(v) }
        }
    }

    /**
     * Custom lightweight Canvas-drawn indicator view for crisp vector rendering at any size
     * (44dp/52dp/62dp circle or 12dp auto-hide edge sliver).
     */
    private class FloatingIndicatorView(context: Context) : View(context) {
        private var isActive: Boolean = true
        private var isLocked: Boolean = true
        private var isCollapsed: Boolean = false
        private var isFlashing: Boolean = false
        private var isOnRightEdge: Boolean = true

        private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.argb(180, 255, 255, 255)
        }
        private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        private val strokeIconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        private val tempRect = RectF()
        private val tempPath = Path()

        fun updateVisualState(
            isActive: Boolean,
            isLocked: Boolean,
            isCollapsed: Boolean,
            isFlashing: Boolean,
            isOnRightEdge: Boolean
        ) {
            this.isActive = isActive
            this.isLocked = isLocked
            this.isCollapsed = isCollapsed
            this.isFlashing = isFlashing
            this.isOnRightEdge = isOnRightEdge
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0f || h <= 0f) return

            bgPaint.color = when {
                isFlashing -> Color.rgb(22, 163, 74)    // Bright emerald checkmark flash
                isActive -> Color.rgb(20, 129, 66)      // Active green
                else -> Color.rgb(185, 28, 28)          // Paused red
            }

            if (isCollapsed) {
                // ~12dp wide edge sliver flush against the left or right edge
                val radius = w * 0.85f
                tempPath.reset()
                tempRect.set(0f, 0f, w, h)
                val radii = if (isOnRightEdge) {
                    floatArrayOf(radius, radius, 0f, 0f, 0f, 0f, radius, radius)
                } else {
                    floatArrayOf(0f, 0f, radius, radius, radius, radius, 0f, 0f)
                }
                tempPath.addRoundRect(tempRect, radii, Path.Direction.CW)
                canvas.drawPath(tempPath, bgPaint)
                return
            }

            // Full circular button
            val cx = w / 2f
            val cy = h / 2f
            val r = (minOf(w, h) / 2f) - 2f
            canvas.drawCircle(cx, cy, r, bgPaint)

            borderPaint.strokeWidth = r * 0.1f
            canvas.drawCircle(cx, cy, r - borderPaint.strokeWidth / 2f, borderPaint)

            when {
                isFlashing -> {
                    // Bold white checkmark
                    strokeIconPaint.strokeWidth = r * 0.22f
                    tempPath.reset()
                    tempPath.moveTo(cx - r * 0.36f, cy + r * 0.02f)
                    tempPath.lineTo(cx - r * 0.08f, cy + r * 0.30f)
                    tempPath.lineTo(cx + r * 0.40f, cy - r * 0.26f)
                    canvas.drawPath(tempPath, strokeIconPaint)
                }

                isActive -> {
                    // Skip-forward play triangle + bar
                    tempPath.reset()
                    tempPath.moveTo(cx - r * 0.32f, cy - r * 0.32f)
                    tempPath.lineTo(cx + r * 0.16f, cy)
                    tempPath.lineTo(cx - r * 0.32f, cy + r * 0.32f)
                    tempPath.close()
                    canvas.drawPath(tempPath, iconPaint)

                    val barLeft = cx + r * 0.22f
                    val barRight = cx + r * 0.36f
                    tempRect.set(barLeft, cy - r * 0.32f, barRight, cy + r * 0.32f)
                    canvas.drawRoundRect(tempRect, 4f, 4f, iconPaint)
                }

                else -> {
                    // Two pause bars
                    val barW = r * 0.18f
                    val barH = r * 0.60f
                    val gap = r * 0.14f
                    tempRect.set(cx - gap - barW, cy - barH / 2f, cx - gap, cy + barH / 2f)
                    canvas.drawRoundRect(tempRect, 4f, 4f, iconPaint)
                    tempRect.set(cx + gap, cy - barH / 2f, cx + gap + barW, cy + barH / 2f)
                    canvas.drawRoundRect(tempRect, 4f, 4f, iconPaint)
                }
            }

            // Tiny crisp lock dot indicator at bottom-right when locked (and not flashing)
            if (isLocked && !isFlashing) {
                val dotR = r * 0.16f
                iconPaint.color = Color.argb(230, 255, 255, 255)
                canvas.drawCircle(cx + r * 0.48f, cy + r * 0.48f, dotR, iconPaint)
                iconPaint.color = Color.WHITE
            }
        }
    }

    companion object {
        private const val AUTO_HIDE_DELAY_MS = 3_000L
        private const val LONG_PRESS_DURATION_MS = 2_000L
        private const val FLASH_DURATION_MS = 1_300L
        private const val SLIVER_WIDTH_DP = 12
        private const val SLIVER_HEIGHT_DP = 46
    }
}
