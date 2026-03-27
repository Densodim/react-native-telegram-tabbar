import ExpoModulesCore
import UIKit

public final class TelegramTabBarModule: Module {
    public func definition() -> ModuleDefinition {
        Name("TelegramTabBar")

        View(TelegramTabBarView.self) {
            Prop("tabs") { (view: TelegramTabBarView, rawTabs: [[String: Any]]?) in
                let tabData: [TabData] = (rawTabs ?? []).compactMap { dict in
                    guard let key   = dict["key"]   as? String,
                          let title = dict["title"] as? String else { return nil }
                    let iconName = dict["iconName"] as? String
                    let icon     = dict["icon"]     as? String
                    let svgPaths = (dict["svgPaths"] as? [[String: Any]] ?? [])
                        .map { SvgElement(from: $0) }
                    return TabData(key: key, title: title, iconName: iconName, icon: icon, svgPaths: svgPaths)
                }
                view.setTabs(tabData)
            }

            Prop("activeIndex") { (view: TelegramTabBarView, index: Int) in
                view.setActiveIndex(index)
            }

            Prop("theme") { (view: TelegramTabBarView, dict: [String: String]?) in
                var t = TabBarTheme()
                if let v = dict?["backgroundColor"] { t.backgroundColor = v }
                if let v = dict?["activeColor"]     { t.activeColor     = v }
                if let v = dict?["inactiveColor"]   { t.inactiveColor   = v }
                if let v = dict?["indicatorColor"]  { t.indicatorColor  = v }
                if let v = dict?["badgeColor"]      { t.badgeColor      = v }
                view.setThemeColors(t)
            }

            Prop("bottomInset") { (view: TelegramTabBarView, inset: Double) in
                view.setBottomInset(CGFloat(inset))
            }

            Prop("isVisible") { (view: TelegramTabBarView, visible: Bool) in
                view.setTabVisible(visible)
            }

            Prop("tabBadges") { (view: TelegramTabBarView, badges: [[String: Any]]?) in
                view.setBadges(badges ?? [])
            }

            Events("onTabPress", "onTabLongPress")
        }
    }
}
