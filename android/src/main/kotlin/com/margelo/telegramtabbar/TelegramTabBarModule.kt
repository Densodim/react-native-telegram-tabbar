package com.margelo.telegramtabbar

import android.graphics.Color
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class TelegramTabBarModule : Module() {
  override fun definition() = ModuleDefinition {
    Name("TelegramTabBar")

    View(TelegramTabBarView::class) {
      Events("onTabPress", "onTabLongPress")

      Prop("tabs") { view: TelegramTabBarView, tabs: List<Map<String, Any?>> ->
        val tabList = tabs.mapNotNull { tab ->
          val key = tab["key"] as? String ?: return@mapNotNull null
          val title = (tab["title"] as? String) ?: ""
          val icon = tab["icon"] as? String
          val iconName = tab["iconName"] as? String
          @Suppress("UNCHECKED_CAST")
          val svgPathsRaw = tab["svgPaths"] as? List<Map<String, Any?>>
          val svgElements = parseSvgElements(svgPathsRaw)
          TelegramTabBarView.TabItem(key = key, title = title, icon = icon, svgPaths = svgElements, iconName = iconName)
        }
        view.setTabs(tabList)
      }

      Prop("activeIndex") { view: TelegramTabBarView, index: Int ->
        view.setActiveIndex(index)
      }

      Prop("theme") { view: TelegramTabBarView, theme: Map<String, Any?> ->
        val bgColor = parseColor(theme["backgroundColor"] as? String, Color.parseColor("#1C1C1E"))
        val activeColor = parseColor(theme["activeColor"] as? String, Color.parseColor("#0A84FF"))
        val inactiveColor = parseColor(theme["inactiveColor"] as? String, Color.parseColor("#636366"))
        val indicatorColor = parseColor(theme["indicatorColor"] as? String, Color.parseColor("#0A84FF"))
        view.setThemeColors(bgColor, activeColor, inactiveColor, indicatorColor)
      }

      Prop("bottomInset") { view: TelegramTabBarView, inset: Float ->
        view.setBottomInset(inset.toInt())
      }

      Prop("isVisible") { view: TelegramTabBarView, visible: Boolean ->
        view.setIsVisible(visible)
      }

      Prop("tabBadges") { view: TelegramTabBarView, badges: List<Map<String, Any?>>? ->
        if (badges == null) {
          view.setBadges(emptyMap())
          view.setDotBadges(emptySet())
          return@Prop
        }
        val numericBadges = mutableMapOf<String, Int>()
        val dotBadges = mutableSetOf<String>()
        badges.forEach { badge ->
          val key = badge["key"] as? String ?: return@forEach
          val isDot = badge["isDot"] as? Boolean ?: false
          if (isDot) {
            dotBadges.add(key)
          } else {
            val count = when (val c = badge["count"]) {
              is Int -> c
              is Double -> c.toInt()
              else -> 0
            }
            numericBadges[key] = count
          }
        }
        view.setBadges(numericBadges)
        view.setDotBadges(dotBadges)
      }

      Prop("iconMap") { _: TelegramTabBarView, _: Map<String, Any?>? ->
        // Legacy no-op — icons now come from TabItem.iconName
      }
    }
  }

  private fun parseSvgElements(arr: List<Map<String, Any?>>?): List<TelegramTabBarView.SvgElement> {
    if (arr == null) return emptyList()
    return arr.mapNotNull { map ->
      val type = map["type"] as? String ?: return@mapNotNull null
      TelegramTabBarView.SvgElement(
        type = type,
        d = map["d"] as? String,
        cx = map["cx"] as? String,
        cy = map["cy"] as? String,
        r = map["r"] as? String,
        x1 = map["x1"] as? String,
        y1 = map["y1"] as? String,
        x2 = map["x2"] as? String,
        y2 = map["y2"] as? String,
        points = map["points"] as? String,
        x = map["x"] as? String,
        y = map["y"] as? String,
        width = map["width"] as? String,
        height = map["height"] as? String,
        rx = map["rx"] as? String,
        ry = map["ry"] as? String,
      )
    }
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
