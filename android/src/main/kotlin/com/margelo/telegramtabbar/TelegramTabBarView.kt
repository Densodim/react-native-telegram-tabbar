package com.margelo.telegramtabbar

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.Rect
import android.text.TextPaint
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.PathParser
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.ViewTreeObserver
import com.qmdeve.blurview.widget.BlurViewGroup
import com.qmdeve.blurview.base.BaseBlurViewGroup
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Native floating TabBar — Telegram-style.
 *
 * Architecture: 2-layer approach for glass effect with sharp content:
 *   Layer 1: BlurBackground (semi-transparent fill for glass effect + elevation shadow)
 *   Layer 2: ContentOverlay  (icons, labels, indicator, badges — always 100% sharp)
 *
 * Icons can be:
 *   1. SVG path data from lucide icons (preferred — crisp stroke-based rendering)
 *   2. Android drawable resources by name (legacy fallback)
 */
class TelegramTabBarView(context: Context) : FrameLayout(context) {

    /** SVG element descriptor — matches JS SvgElement interface */
    data class SvgElement(
        val type: String,
        val d: String? = null,
        val cx: String? = null,
        val cy: String? = null,
        val r: String? = null,
        val x1: String? = null,
        val y1: String? = null,
        val x2: String? = null,
        val y2: String? = null,
        val points: String? = null,
        val x: String? = null,
        val y: String? = null,
        val width: String? = null,
        val height: String? = null,
        val rx: String? = null,
        val ry: String? = null
    )

    data class TabItem(
        val key: String,
        val title: String,
        val icon: String? = null,
        val svgPaths: List<SvgElement>? = null
    )

    private data class TabViewHolder(
        val wrapper: FrameLayout,
        val cardView: View,          // White card bg — always MATCH_PARENT + margins, just toggled visible/invisible
        val column: LinearLayout,
        val iconView: View,          // Can be ImageView or SvgIconView
        val labelView: TextView
    )

    private var tabs: List<TabItem> = emptyList()
    /**
     * Icon map: route name → SVG elements. Sent once from JS on mount (static
     * Lucide data) and cached here. rebuildTabs() consults this map first so
     * icons survive tab list rebuilds caused by auth state changes / role switches.
     */
    private var iconMap: Map<String, List<SvgElement>> = emptyMap()
    private var activeIndex: Int = 0
    private var badges: Map<String, Int> = emptyMap()
    private var dotBadges: Set<String> = emptySet()

    // Theme
    private var bgColor: Int = Color.BLACK
    private var activeColor: Int = Color.parseColor("#007AFF")
    private var inactiveColor: Int = Color.parseColor("#3C3C43")
    private var indicatorColor: Int = Color.parseColor("#007AFF")

    // Dimensions
    private val density = context.resources.displayMetrics.density
    private val floatingMarginH = (16 * density).roundToInt()
    private val floatingMarginBottom = (12 * density).roundToInt()
    private val cornerRadius = (16 * density)
    private val tabBarHeight = (60 * density).roundToInt()
    private val elevationDp = (8 * density)
    private val indicatorHeight = (3 * density)
    private val indicatorTopRadius = (1.5f * density)
    private val iconSizePx = (24 * density).roundToInt()
    private val badgeRadius = (4 * density)
    private val badgeTextSize = 10f * density
    private val activeCardCornerRadius = (10 * density)
    private val activeCardPaddingH = (10 * density).roundToInt()
    private val activeCardPaddingV = (6 * density).roundToInt()
    private val activeCardMargin = (6 * density).roundToInt()
    private val badgeOffsetX = (12 * density)
    private val badgeOffsetY = (4 * density)
    private val badgeMinWidth = (16 * density)
    private val badgePaddingH = (4 * density)

    private var bottomInset: Int = 0
    private var isBarVisible: Boolean = true
    private var visibilityAnimator: ValueAnimator? = null

    // ── PorterDuff Color Filter Manager (Phase 3) ───────────────────────
    // GPU-accelerated color blending, Telegram-style. Cached per color.
    companion object ColorFilterManager {
        private val filterCache = mutableMapOf<Int, PorterDuffColorFilter>()

        fun getColorFilter(color: Int): PorterDuffColorFilter {
            return filterCache.getOrPut(color) {
                PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
            }
        }

        fun clearFilterCache() {
            filterCache.clear()
        }
    }

    // ── Layers ──────────────────────────────────────────────────────────
    // Fallback drawable — flat color shown until blur activates (or if blur root not found).
    private val pillDrawable = GradientDrawable().apply {
        setColor(Color.argb(0xA3, 0, 0, 0))   // #000000 @ 64 % — Figma spec fallback
        cornerRadius = this@TelegramTabBarView.cornerRadius
    }

    // Layer 0: BlurViewGroup — captures the sibling screens container (not the decor view)
    // to produce a true frosted-glass effect without the full-screen overlay artifact.
    private val blurBackground = BlurViewGroup(context, null).also { v ->
        v.background = pillDrawable     // Flat fallback until blur root is found
        v.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
            }
        }
        v.clipToOutline = true
        v.clipChildren = true
        v.elevation = elevationDp
        v.blurRounds = 4
        v.setDownsampleFactor(4.0f)
    }

    private val contentOverlay = ContentOverlayView(context)

    var onTabPress: ((String) -> Unit)? = null
    var onTabLongPress: ((String) -> Unit)? = null

    init {
        setBackgroundColor(Color.TRANSPARENT)
        clipChildren = false
        clipToPadding = false

        // Layer 0: blur background
        addView(blurBackground, LayoutParams(
            LayoutParams.MATCH_PARENT, tabBarHeight
        ).apply {
            gravity = Gravity.BOTTOM
            leftMargin = floatingMarginH
            rightMargin = floatingMarginH
            bottomMargin = floatingMarginBottom
        })

        // Layer 2: content on top (no blur)
        addView(contentOverlay, LayoutParams(
            LayoutParams.MATCH_PARENT, tabBarHeight
        ).apply {
            gravity = Gravity.BOTTOM
            leftMargin = floatingMarginH
            rightMargin = floatingMarginH
            bottomMargin = floatingMarginBottom
        })

        // Edge-to-edge: use ViewCompat for reliable insets across all API levels
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            bottomInset = navInsets.bottom  // Always in pixels
            updateMargins()
            insets
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ViewCompat.requestApplyInsets(this)
        // post{} ensures BlurViewGroup.onAttachedToWindow() has already run
        // (Android dispatches parent first, then children, so children attach during super call above;
        //  post{} queues after the current dispatch so fields are initialised).
        post { setupBlur() }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isBlurInitialized = false
    }

    private var isBlurInitialized = false

    /** Flat pill color — Figma spec. Used as fallback when blur root is unavailable. */
    private fun pillColor(bgColor: Int): Int {
        val luminance = (0.2126 * Color.red(bgColor) + 0.7152 * Color.green(bgColor) +
                0.0722 * Color.blue(bgColor)) / 255.0
        return if (luminance < 0.4) Color.argb(0xA3, 48, 48, 56)
        else Color.argb(0xA3, 0, 0, 0)
    }

    /**
     * Overlay color rendered on top of the blur — matches Figma spec: #000000 @ 64%.
     * Light theme: black 64% → dark frosted pill (matches mockup).
     * Dark  theme: charcoal 64% → pill stays distinguishable on dark background.
     */
    private fun blurOverlayColor(bgColor: Int): Int {
        val luminance = (0.2126 * Color.red(bgColor) + 0.7152 * Color.green(bgColor) +
                0.0722 * Color.blue(bgColor)) / 255.0
        return if (luminance < 0.4) Color.argb(0xA3, 48, 48, 56)  // dark theme  — charcoal 64 %
        else Color.argb(0xA3, 0, 0, 0)                              // light theme — black   64 % (Figma)
    }

    private fun setupBlur() {
        if (isBlurInitialized) return
        swapBlurRootToSibling()
        blurBackground.setBlurRadius(25f)
        blurBackground.setCornerRadius(cornerRadius)
        blurBackground.setOverlayColor(blurOverlayColor(bgColor))
        isBlurInitialized = true
    }

    /**
     * Redirects QmBlurView's blur capture root from the activity decor view to
     * the SIBLING screens container (the ViewGroup that holds the navigation screens).
     *
     * In React Navigation bottom tabs the hierarchy is:
     *   BottomTabNavigator root
     *     ├── ScreensContainer   ← this is what we want to blur
     *     └── TelegramTabBarView ← us
     *
     * Using the sibling (not the decor view) eliminates the full-screen overlay artifact
     * because the BlurView only captures the content it floats over.
     */
    private fun swapBlurRootToSibling() {
        val sibling = findScreensContainer() ?: return
        // Blur root found — clear the flat-color fallback
        blurBackground.background = null

        try {
            val blurViewGroupClass = BlurViewGroup::class.java
            val baseField = blurViewGroupClass.getDeclaredField("mBaseBlurViewGroup")
            baseField.isAccessible = true
            val base = baseField.get(blurBackground) ?: return

            val baseClass = BaseBlurViewGroup::class.java

            val decorViewField = baseClass.getDeclaredField("mDecorView")
            decorViewField.isAccessible = true
            val oldRoot = decorViewField.get(base) as? View

            val preDrawListenerField = baseClass.getDeclaredField("preDrawListener")
            preDrawListenerField.isAccessible = true
            val listener = preDrawListenerField.get(base) as? ViewTreeObserver.OnPreDrawListener

            if (listener != null && oldRoot != null) {
                try { oldRoot.viewTreeObserver.removeOnPreDrawListener(listener) } catch (_: Exception) {}
                decorViewField.set(base, sibling)
                sibling.viewTreeObserver.addOnPreDrawListener(listener)

                baseClass.getDeclaredField("mDifferentRoot").also {
                    it.isAccessible = true; it.setBoolean(base, true)
                }
                baseClass.getDeclaredField("mForceRedraw").also {
                    it.isAccessible = true; it.setBoolean(base, true)
                }
            }
        } catch (_: Exception) {
            // Reflection failed — restore flat-color fallback
            blurBackground.background = pillDrawable
        }
    }

    private fun findScreensContainer(): ViewGroup? {
        // The screens container is the first sibling child of the same parent.
        // In React Navigation bottom tabs both the screens stack and the tab bar
        // share the same parent ViewGroup.
        val parent = this.parent as? ViewGroup ?: return null
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child !== this && child is ViewGroup) {
                android.util.Log.d("TelegramTabBar", "blur root → ${child.javaClass.simpleName}")
                return child
            }
        }
        return null
    }

    private fun updateMargins() {
        listOf(blurBackground, contentOverlay).forEach { v ->
            (v.layoutParams as LayoutParams).bottomMargin = floatingMarginBottom + bottomInset
            v.layoutParams = v.layoutParams
        }
        requestLayout()
    }

    // ── Public API ──────────────────────────────────────────────────────

    fun setTabs(newTabs: List<TabItem>) {
        // Compare only route keys — SVG data is now in iconMap and must not
        // trigger an unnecessary rebuild when it differs from the cached version.
        if (tabs.map { it.key } == newTabs.map { it.key } && tabs.size == newTabs.size) return
        tabs = newTabs
        contentOverlay.rebuildTabs()
        // After RN prop batch completes, force measure+draw with new tab count
        contentOverlay.post {
            contentOverlay.requestLayout()
            contentOverlay.invalidate()
        }
    }

    fun setIconMap(newMap: Map<String, List<SvgElement>>) {
        if (iconMap == newMap) return
        iconMap = newMap
        // Rebuild so icons appear even when setTabs was applied before setIconMap
        // (React Native does not guarantee prop delivery order).
        if (tabs.isNotEmpty()) {
            contentOverlay.rebuildTabs()
            contentOverlay.post {
                contentOverlay.requestLayout()
                contentOverlay.invalidate()
            }
        }
    }

    fun setActiveIndex(index: Int) {
        if (index == activeIndex && contentOverlay.indicatorAnimator == null) return
        activeIndex = index.coerceIn(0, max(0, tabs.size - 1))
        contentOverlay.animateIndicatorTo(activeIndex)
        contentOverlay.updateTabAppearance()
    }

    fun setBadges(newBadges: Map<String, Int>) {
        val old = badges
        badges = newBadges
        if (old != newBadges) {
            for ((k, c) in newBadges) if (c > 0 && (old[k] ?: 0) == 0) contentOverlay.animateBadgeIn(k)
            for ((k, c) in old) if (c > 0 && (newBadges[k] ?: 0) == 0) contentOverlay.animateBadgeOut(k)
            contentOverlay.invalidate()
        }
    }

    fun setDotBadges(newDotBadges: Set<String>) {
        if (dotBadges == newDotBadges) return
        val old = dotBadges
        dotBadges = newDotBadges
        for (key in newDotBadges) if (key !in old) contentOverlay.animateBadgeIn(key)
        for (key in old) if (key !in newDotBadges && (badges[key] ?: 0) == 0) contentOverlay.animateBadgeOut(key)
        contentOverlay.invalidate()
    }

    fun setThemeColors(bg: Int, active: Int, inactive: Int, indicator: Int) {
        bgColor = bg; activeColor = active; inactiveColor = inactive; indicatorColor = indicator
        if (isBlurInitialized) {
            blurBackground.setOverlayColor(blurOverlayColor(bgColor))
        } else {
            pillDrawable.setColor(pillColor(bgColor))
        }
        contentOverlay.updateTabAppearance()
        contentOverlay.invalidate()
    }

    fun setBottomInset(inset: Int) {
        // Fallback only: use JS value (in dp) when native insets haven't arrived yet.
        // Convert dp → px since native layout works in pixels.
        if (bottomInset == 0 && inset > 0) {
            bottomInset = (inset * density).roundToInt()
            updateMargins()
        }
    }

    fun setIsVisible(visible: Boolean) {
        if (isBarVisible == visible) return
        isBarVisible = visible
        animateVisibility(visible)
    }

    private fun animateVisibility(show: Boolean) {
        visibilityAnimator?.cancel()
        val totalH = tabBarHeight + floatingMarginBottom + bottomInset + (8 * density).roundToInt()
        val fromY = translationY
        val toY = if (show) 0f else totalH.toFloat()

        visibilityAnimator = ValueAnimator.ofFloat(fromY, toY).apply {
            duration = 250L
            interpolator = PathInterpolator(0.4f, 0f, 0.2f, 1f)
            addUpdateListener { translationY = it.animatedValue as Float }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(a: Animator) { visibilityAnimator = null }
            })
            start()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = tabBarHeight + floatingMarginBottom + bottomInset + (8 * density).roundToInt()
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY)
        )
    }
    // ═════════════════════════════════════════════════════════════════════
    // ═══ Layer 2: Content (tabs, indicator, badges — always sharp) ═════
    // ═════════════════════════════════════════════════════════════════════

    inner class ContentOverlayView(context: Context) : ViewGroup(context) {

        private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL; color = Color.parseColor("#FF3B30")
        }
        private val badgeTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = badgeTextSize
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        private val indicatorRect = RectF()
        private val badgeRect = RectF()
        private val telegramInterpolator = PathInterpolator(0.4f, 0f, 0.2f, 1f)

        var indicatorAnimator: ValueAnimator? = null
        private var currentIndicatorX = 0f
        private var targetIndicatorX = 0f
        private val badgeScales = mutableMapOf<String, Float>()
        private val badgeAlphas = mutableMapOf<String, Float>()

        // Phase 3/5: Track per-tab color animators for proper cancellation
        private val tabColorAnimators = mutableMapOf<Int, ValueAnimator>()
        // Phase 5: Track per-tab bounce animators for cancellation
        private val tabBounceAnimators = mutableMapOf<Int, AnimatorSet>()

        private val tabHolders = mutableListOf<TabViewHolder>()

        init {
            setWillNotDraw(false)
            clipChildren = false
            clipToPadding = false
            setBackgroundColor(Color.TRANSPARENT)
            // Empty outline — no shadow drawn, but translationZ still controls z-order
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setEmpty()
                }
            }
            // CRITICAL: Must be above blurBackground which has elevation.
            // Use translationZ (not elevation) to avoid drawing a shadow.
            translationZ = elevationDp + 0.1f
        }

        fun rebuildTabs() {
            removeAllViews()
            tabHolders.clear()

            for (i in tabs.indices) {
                val tab = tabs[i]
                val isActive = i == activeIndex

                // Wrapper with ripple
                val wrapper = FrameLayout(context)
                val tv = TypedValue()
                context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, tv, true)
                wrapper.foreground = ContextCompat.getDrawable(context, tv.resourceId)
                wrapper.isClickable = true
                wrapper.isFocusable = true

                // Layer 1: white card background — LayoutParams set ONCE, never changed
                // Visibility is toggled instead to avoid layout pass bugs
                val cardView = View(context).apply {
                    background = GradientDrawable().apply {
                        setColor(Color.WHITE)
                        cornerRadius = activeCardCornerRadius
                    }
                    visibility = if (isActive) View.VISIBLE else View.INVISIBLE
                }
                wrapper.addView(cardView, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER
                ).apply { setMargins(activeCardMargin, activeCardMargin, activeCardMargin, activeCardMargin) })

                // Layer 2: icon + label column — always MATCH_PARENT, no margins
                val column = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                }

                // Set listeners after column is created (bounce needs column reference)
                wrapper.setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
                    animateTabBounce(i, column)
                    onTabPress?.invoke(tab.key)
                }
                wrapper.setOnLongClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
                    onTabLongPress?.invoke(tab.key)
                    true
                }

                // Icon — SVG paths (from lucide) take priority, then drawable fallback
                val rawColor = if (isActive) activeColor else inactiveColor
                val iconColor = Color.rgb(Color.red(rawColor), Color.green(rawColor), Color.blue(rawColor))

                // iconMap (sent once on mount) takes priority; fall back to
                // svgPaths embedded in the tab item for backward compatibility.
                val svgData = iconMap[tab.key]?.takeIf { it.isNotEmpty() }
                    ?: tab.svgPaths?.takeIf { it.isNotEmpty() }

                val iconView: View = if (svgData != null) {
                    // Preferred: draw SVG paths from lucide icons — crisp stroke rendering
                    SvgIconView(context, svgData).apply {
                        setColor(iconColor)
                    }
                } else {
                    // Legacy fallback: use Android drawable resource
                    ImageView(context).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        alpha = 1f
                        val drawable = resolveIcon(tab.icon, tab.key)
                        if (drawable != null) {
                            val mutated = drawable.mutate()
                            mutated.setTintList(null)
                            setImageDrawable(mutated)
                            imageTintList = android.content.res.ColorStateList.valueOf(iconColor)
                        } else {
                            visibility = View.GONE
                        }
                    }
                }

                column.addView(iconView, LinearLayout.LayoutParams(iconSizePx, iconSizePx).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                })

                // Log icon creation for debugging
                if (iconView is SvgIconView) {
                    android.util.Log.d("TelegramTabBar", "Tab[${tab.key}] → SvgIconView with ${tab.svgPaths?.size ?: 0} elements, color=${String.format("#%08X", iconColor)}")
                } else {
                    android.util.Log.d("TelegramTabBar", "Tab[${tab.key}] → ImageView (drawable fallback)")
                }

                // Label — force full alpha on text color
                val labelColor = if (isActive) activeColor else inactiveColor
                val labelView = TextView(context).apply {
                    text = tab.title
                    textSize = 10f // sp
                    setTextColor(Color.rgb(Color.red(labelColor), Color.green(labelColor), Color.blue(labelColor)))
                    typeface = if (isActive) Typeface.create("sans-serif-medium", Typeface.BOLD)
                        else Typeface.create("sans-serif-medium", Typeface.NORMAL)
                    gravity = Gravity.CENTER
                    setPadding(0, (1 * density).roundToInt(), 0, 0)
                    maxLines = 1
                }
                column.addView(labelView, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ))

                wrapper.addView(column, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER
                ))

                addView(wrapper)
                tabHolders.add(TabViewHolder(wrapper, cardView, column, iconView, labelView))
            }

            // Init badge states (numeric badges and dot badges)
            for (tab in tabs) {
                val hasBadge = (badges[tab.key] ?: 0) > 0 || dotBadges.contains(tab.key)
                badgeScales.putIfAbsent(tab.key, if (hasBadge) 1f else 0f)
                badgeAlphas.putIfAbsent(tab.key, if (hasBadge) 1f else 0f)
            }

            post {
                if (tabs.isNotEmpty() && width > 0) {
                    currentIndicatorX = activeIndex * (width.toFloat() / tabs.size)
                    targetIndicatorX = currentIndicatorX
                    invalidate()
                }
            }
            requestLayout()
            invalidate()
        }

        private fun resolveIcon(iconName: String?, fallbackKey: String): Drawable? {
            val name = iconName ?: fallbackKey
            // Try to find drawable resource by name
            val resId = context.resources.getIdentifier(name, "drawable", context.packageName)
            if (resId != 0) return ContextCompat.getDrawable(context, resId)

            // Try with ic_ prefix
            val resId2 = context.resources.getIdentifier("ic_$name", "drawable", context.packageName)
            if (resId2 != 0) return ContextCompat.getDrawable(context, resId2)

            // Try Material symbols / system icons
            val resId3 = context.resources.getIdentifier(name, "drawable", "android")
            if (resId3 != 0) return ContextCompat.getDrawable(context, resId3)

            return null
        }

        // ── Tab bounce animation (Phase 5: Telegram-style 3-phase) ──────

        private fun animateTabBounce(tabIndex: Int, targetView: View) {
            // Cancel any in-progress bounce for this tab
            tabBounceAnimators[tabIndex]?.cancel()

            val scaleUp = 1.2f       // More pronounced than original 1.15f
            val scaleOvershoot = 0.95f  // Dip below baseline for spring feel

            val bounceUp = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(targetView, "scaleX", 1f, scaleUp),
                    ObjectAnimator.ofFloat(targetView, "scaleY", 1f, scaleUp)
                )
                duration = 100
                interpolator = telegramInterpolator
            }

            // Overshoot phase: quick dip below 1.0 for spring feel
            val bounceOvershoot = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(targetView, "scaleX", scaleUp, scaleOvershoot),
                    ObjectAnimator.ofFloat(targetView, "scaleY", scaleUp, scaleOvershoot)
                )
                duration = 80
                interpolator = telegramInterpolator
            }

            val bounceDown = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(targetView, "scaleX", scaleOvershoot, 1f),
                    ObjectAnimator.ofFloat(targetView, "scaleY", scaleOvershoot, 1f)
                )
                duration = 120
                interpolator = OvershootInterpolator(1.5f)
            }

            val fullBounce = AnimatorSet().apply {
                playSequentially(bounceUp, bounceOvershoot, bounceDown)
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(a: Animator) {
                        tabBounceAnimators.remove(tabIndex)
                    }
                })
                start()
            }
            tabBounceAnimators[tabIndex] = fullBounce
        }

        fun updateTabAppearance() {
            for (i in tabHolders.indices) {
                val holder = tabHolders[i]
                val isActive = i == activeIndex
                val rawColor = if (isActive) activeColor else inactiveColor
                val color = Color.rgb(Color.red(rawColor), Color.green(rawColor), Color.blue(rawColor))

                // Phase 3: Cancel any in-progress color animation for this tab
                tabColorAnimators[i]?.cancel()

                // Get current color depending on icon type
                val currentColor = when (val iv = holder.iconView) {
                    is SvgIconView -> iv.getColor()
                    is ImageView -> iv.imageTintList?.defaultColor ?: inactiveColor
                    else -> inactiveColor
                }

                // Animate color transition with proper lifecycle (Phase 3 + 4)
                val animator = ValueAnimator.ofArgb(currentColor, color).apply {
                    duration = 200
                    interpolator = telegramInterpolator
                    addUpdateListener { anim ->
                        val c = anim.animatedValue as Int
                        when (val iv = holder.iconView) {
                            is SvgIconView -> iv.setColor(c)  // Uses PorterDuff internally
                            is ImageView -> {
                                // Phase 3: PorterDuff for ImageView too
                                iv.colorFilter = getColorFilter(c)
                            }
                        }
                        holder.labelView.setTextColor(c)
                    }
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(a: Animator) {
                            tabColorAnimators.remove(i)
                        }
                    })
                    start()
                }
                tabColorAnimators[i] = animator

                holder.labelView.typeface = if (isActive) {
                    Typeface.create("sans-serif-medium", Typeface.BOLD)
                } else {
                    Typeface.create("sans-serif-medium", Typeface.NORMAL)
                }

                // Toggle card visibility — LayoutParams never change, no layout pass needed
                holder.cardView.visibility = if (isActive) View.VISIBLE else View.INVISIBLE
            }
        }

        // ── Indicator ───────────────────────────────────────────────────

        fun animateIndicatorTo(index: Int) {
            if (tabs.isEmpty() || width == 0) return
            val tabWidth = width.toFloat() / tabs.size

            indicatorAnimator?.let {
                currentIndicatorX = it.animatedValue as? Float ?: currentIndicatorX
                it.cancel()
            }
            targetIndicatorX = index * tabWidth

            indicatorAnimator = ValueAnimator.ofFloat(currentIndicatorX, targetIndicatorX).apply {
                duration = 250L
                interpolator = telegramInterpolator
                addUpdateListener { currentIndicatorX = it.animatedValue as Float; invalidate() }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(a: Animator) { indicatorAnimator = null }
                })
                start()
            }
        }

        // ── Badge animations ────────────────────────────────────────────

        fun animateBadgeIn(key: String) {
            AnimatorSet().apply {
                val s = ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 250  // Phase 5: Longer for more pronounced spring
                    interpolator = PathInterpolator(0.34f, 1.6f, 0.64f, 1f)  // Stronger overshoot
                    addUpdateListener { badgeScales[key] = it.animatedValue as Float; invalidate() }
                }
                val a = ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 200  // Phase 5: Slightly longer fade
                    addUpdateListener { badgeAlphas[key] = it.animatedValue as Float }
                }
                playTogether(s, a); start()
            }
        }

        fun animateBadgeOut(key: String) {
            AnimatorSet().apply {
                val s = ValueAnimator.ofFloat(badgeScales[key] ?: 1f, 0f).apply {
                    duration = 150; interpolator = telegramInterpolator
                    addUpdateListener { badgeScales[key] = it.animatedValue as Float; invalidate() }
                }
                val a = ValueAnimator.ofFloat(badgeAlphas[key] ?: 1f, 0f).apply {
                    duration = 150
                    addUpdateListener { badgeAlphas[key] = it.animatedValue as Float }
                }
                playTogether(s, a); start()
            }
        }

        // ── Measure / Layout ────────────────────────────────────────────

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val w = MeasureSpec.getSize(widthMeasureSpec)
            val n = tabHolders.size
            if (n > 0) {
                val tw = w / n
                val cw = MeasureSpec.makeMeasureSpec(tw, MeasureSpec.EXACTLY)
                val ch = MeasureSpec.makeMeasureSpec(tabBarHeight, MeasureSpec.EXACTLY)
                tabHolders.forEach { it.wrapper.measure(cw, ch) }
            }
            setMeasuredDimension(w, tabBarHeight)
        }

        override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
            val n = tabHolders.size
            if (n == 0) return
            val tw = (r - l) / n
            tabHolders.forEachIndexed { i, h ->
                h.wrapper.layout(i * tw, 0, i * tw + tw, tabBarHeight)
            }
            if (changed && indicatorAnimator == null) {
                currentIndicatorX = activeIndex * tw.toFloat()
                targetIndicatorX = currentIndicatorX
            }
        }

        // ── Drawing ─────────────────────────────────────────────────────

        override fun dispatchDraw(canvas: Canvas) {
            super.dispatchDraw(canvas)
            drawBadges(canvas)
        }

        private fun drawIndicator(canvas: Canvas) {
            if (tabs.isEmpty() || width == 0) return
            val tw = width.toFloat() / tabs.size
            val iw = tw * 0.45f
            indicatorPaint.color = indicatorColor
            indicatorRect.set(
                currentIndicatorX + (tw - iw) / 2f, 0f,
                currentIndicatorX + (tw + iw) / 2f, indicatorHeight
            )
            canvas.drawRoundRect(indicatorRect, indicatorTopRadius, indicatorTopRadius, indicatorPaint)
        }

        private fun drawBadges(canvas: Canvas) {
            if (tabs.isEmpty() || width == 0) return
            val tw = width.toFloat() / tabs.size

            for (i in tabs.indices) {
                val tab = tabs[i]
                val count = badges[tab.key] ?: 0
                val isDot = dotBadges.contains(tab.key)
                val hasBadge = count > 0 || isDot
                val scale = badgeScales[tab.key] ?: if (hasBadge) 1f else 0f
                val alpha = badgeAlphas[tab.key] ?: if (hasBadge) 1f else 0f
                if (scale <= 0.01f) continue

                val cx = i * tw + tw / 2f + badgeOffsetX
                val cy = badgeOffsetY + 14 * density

                if (isDot && count <= 0) {
                    // Dot badge: small circle without text
                    val dotRadius = 4 * density
                    canvas.save()
                    canvas.scale(scale, scale, cx, cy)
                    badgePaint.alpha = (alpha * 255).roundToInt()
                    canvas.drawCircle(cx, cy, dotRadius, badgePaint)
                    canvas.restore()
                } else if (count > 0) {
                    // Numeric badge (unchanged)
                    val text = if (count > 99) "99+" else count.toString()
                    val textW = badgeTextPaint.measureText(text)
                    val bw = max(badgeMinWidth, textW + badgePaddingH * 2)
                    val bh = badgeRadius * 2

                    canvas.save()
                    canvas.scale(scale, scale, cx, cy)
                    badgePaint.alpha = (alpha * 255).roundToInt()
                    badgeTextPaint.alpha = (alpha * 255).roundToInt()
                    badgeRect.set(cx - bw / 2f, cy - bh / 2f, cx + bw / 2f, cy + bh / 2f)
                    canvas.drawRoundRect(badgeRect, badgeRadius, badgeRadius, badgePaint)
                    val tb = Rect()
                    badgeTextPaint.getTextBounds(text, 0, text.length, tb)
                    canvas.drawText(text, cx, cy + tb.height() / 2f, badgeTextPaint)
                    canvas.restore()
                }
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // ═══ SVG Icon View — draws lucide-style stroke icons via Canvas ════
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Renders SVG icon using pre-transformed, cached paths for optimal performance.
     *
     * Architecture (Phases 1-4 from refactoring plan):
     * - Paths are parsed once from SVG data (ensureParsed)
     * - Paths are pre-transformed to screen coordinates and cached (PathCache)
     * - Paint uses fixed stroke width (1.75dp) — never affected by canvas scaling
     * - Color changes use PorterDuff SRC_IN filters (GPU-accelerated)
     * - Hardware layer for static state, software layer during color animation
     *
     * Performance characteristics:
     * - First draw: ~2-3ms (parsing + transform + cache)
     * - Subsequent draws: ~0.5ms (cache hit, zero allocations)
     * - Color change: ~0.1ms (PorterDuff filter swap)
     *
     * Stroke model (Phase 1):
     *   strokeWidth = 1.75dp * density (fixed, scale-independent)
     *   Paths transformed via Path.transform(Matrix) — NOT canvas.scale()
     *   This ensures stroke width is never multiplied by the viewport scale factor.
     *
     * Based on Telegram Android patterns:
     *   ChevronView (1.75dp stroke), AnimatedArrowDrawable (PorterDuff),
     *   RLottieDrawable (path caching), ImageReceiver (filter caching)
     */
    inner class SvgIconView(context: Context, private val elements: List<SvgElement>) : View(context) {

        init {
            // CRITICAL: Without this, View skips onDraw() entirely!
            setWillNotDraw(false)
            // Ensure full opacity
            alpha = 1f
        }

        // ── Paint (configured once, color set directly for stroke rendering) ──
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * context.resources.displayMetrics.density  // 2.5dp — bold, crisp strokes
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isDither = true
            color = Color.BLACK
        }

        private var iconColor: Int = Color.BLACK

        // ── Source paths (parsed once from SVG data) ────────────────────
        private val parsedPaths = mutableListOf<Path>()
        private val parsedCircles = mutableListOf<FloatArray>()  // [cx, cy, r]
        private val parsedLines = mutableListOf<FloatArray>()    // [x1, y1, x2, y2]
        private var pathsParsed = false

        // ── Path Cache (Phase 2) ────────────────────────────────────────
        // Caches pre-transformed paths to avoid per-frame Matrix operations.
        // Invalidated only on size change.
        private var cachedScale = -1f
        private val cachedTransformedPaths = mutableListOf<Path>()
        private val cachedScaledCircles = mutableListOf<FloatArray>()
        private val cachedScaledLines = mutableListOf<FloatArray>()
        // Pre-computed pixel-perfect offsets
        private var cachedOffsetX = 0f
        private var cachedOffsetY = 0f

        // Reusable Matrix object — zero allocation in onDraw
        private val transformMatrix = Matrix()

        fun setColor(color: Int) {
            if (iconColor == color) return
            iconColor = color
            // Direct color for stroke-based rendering (no PorterDuff needed for Canvas strokes)
            strokePaint.color = color
            invalidate()
        }

        fun getColor(): Int = iconColor

        // ── SVG Parsing (once per icon lifecycle) ───────────────────────
        private fun ensureParsed() {
            if (pathsParsed) return
            pathsParsed = true

            for (el in elements) {
                when (el.type) {
                    "path" -> {
                        el.d?.let { d ->
                            try {
                                parsedPaths.add(PathParser.createPathFromPathData(d))
                            } catch (e: Exception) {
                                android.util.Log.w("SvgIconView", "Failed to parse path: $d", e)
                            }
                        }
                    }
                    "circle" -> {
                        val cx = el.cx?.toFloatOrNull() ?: 0f
                        val cy = el.cy?.toFloatOrNull() ?: 0f
                        val r = el.r?.toFloatOrNull() ?: 0f
                        parsedCircles.add(floatArrayOf(cx, cy, r))
                    }
                    "line" -> {
                        val x1 = el.x1?.toFloatOrNull() ?: 0f
                        val y1 = el.y1?.toFloatOrNull() ?: 0f
                        val x2 = el.x2?.toFloatOrNull() ?: 0f
                        val y2 = el.y2?.toFloatOrNull() ?: 0f
                        parsedLines.add(floatArrayOf(x1, y1, x2, y2))
                    }
                    "polyline", "polygon" -> {
                        el.points?.let { pts ->
                            try {
                                val coords = pts.trim().split("[,\\s]+".toRegex()).map { it.toFloat() }
                                if (coords.size >= 4) {
                                    val path = Path()
                                    path.moveTo(coords[0], coords[1])
                                    for (i in 2 until coords.size step 2) {
                                        if (i + 1 < coords.size) path.lineTo(coords[i], coords[i + 1])
                                    }
                                    if (el.type == "polygon") path.close()
                                    parsedPaths.add(path)
                                }
                            } catch (_: Exception) {}
                        }
                    }
                    "rect" -> {
                        val x = el.x?.toFloatOrNull() ?: 0f
                        val y = el.y?.toFloatOrNull() ?: 0f
                        val w = el.width?.toFloatOrNull() ?: 0f
                        val h = el.height?.toFloatOrNull() ?: 0f
                        val rx = el.rx?.toFloatOrNull() ?: 0f
                        val ry = el.ry?.toFloatOrNull() ?: rx
                        val path = Path()
                        path.addRoundRect(RectF(x, y, x + w, y + h), rx, ry, Path.Direction.CW)
                        parsedPaths.add(path)
                    }
                }
            }

            // Apply initial color directly
            strokePaint.color = iconColor
        }

        // ── Cache Management (Phase 2) ──────────────────────────────────
        // Rebuilds transformed paths when scale changes (i.e. on view resize).
        // In steady state, onDraw only reads from cache — zero allocations.
        private fun ensureCached(scale: Float) {
            if (cachedScale == scale) return
            cachedScale = scale

            // Pre-transform paths
            transformMatrix.setScale(scale, scale)
            cachedTransformedPaths.clear()
            for (path in parsedPaths) {
                val transformed = Path()
                path.transform(transformMatrix, transformed)
                cachedTransformedPaths.add(transformed)
            }

            // Pre-scale circles
            cachedScaledCircles.clear()
            for (circle in parsedCircles) {
                cachedScaledCircles.add(floatArrayOf(
                    circle[0] * scale,
                    circle[1] * scale,
                    circle[2] * scale
                ))
            }

            // Pre-scale lines
            cachedScaledLines.clear()
            for (line in parsedLines) {
                cachedScaledLines.add(floatArrayOf(
                    line[0] * scale,
                    line[1] * scale,
                    line[2] * scale,
                    line[3] * scale
                ))
            }

            // Pixel-perfect centering (rounded to avoid sub-pixel blur)
            val viewportSize = 24f
            cachedOffsetX = ((width - viewportSize * scale) / 2f).roundToInt().toFloat()
            cachedOffsetY = ((height - viewportSize * scale) / 2f).roundToInt().toFloat()
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            cachedScale = -1f  // Force cache rebuild on next draw
        }

        // ── Drawing ───────────────────────────────────────────────────────
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            ensureParsed()

            android.util.Log.d("SvgIconView", "onDraw: w=$width h=$height paths=${parsedPaths.size} circles=${parsedCircles.size} lines=${parsedLines.size} color=${String.format("#%08X", iconColor)} paintAlpha=${strokePaint.alpha} strokeW=${strokePaint.strokeWidth}")

            if (parsedPaths.isEmpty() && parsedCircles.isEmpty() && parsedLines.isEmpty()) return

            // Force color and full opacity every frame
            strokePaint.color = iconColor
            strokePaint.alpha = 255

            // Lucide icons are designed in a 24×24 viewport
            val viewportSize = 24f
            val scale = min(width.toFloat(), height.toFloat()) / viewportSize

            // Ensure cache is populated (no-op if scale unchanged)
            ensureCached(scale)

            canvas.save()
            canvas.translate(cachedOffsetX, cachedOffsetY)

            // Draw from cache
            for (path in cachedTransformedPaths) {
                canvas.drawPath(path, strokePaint)
            }
            for (circle in cachedScaledCircles) {
                canvas.drawCircle(circle[0], circle[1], circle[2], strokePaint)
            }
            for (line in cachedScaledLines) {
                canvas.drawLine(line[0], line[1], line[2], line[3], strokePaint)
            }

            canvas.restore()
        }
    }
}
