package com.margelo.telegramtabbar

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.Outline
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.ViewTreeObserver
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.composables.icons.lucide.R as LucideR
import com.qmdeve.blurview.base.BaseBlurViewGroup
import com.qmdeve.blurview.widget.BlurViewGroup
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Native floating TabBar — Telegram-style.
 *
 * Architecture: 2-layer approach for glass effect with sharp content:
 *   Layer 0: BlurBackground (QmBlurView — frosted-glass pill with elevation shadow)
 *   Layer 1: ComposeView   (icons, labels, indicator, badges — crisp Compose rendering)
 *
 * Icons are rendered natively via Material Icons Extended (ImageVector).
 * Touch is handled by Modifier.combinedClickable — no native touch conflicts.
 */
class TelegramTabBarView(context: Context) : FrameLayout(context) {

    /** SVG element descriptor — kept for backward-compatible bridge serialisation. */
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
        val svgPaths: List<SvgElement>? = null,
        /** Lucide icon name (camelCase). E.g. "house", "search", "messageCircle". */
        val iconName: String? = null
    )

    // ── Internal state ───────────────────────────────────────────────────
    private var tabs: List<TabItem> = emptyList()
    private var activeIndex: Int = 0

    // Theme
    private var bgColor: Int = Color.BLACK
    private var activeColor: Int = Color.parseColor("#111111")    // Text/Primary/Stronger (icon + text active)
    private var inactiveColor: Int = Color.parseColor("#A9ABB1")  // Text/Primary/Weak
    private var indicatorColor: Int = Color.parseColor("#111111")

    // Dimensions (pixels)
    private val density = context.resources.displayMetrics.density
    private val floatingMarginH = (16 * density).roundToInt()
    private val floatingMarginBottom = (12 * density).roundToInt()
    private val cornerRadius = (16 * density)
    private val tabBarHeight = (60 * density).roundToInt()
    private val elevationDp = (8 * density)

    private var bottomInset: Int = 0
    private var isBarVisible: Boolean = true
    private var visibilityAnimator: ValueAnimator? = null

    // ── Compose reactive state ───────────────────────────────────────────
    private val tabsState           = mutableStateOf<List<TabItem>>(emptyList())
    private val activeIndexState    = mutableStateOf(0)
    private val activeColorIntState = mutableStateOf(Color.parseColor("#111111"))
    private val inactiveColorIntState   = mutableStateOf(Color.parseColor("#A9ABB1"))
    private val indicatorColorIntState  = mutableStateOf(Color.parseColor("#111111"))
    private val badgesState         = mutableStateOf<Map<String, Int>>(emptyMap())
    private val dotBadgesState      = mutableStateOf<Set<String>>(emptySet())

    companion object {
        /** camelCase icon name → Lucide drawable resource ID. */
        private val LUCIDE_ICON_MAP: Map<String, Int> = mapOf(
            "house"          to LucideR.drawable.lucide_ic_house,
            "home"           to LucideR.drawable.lucide_ic_house,
            "search"         to LucideR.drawable.lucide_ic_search,
            "logIn"          to LucideR.drawable.lucide_ic_log_in,
            "log-in"         to LucideR.drawable.lucide_ic_log_in,
            "plusCircle"     to LucideR.drawable.lucide_ic_circle_plus,
            "plus-circle"    to LucideR.drawable.lucide_ic_circle_plus,
            "messageCircle"  to LucideR.drawable.lucide_ic_message_circle,
            "message-circle" to LucideR.drawable.lucide_ic_message_circle,
            "user"           to LucideR.drawable.lucide_ic_user,
            "bell"           to LucideR.drawable.lucide_ic_bell,
            "settings"       to LucideR.drawable.lucide_ic_settings,
            "heart"          to LucideR.drawable.lucide_ic_heart,
            "bookmark"       to LucideR.drawable.lucide_ic_bookmark,
            "calendar"       to LucideR.drawable.lucide_ic_calendar,
            "camera"         to LucideR.drawable.lucide_ic_camera,
            "image"          to LucideR.drawable.lucide_ic_image,
            "mail"           to LucideR.drawable.lucide_ic_mail,
            "map"            to LucideR.drawable.lucide_ic_map,
            "mapPin"         to LucideR.drawable.lucide_ic_map_pin,
            "map-pin"        to LucideR.drawable.lucide_ic_map_pin,
            "menu"           to LucideR.drawable.lucide_ic_menu,
            "phone"          to LucideR.drawable.lucide_ic_phone,
            "star"           to LucideR.drawable.lucide_ic_star,
        )
    }

    // ── Layers ────────────────────────────────────────────────────────────

    private val pillDrawable = GradientDrawable().apply {
        setColor(Color.argb(0xA3, 0, 0, 0))
        setCornerRadius(this@TelegramTabBarView.cornerRadius)
    }

    // Layer 0: frosted-glass blur pill — NO elevation here so it doesn't overlap contentOverlay
    private val blurBackground = BlurViewGroup(context, null).also { v ->
        v.background = pillDrawable
        v.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
            }
        }
        v.clipToOutline = true
        v.clipChildren  = true
        v.blurRounds    = 4
        v.setDownsampleFactor(4.0f)
    }

    // Layer 1: Compose UI (tabs, white card, indicator, badges)
    // Use DisposeOnDetachedFromWindowOrReleasedFromPool — safe for non-Fragment/Activity hosts
    private val contentOverlay = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
        setContent { TabBarContent() }
    }

    var onTabPress: ((String) -> Unit)? = null
    var onTabLongPress: ((String) -> Unit)? = null

    init {
        setBackgroundColor(Color.TRANSPARENT)
        clipChildren  = false
        clipToPadding = false
        // Shadow on the outer container so it floats above content; children draw in addView order
        elevation = elevationDp

        addView(blurBackground, LayoutParams(LayoutParams.MATCH_PARENT, tabBarHeight).apply {
            gravity      = Gravity.BOTTOM
            leftMargin   = floatingMarginH
            rightMargin  = floatingMarginH
            bottomMargin = floatingMarginBottom
        })

        addView(contentOverlay, LayoutParams(LayoutParams.MATCH_PARENT, tabBarHeight).apply {
            gravity      = Gravity.BOTTOM
            leftMargin   = floatingMarginH
            rightMargin  = floatingMarginH
            bottomMargin = floatingMarginBottom
        })

        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            bottomInset = nav.bottom
            updateMargins()
            insets
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ViewCompat.requestApplyInsets(this)
        post {
            // Fabric completes its layout pass BEFORE onAttachedToWindow(), so our
            // onMeasure guard skipped measuring Compose children. requestLayout() won't
            // cause Fabric to re-measure. Instead, directly re-measure+layout this view
            // using the bounds already set by Fabric, so ComposeView gets properly measured
            // now that windowToken is available for the Recomposer.
            val w = width
            val h = height
            if (w > 0 && h > 0) {
                measure(
                    MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY)
                )
                layout(left, top, right, bottom)
            }
            setupBlur()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isBlurInitialized = false
    }

    private var isBlurInitialized = false

    // #000000 at 64% opacity (0xA3 = 163 = 64% of 255)
    private fun pillColor(@Suppress("UNUSED_PARAMETER") bg: Int): Int = Color.argb(0xA3, 0, 0, 0)

    private fun blurOverlayColor(@Suppress("UNUSED_PARAMETER") bg: Int): Int = Color.argb(0xA3, 0, 0, 0)

    private fun setupBlur() {
        if (isBlurInitialized) return
        swapBlurRootToSibling()
        blurBackground.setBlurRadius(25f)
        blurBackground.setCornerRadius(cornerRadius)
        blurBackground.setOverlayColor(blurOverlayColor(bgColor))
        isBlurInitialized = true
    }

    /**
     * Redirects QmBlurView's root from the Activity decor view to the sibling
     * screens container, eliminating the full-screen overlay artefact.
     */
    private fun swapBlurRootToSibling() {
        val sibling = findScreensContainer() ?: return
        blurBackground.background = null
        try {
            val blurGroupClass = BlurViewGroup::class.java
            val baseField = blurGroupClass.getDeclaredField("mBaseBlurViewGroup")
            baseField.isAccessible = true
            val base = baseField.get(blurBackground) ?: return

            val baseClass = BaseBlurViewGroup::class.java

            val decorViewField = baseClass.getDeclaredField("mDecorView")
            decorViewField.isAccessible = true
            val oldRoot = decorViewField.get(base) as? View

            val listenerField = baseClass.getDeclaredField("preDrawListener")
            listenerField.isAccessible = true
            val listener = listenerField.get(base) as? ViewTreeObserver.OnPreDrawListener

            if (listener != null && oldRoot != null) {
                try { oldRoot.viewTreeObserver.removeOnPreDrawListener(listener) } catch (_: Exception) {}
                decorViewField.set(base, sibling)
                sibling.viewTreeObserver.addOnPreDrawListener(listener)
                baseClass.getDeclaredField("mDifferentRoot").also { it.isAccessible = true; it.setBoolean(base, true) }
                baseClass.getDeclaredField("mForceRedraw").also  { it.isAccessible = true; it.setBoolean(base, true) }
            }
        } catch (_: Exception) {
            blurBackground.background = pillDrawable
        }
    }

    private fun findScreensContainer(): ViewGroup? {
        val parent = this.parent as? ViewGroup ?: return null
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child !== this && child is ViewGroup) return child
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

    // ── Public API ────────────────────────────────────────────────────────

    fun setTabs(newTabs: List<TabItem>) {
        if (tabs == newTabs) return
        tabs = newTabs
        tabsState.value = newTabs
    }

    /** Legacy: no-op — icons now come from TabItem.iconName. */
    fun setIconMap(@Suppress("UNUSED_PARAMETER") newMap: Map<String, List<SvgElement>>) = Unit

    fun setActiveIndex(index: Int) {
        val clamped = index.coerceIn(0, max(0, tabs.size - 1))
        activeIndex = clamped
        activeIndexState.value = clamped
    }

    fun setBadges(newBadges: Map<String, Int>) {
        badgesState.value = newBadges
    }

    fun setDotBadges(newDotBadges: Set<String>) {
        dotBadgesState.value = newDotBadges
    }

    fun setThemeColors(bg: Int, active: Int, inactive: Int, indicator: Int) {
        bgColor = bg; activeColor = active; inactiveColor = inactive; indicatorColor = indicator
        activeColorIntState.value     = active
        inactiveColorIntState.value   = inactive
        indicatorColorIntState.value  = indicator
        if (isBlurInitialized) blurBackground.setOverlayColor(blurOverlayColor(bg))
        else pillDrawable.setColor(pillColor(bg))
    }

    fun setBottomInset(inset: Int) {
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
        val toY = if (show) 0f else totalH.toFloat()
        visibilityAnimator = ValueAnimator.ofFloat(translationY, toY).apply {
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
        if (!isAttachedToWindow) {
            // React Native Fabric can call measure() before onAttachedToWindow().
            // Skip measuring Compose children here to avoid the "Cannot locate windowRecomposer"
            // crash — ComposeView needs an attached window to find the Activity's LifecycleOwner.
            // Fabric will trigger a proper layout pass after attachment.
            setMeasuredDimension(w, h)
            return
        }
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY)
        )
    }

    // ═════════════════════════════════════════════════════════════════════
    // ═══ Compose content: icons, white card, indicator, badges ══════════
    // ═════════════════════════════════════════════════════════════════════

    @Composable
    private fun TabBarContent() {
        val tabs             by tabsState
        val activeIndex      by activeIndexState
        val activeColorInt   by activeColorIntState
        val inactiveColorInt by inactiveColorIntState
        val badges           by badgesState
        val dotBadges        by dotBadgesState

        val haptic = LocalHapticFeedback.current

        Box(modifier = Modifier.fillMaxSize()) {

            // ── Tab cells ──────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxSize()) {
                tabs.forEachIndexed { index, tab ->
                    val isActive = index == activeIndex

                    // Icon tint: Text/Primary/Stronger (#111111) when active, Text/Primary/Weak (#A9ABB1) when inactive
                    val iconColor by animateColorAsState(
                        targetValue   = if (isActive) activeColorInt.toComposeColor()
                                        else inactiveColorInt.toComposeColor(),
                        animationSpec = tween(durationMillis = 200),
                        label         = "tabIconColor_$index"
                    )
                    // Text color: Text/Primary/Stronger (#111111) active, Text/Primary/Weak (#A9ABB1) inactive
                    val textColor by animateColorAsState(
                        targetValue   = if (isActive) ComposeColor(0xFF111111)
                                        else inactiveColorInt.toComposeColor(),
                        animationSpec = tween(durationMillis = 200),
                        label         = "tabTextColor_$index"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .combinedClickable(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    this@TelegramTabBarView.onTabPress?.invoke(tab.key)
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    this@TelegramTabBarView.onTabLongPress?.invoke(tab.key)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // White active-card background
                        if (isActive) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp)
                                    .background(ComposeColor.White, RoundedCornerShape(10.dp))
                            )
                        }

                        // Icon + label
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val iconResId = LUCIDE_ICON_MAP[tab.iconName]
                            if (iconResId != null) {
                                Icon(
                                    painter          = painterResource(iconResId),
                                    contentDescription = tab.title,
                                    tint             = iconColor,
                                    modifier         = Modifier.size(24.dp)
                                )
                            }
                            if (tab.title.isNotEmpty()) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text          = tab.title,
                                    color         = textColor,
                                    fontSize      = 10.sp,
                                    fontWeight    = FontWeight.Medium,
                                    lineHeight    = 10.sp,
                                    letterSpacing = 0.sp,
                                    maxLines      = 1,
                                    overflow      = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Badge (dot or numeric)
                        val count = badges[tab.key] ?: 0
                        val isDot = dotBadges.contains(tab.key)
                        if (count > 0 || isDot) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 4.dp, end = 10.dp)
                            ) {
                                if (isDot && count == 0) {
                                    Box(Modifier.size(8.dp).background(ComposeColor(0xFFFF3B30), CircleShape))
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .background(ComposeColor(0xFFFF3B30), RoundedCornerShape(50))
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text       = if (count > 99) "99+" else count.toString(),
                                            color      = ComposeColor.White,
                                            fontSize   = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    // ── Helper: Android Color Int → Compose Color ─────────────────────────

    private fun Int.toComposeColor() = ComposeColor(
        red   = Color.red(this)   / 255f,
        green = Color.green(this) / 255f,
        blue  = Color.blue(this)  / 255f,
        alpha = Color.alpha(this) / 255f
    )

}
