package com.margelo.telegramtabbar

import android.graphics.Color
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.react.uimanager.events.RCTEventEmitter

class TelegramTabBarViewManager(
  private val reactContext: ReactApplicationContext,
) : SimpleViewManager<TelegramTabBarView>() {

  override fun getName(): String = "TelegramTabBar"

  override fun createViewInstance(context: ThemedReactContext): TelegramTabBarView {
    val view = TelegramTabBarView(context)

    // Wire event callbacks
    view.onTabPress = { key ->
      val event = com.facebook.react.bridge.Arguments.createMap().apply {
        putString("key", key)
      }
      context.getJSModule(RCTEventEmitter::class.java)
        .receiveEvent(view.id, "topTabPress", event)
    }
    view.onTabLongPress = { key ->
      val event = com.facebook.react.bridge.Arguments.createMap().apply {
        putString("key", key)
      }
      context.getJSModule(RCTEventEmitter::class.java)
        .receiveEvent(view.id, "topTabLongPress", event)
    }

    return view
  }

  override fun getExportedCustomBubblingEventTypeConstants(): Map<String, Any> {
    return mapOf(
      "topTabPress" to mapOf(
        "phasedRegistrationNames" to mapOf(
          "bubbled" to "onTabPress",
          "captured" to "onTabPressCapture"
        )
      ),
      "topTabLongPress" to mapOf(
        "phasedRegistrationNames" to mapOf(
          "bubbled" to "onTabLongPress",
          "captured" to "onTabLongPressCapture"
        )
      )
    )
  }

  @ReactProp(name = "tabs")
  fun setTabs(view: TelegramTabBarView, tabs: ReadableArray?) {
    if (tabs == null) return
    val tabList = mutableListOf<TelegramTabBarView.TabItem>()
    for (i in 0 until tabs.size()) {
      val tab = tabs.getMap(i) ?: continue
      val key = tab.getString("key") ?: continue
      val title = tab.getString("title") ?: ""
      val icon = if (tab.hasKey("icon")) tab.getString("icon") else null
      val svgPathsArr = if (tab.hasKey("svgPaths")) tab.getArray("svgPaths") else null
      val svgElements = parseSvgElements(svgPathsArr)
      tabList.add(TelegramTabBarView.TabItem(key = key, title = title, icon = icon, svgPaths = svgElements))
    }
    view.setTabs(tabList)
  }

  @ReactProp(name = "iconMap")
  fun setIconMap(view: TelegramTabBarView, map: ReadableMap?) {
    if (map == null) return
    val iconMap = mutableMapOf<String, List<TelegramTabBarView.SvgElement>>()
    val iterator = map.keySetIterator()
    while (iterator.hasNextKey()) {
      val key = iterator.nextKey()
      val arr = if (map.hasKey(key)) map.getArray(key) else null
      iconMap[key] = parseSvgElements(arr)
    }
    view.setIconMap(iconMap)
  }

  @ReactProp(name = "activeIndex", defaultInt = 0)
  fun setActiveIndex(view: TelegramTabBarView, index: Int) {
    view.setActiveIndex(index)
  }

  @ReactProp(name = "theme")
  fun setTheme(view: TelegramTabBarView, theme: ReadableMap?) {
    if (theme == null) return
    val bgColor = parseColor(theme.getString("backgroundColor"), Color.parseColor("#1C1C1E"))
    val activeColor = parseColor(theme.getString("activeColor"), Color.parseColor("#0A84FF"))
    val inactiveColor = parseColor(theme.getString("inactiveColor"), Color.parseColor("#636366"))
    val indicatorColor = parseColor(theme.getString("indicatorColor"), Color.parseColor("#0A84FF"))
    view.setThemeColors(bgColor, activeColor, inactiveColor, indicatorColor)
  }

  @ReactProp(name = "bottomInset", defaultFloat = 0f)
  fun setBottomInset(view: TelegramTabBarView, inset: Float) {
    view.setBottomInset(inset.toInt())
  }

  @ReactProp(name = "isVisible", defaultBoolean = true)
  fun setIsVisible(view: TelegramTabBarView, visible: Boolean) {
    view.setIsVisible(visible)
  }

  @ReactProp(name = "tabBadges")
  fun setTabBadges(view: TelegramTabBarView, badges: ReadableArray?) {
    if (badges == null) {
      view.setBadges(emptyMap())
      view.setDotBadges(emptySet())
      return
    }
    val numericBadges = mutableMapOf<String, Int>()
    val dotBadges = mutableSetOf<String>()
    for (i in 0 until badges.size()) {
      val badge = badges.getMap(i) ?: continue
      val key = badge.getString("key") ?: continue
      val isDot = badge.hasKey("isDot") && badge.getBoolean("isDot")
      if (isDot) {
        dotBadges.add(key)
      } else {
        numericBadges[key] = if (badge.hasKey("count")) badge.getInt("count") else 0
      }
    }
    view.setBadges(numericBadges)
    view.setDotBadges(dotBadges)
  }

  private fun parseSvgElements(arr: ReadableArray?): List<TelegramTabBarView.SvgElement> {
    if (arr == null) return emptyList()
    val result = mutableListOf<TelegramTabBarView.SvgElement>()
    for (i in 0 until arr.size()) {
      val map = arr.getMap(i) ?: continue
      val type = map.getString("type") ?: continue
      result.add(TelegramTabBarView.SvgElement(
        type = type,
        d = if (map.hasKey("d")) map.getString("d") else null,
        cx = if (map.hasKey("cx")) map.getString("cx") else null,
        cy = if (map.hasKey("cy")) map.getString("cy") else null,
        r = if (map.hasKey("r")) map.getString("r") else null,
        x1 = if (map.hasKey("x1")) map.getString("x1") else null,
        y1 = if (map.hasKey("y1")) map.getString("y1") else null,
        x2 = if (map.hasKey("x2")) map.getString("x2") else null,
        y2 = if (map.hasKey("y2")) map.getString("y2") else null,
        points = if (map.hasKey("points")) map.getString("points") else null,
        x = if (map.hasKey("x")) map.getString("x") else null,
        y = if (map.hasKey("y")) map.getString("y") else null,
        width = if (map.hasKey("width")) map.getString("width") else null,
        height = if (map.hasKey("height")) map.getString("height") else null,
        rx = if (map.hasKey("rx")) map.getString("rx") else null,
        ry = if (map.hasKey("ry")) map.getString("ry") else null,
      ))
    }
    return result
  }

  private fun parseColor(value: String?, default: Int): Int {
    if (value.isNullOrBlank()) return default
    return try {
      Color.parseColor(value)
    } catch (e: Exception) {
      default
    }
  }
}
