package com.margelo.telegramtabbar

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.Outline
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.ViewTreeObserver
import android.view.animation.PathInterpolator
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.composables.icons.lucide.R as LucideR
import com.qmdeve.blurview.base.BaseBlurViewGroup
import com.qmdeve.blurview.widget.BlurViewGroup
import expo.modules.kotlin.AppContext
import expo.modules.kotlin.viewevent.EventDispatcher
import expo.modules.kotlin.views.ExpoView
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

@SuppressLint("ViewConstructor")
class TelegramTabBarView(context: Context, appContext: AppContext) : ExpoView(context, appContext) {

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
        val iconName: String? = null
    )

    // ── ExpoModulesCore event dispatchers ─────────────────────────────────
    internal val onTabPress by EventDispatcher()
    internal val onTabLongPress by EventDispatcher()

    // ── shouldUseAndroidLayout = true is CRITICAL for ExpoView (LinearLayout):
    // Without it, requestLayout() is silently ignored by Fabric/React Native.
    // With it, ExpoView.requestLayout() automatically posts measureAndLayout().
    override val shouldUseAndroidLayout = true

    // ── Internal state ────────────────────────────────────────────────────
    private var tabs: List<TabItem> = emptyList()
    private var activeIndex: Int = 0

    private var bgColor: Int = Color.BLACK
    private var activeColor: Int = Color.parseColor("#111111")
    private var inactiveColor: Int = Color.parseColor("#A9ABB1")
    private var indicatorColor: Int = Color.parseColor("#111111")
    private var badgeColor: Int = Color.parseColor("#51CFC4")

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

    // ── Compose reactive state ────────────────────────────────────────────
    // referentialEqualityPolicy: ANY write (even same content, new list object) triggers
    // recomposition. This is essential for the post{} re-push on reattachment to work
    // even when the tab data hasn't changed structurally.
    private val tabsState           = mutableStateOf<List<TabItem>>(emptyList(), referentialEqualityPolicy())
    private val activeIndexState    = mutableStateOf(0)
    private val activeColorIntState     = mutableStateOf(Color.parseColor("#111111"))
    private val inactiveColorIntState   = mutableStateOf(Color.parseColor("#A9ABB1"))
    private val indicatorColorIntState  = mutableStateOf(Color.parseColor("#111111"))
    private val badgeColorIntState      = mutableStateOf(Color.parseColor("#51CFC4"))
    private val badgesState         = mutableStateOf<Map<String, Int>>(emptyMap())
    private val dotBadgesState      = mutableStateOf<Set<String>>(emptySet())

    companion object {
        private val LUCIDE_ICON_MAP: Map<String, Int> = mapOf(
            "house"          to LucideR.drawable.lucide_ic_house,
            "users"          to LucideR.drawable.lucide_ic_users,
            "list-todo"      to LucideR.drawable.lucide_ic_list_todo,
            "task"           to LucideR.drawable.lucide_ic_layout_list,
            "chat"           to LucideR.drawable.lucide_ic_message_square_text,
            "wallet"         to LucideR.drawable.lucide_ic_wallet,
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

    // ── Layers ─────────────────────────────────────────────────────────────

    private val pillDrawable = GradientDrawable().apply {
        setColor(Color.argb(0xA3, 0, 0, 0))
        setCornerRadius(this@TelegramTabBarView.cornerRadius)
    }

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

    private val contentOverlay = ComposeView(context).apply {
        // The tab bar is often detached when navigating to screens that hide it.
        // Recreating the composition on the next attach is more reliable than
        // trying to revive the previous one, which could come back without text/icons.
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        setContent { TabBarContent() }
    }

    init {
        setBackgroundColor(Color.TRANSPARENT)
        clipChildren  = false
        clipToPadding = false
        elevation = elevationDp

        // LayoutParams don't drive positioning here (we override onLayout manually),
        // but we still need to add children so they are part of the view tree.
        addView(blurBackground, LayoutParams(0, 0))
        addView(contentOverlay, LayoutParams(0, 0))

        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            bottomInset = nav.bottom
            requestLayout()
            insets
        }
    }

    override fun onAttachedToWindow() {
        // Push state before super so the very first composition (or any surviving composition)
        // starts with the latest data. Uses ArrayList to ensure a new object reference which,
        // combined with referentialEqualityPolicy, always triggers recomposition.
        tabsState.value = ArrayList(tabs)
        activeIndexState.value = activeIndex
        activeColorIntState.value = activeColor
        inactiveColorIntState.value = inactiveColor
        indicatorColorIntState.value = indicatorColor
        badgeColorIntState.value = badgeColor
        super.onAttachedToWindow()
        ViewCompat.requestApplyInsets(this)
        requestLayout()
        // Safety net: re-push after the framework has attached all children (contentOverlay
        // calls ensureCompositionCreated in its own onAttachedToWindow, which runs AFTER ours).
        // This guarantees the now-live composition recomposes even if the recomposer was
        // paused/cancelled during detach and swallowed the pre-super push.
        post {
            tabsState.value = ArrayList(tabs)
            activeIndexState.value = activeIndex
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isBlurInitialized = false
    }

    // ExpoView extends LinearLayout — its default layout places children side-by-side,
    // giving contentOverlay width=0. We override onLayout to stack both children at the
    // same pill position (blurBackground behind, contentOverlay on top).
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val w = right - left
        val h = bottom - top

        val pillLeft   = floatingMarginH
        val pillRight  = w - floatingMarginH
        val pillWidth  = pillRight - pillLeft
        val pillTop    = h - tabBarHeight - bottomInset - floatingMarginBottom
        val pillBottom = pillTop + tabBarHeight

        if (pillWidth > 0 && pillTop >= 0) {
            val wSpec = MeasureSpec.makeMeasureSpec(pillWidth, MeasureSpec.EXACTLY)
            val hSpec = MeasureSpec.makeMeasureSpec(tabBarHeight, MeasureSpec.EXACTLY)

            blurBackground.measure(wSpec, hSpec)
            blurBackground.layout(pillLeft, pillTop, pillRight, pillBottom)

            // ComposeView.measure() needs windowRecomposer → only safe after onAttachedToWindow.
            // Fabric may call onLayout before attachment; requestLayout() in onAttachedToWindow
            // schedules a second pass (via shouldUseAndroidLayout) once the recomposer is ready.
            if (isAttachedToWindow) {
                contentOverlay.measure(wSpec, hSpec)
                contentOverlay.layout(pillLeft, pillTop, pillRight, pillBottom)
            }
        }

        if (isAttachedToWindow && !isBlurInitialized) post { setupBlur() }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = tabBarHeight + floatingMarginBottom + bottomInset + (8 * density).roundToInt()
        // Report our total size to Fabric/parent. Children are positioned in onLayout().
        setMeasuredDimension(w, h)
    }

    private var isBlurInitialized = false

    // Pill и blur overlay всегда тёмные — дизайн предусматривает frosted-glass эффект.
    private fun pillColor(): Int = Color.argb(0xA3, 0, 0, 0)
    private fun blurOverlayColor(): Int = Color.argb(0xA3, 0, 0, 0)

    private fun setupBlur() {
        if (isBlurInitialized) return
        swapBlurRootToSibling()
        blurBackground.setBlurRadius(25f)
        blurBackground.setCornerRadius(cornerRadius)
        blurBackground.setOverlayColor(blurOverlayColor())
        isBlurInitialized = true
    }

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

    // ── Public API ─────────────────────────────────────────────────────────

    fun setTabs(newTabs: List<TabItem>) {
        tabs = newTabs
        // ArrayList wrapping gives a new reference each call; with referentialEqualityPolicy
        // this guarantees a recomposition even if newTabs is structurally identical.
        tabsState.value = ArrayList(newTabs)
    }

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

    fun setThemeColors(bg: Int, active: Int, inactive: Int, indicator: Int, badge: Int) {
        bgColor = bg; activeColor = active; inactiveColor = inactive
        indicatorColor = indicator; badgeColor = badge
        activeColorIntState.value    = active
        inactiveColorIntState.value  = inactive
        indicatorColorIntState.value = indicator
        badgeColorIntState.value     = badge
        if (isBlurInitialized) blurBackground.setOverlayColor(blurOverlayColor())
        else pillDrawable.setColor(pillColor())
    }

    fun setBottomInset(inset: Int) {
        if (bottomInset == 0 && inset > 0) {
            bottomInset = (inset * density).roundToInt()
            requestLayout()
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

    private suspend fun loadBitmap(source: String): android.graphics.Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(source)
                when (uri.scheme?.lowercase()) {
                    "http", "https" -> {
                        val connection = java.net.URL(source).openConnection().apply {
                            connectTimeout = 5000
                            readTimeout = 5000
                        }
                        connection.getInputStream().use(BitmapFactory::decodeStream)
                    }
                    "content", "file", "android.resource" -> {
                        context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
                    }
                    else -> {
                        val file = File(source)
                        if (file.exists()) file.inputStream().use(BitmapFactory::decodeStream) else null
                    }
                }
            } catch (_: Exception) {
                null
            }
        }

    // ══════════════════════════════════════════════════════════════════════
    // ═══ Compose content ═════════════════════════════════════════════════
    // ══════════════════════════════════════════════════════════════════════

    @Composable
    private fun TabBarContent() {
        val tabs             by tabsState
        val activeIndex      by activeIndexState
        val activeColorInt   by activeColorIntState
        val inactiveColorInt by inactiveColorIntState
        val badgeColorInt    by badgeColorIntState
        val badges           by badgesState
        val dotBadges        by dotBadgesState

        val haptic = LocalHapticFeedback.current

        Box(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {
                tabs.forEachIndexed { index, tab ->
                    val isActive = index == activeIndex

                    val iconColor by animateColorAsState(
                        targetValue   = if (isActive) activeColorInt.toComposeColor() else inactiveColorInt.toComposeColor(),
                        animationSpec = tween(durationMillis = 200),
                        label         = "tabIconColor_$index"
                    )
                    val textColor by animateColorAsState(
                        targetValue   = if (isActive) activeColorInt.toComposeColor() else inactiveColorInt.toComposeColor(),
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
                                    onTabPress(mapOf("key" to tab.key))
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onTabLongPress(mapOf("key" to tab.key))
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isActive) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp)
                                    .background(ComposeColor.White, RoundedCornerShape(10.dp))
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val iconResId = LUCIDE_ICON_MAP[tab.iconName]
                            if (!tab.icon.isNullOrEmpty()) {
                                // Avatar image: load from URL asynchronously
                                val bitmap by produceState<android.graphics.Bitmap?>(null, tab.icon) {
                                    value = loadBitmap(tab.icon)
                                }
                                if (bitmap != null) {
                                    Image(
                                        bitmap            = bitmap!!.asImageBitmap(),
                                        contentDescription = tab.title,
                                        modifier          = Modifier.size(24.dp).clip(CircleShape),
                                        contentScale      = ContentScale.Crop,
                                    )
                                } else if (iconResId != null) {
                                    Icon(
                                        painter           = painterResource(iconResId),
                                        contentDescription = tab.title,
                                        tint              = iconColor,
                                        modifier          = Modifier.size(24.dp)
                                    )
                                }
                            } else if (iconResId != null) {
                                Icon(
                                    painter           = painterResource(iconResId),
                                    contentDescription = tab.title,
                                    tint              = iconColor,
                                    modifier          = Modifier.size(24.dp)
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

                        val count = badges[tab.key] ?: 0
                        val isDot = dotBadges.contains(tab.key)
                        if (count > 0 || isDot) {
                            // Active tab: бейдж у угла белой карточки (padding 6dp от краёв ячейки).
                            // Inactive tab: бейдж у правого края иконки (24dp иконка по центру).
                            val badgeTopPadding = if (isActive) 4.dp else 6.dp
                            val badgeEndPadding = if (isActive) 4.dp else 18.dp
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = badgeTopPadding, end = badgeEndPadding)
                            ) {
                                if (isDot && count == 0) {
                                    Box(
                                        Modifier
                                            .size(8.dp)
                                            .background(badgeColorInt.toComposeColor(), CircleShape)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .height(16.dp)
                                            .widthIn(min = 16.dp)
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(badgeColorInt.toComposeColor(), RoundedCornerShape(999.dp))
                                            .padding(horizontal = 4.dp, vertical = 0.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text       = if (count > 99) "99+" else count.toString(),
                                            color      = ComposeColor.White,
                                            fontSize   = 12.sp,
                                            fontWeight = FontWeight.Normal,
                                            lineHeight = 13.sp,
                                            maxLines   = 1,
                                            modifier   = Modifier.widthIn(min = 0.dp)
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

    private fun Int.toComposeColor() = ComposeColor(
        red   = Color.red(this)   / 255f,
        green = Color.green(this) / 255f,
        blue  = Color.blue(this)  / 255f,
        alpha = Color.alpha(this) / 255f
    )
}
