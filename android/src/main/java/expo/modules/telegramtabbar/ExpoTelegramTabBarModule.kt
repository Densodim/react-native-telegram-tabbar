package expo.modules.telegramtabbar

import android.graphics.Color
import android.util.Log
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class ExpoTelegramTabBarModule : Module() {
    override fun definition() = ModuleDefinition {
        Name("ExpoTelegramTabBar")

        View(TelegramTabBarView::class) {
            Events("onTabPress", "onTabLongPress")

            Prop("tabs") { view: TelegramTabBarView, tabs: List<Map<String, Any?>> ->
                val items = tabs.map { map ->
                    val svgPathsRaw = map["svgPaths"] as? List<*>
                    val svgPaths = svgPathsRaw?.mapNotNull { parseSvgElement(it) }

                    TelegramTabBarView.TabItem(
                        key = map["key"] as? String ?: "",
                        title = map["title"] as? String ?: "",
                        icon = map["icon"] as? String,
                        svgPaths = svgPaths
                    )
                }
                view.setTabs(items)
            }

            Prop("activeIndex") { view: TelegramTabBarView, index: Int ->
                view.setActiveIndex(index)
            }

            Prop("badges") { view: TelegramTabBarView, badges: Map<String, Any?>? ->
                val parsed = badges?.mapValues { (_, v) ->
                    when (v) {
                        is Number -> v.toInt()
                        is String -> v.toIntOrNull() ?: 0
                        else -> 0
                    }
                } ?: emptyMap()
                view.setBadges(parsed)
            }

            Prop("dotBadges") { view: TelegramTabBarView, dotBadges: List<String>? ->
                view.setDotBadges(dotBadges?.toSet() ?: emptySet())
            }

            Prop("theme") { view: TelegramTabBarView, theme: Map<String, Any?>? ->
                if (theme != null) {
                    val bg = parseColor(theme["backgroundColor"] as? String, Color.WHITE)
                    val active = parseColor(theme["activeColor"] as? String, Color.parseColor("#007AFF"))
                    val inactive = parseColor(theme["inactiveColor"] as? String, Color.parseColor("#3C3C43"))
                    val indicator = parseColor(theme["indicatorColor"] as? String, active)
                    Log.d("TelegramTabBar", "Theme colors — bg: ${String.format("#%08X", bg)}, active: ${String.format("#%08X", active)}, inactive: ${String.format("#%08X", inactive)}")
                    view.setThemeColors(bg, active, inactive, indicator)
                }
            }

            Prop("bottomInset") { view: TelegramTabBarView, inset: Int ->
                view.setBottomInset(inset)
            }

            Prop("isVisible") { view: TelegramTabBarView, visible: Boolean ->
                view.setIsVisible(visible)
            }
        }
    }

    private fun parseColor(colorString: String?, default: Int): Int {
        if (colorString.isNullOrEmpty()) return default
        return try {
            Color.parseColor(colorString)
        } catch (e: IllegalArgumentException) {
            default
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseSvgElement(raw: Any?): TelegramTabBarView.SvgElement? {
        val map = raw as? Map<String, Any?> ?: return null
        return TelegramTabBarView.SvgElement(
            type = map["type"] as? String ?: return null,
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
            ry = map["ry"] as? String
        )
    }
}
