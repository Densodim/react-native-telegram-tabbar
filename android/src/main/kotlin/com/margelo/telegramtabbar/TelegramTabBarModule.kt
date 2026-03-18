package com.margelo.telegramtabbar

import android.graphics.Color
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record

// ── ExpoModulesCore Record types (required for proper JS→Kotlin conversion) ──

class TabItemRecord : Record {
  @Field val key: String = ""
  @Field val title: String = ""
  @Field val iconName: String? = null
  @Field val icon: String? = null
}

class TabBadgeRecord : Record {
  @Field val key: String = ""
  @Field val count: Int = 0
  @Field val isDot: Boolean = false
}

class TabBarThemeRecord : Record {
  @Field val backgroundColor: String? = null
  @Field val activeColor: String? = null
  @Field val inactiveColor: String? = null
  @Field val indicatorColor: String? = null
}

// ─────────────────────────────────────────────────────────────────────────────

class TelegramTabBarModule : Module() {
  override fun definition() = ModuleDefinition {
    Name("TelegramTabBar")

    View(TelegramTabBarView::class) {
      Events("onTabPress", "onTabLongPress")

      Prop("tabs") { view: TelegramTabBarView, tabs: List<TabItemRecord> ->
        val tabList = tabs.mapNotNull { tab ->
          if (tab.key.isEmpty()) return@mapNotNull null
          TelegramTabBarView.TabItem(
            key      = tab.key,
            title    = tab.title,
            icon     = tab.icon,
            iconName = tab.iconName,
          )
        }
        view.setTabs(tabList)
      }

      Prop("activeIndex") { view: TelegramTabBarView, index: Int ->
        view.setActiveIndex(index)
      }

      Prop("theme") { view: TelegramTabBarView, theme: TabBarThemeRecord ->
        val bgColor       = parseColor(theme.backgroundColor, Color.parseColor("#000000"))
        val activeColor   = parseColor(theme.activeColor,     Color.parseColor("#111111"))
        val inactiveColor = parseColor(theme.inactiveColor,   Color.parseColor("#A9ABB1"))
        val indicatorColor = parseColor(theme.indicatorColor, Color.parseColor("#111111"))
        view.setThemeColors(bgColor, activeColor, inactiveColor, indicatorColor)
      }

      Prop("bottomInset") { view: TelegramTabBarView, inset: Float ->
        view.setBottomInset(inset.toInt())
      }

      Prop("isVisible") { view: TelegramTabBarView, visible: Boolean ->
        view.setIsVisible(visible)
      }

      Prop("tabBadges") { view: TelegramTabBarView, badges: List<TabBadgeRecord>? ->
        if (badges == null) {
          view.setBadges(emptyMap())
          view.setDotBadges(emptySet())
          return@Prop
        }
        val numericBadges = mutableMapOf<String, Int>()
        val dotBadges = mutableSetOf<String>()
        badges.forEach { badge ->
          if (badge.key.isEmpty()) return@forEach
          if (badge.isDot) dotBadges.add(badge.key)
          else numericBadges[badge.key] = badge.count
        }
        view.setBadges(numericBadges)
        view.setDotBadges(dotBadges)
      }

      Prop("iconMap") { _: TelegramTabBarView, _: Any? ->
        // Legacy no-op — icons come from TabItemRecord.iconName
      }
    }
  }

  private fun parseColor(value: String?, default: Int): Int {
    if (value.isNullOrBlank()) return default
    return try { Color.parseColor(value) } catch (e: Exception) { default }
  }
}
