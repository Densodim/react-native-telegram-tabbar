/**
 * TelegramTabBarViewManager.swift
 *
 * React Native ViewManager for TelegramTabBarView.
 * Bridges JS props → Swift, and wires RCTBubblingEventBlock callbacks.
 *
 * All prop setters run on the main queue (methodQueue returns .main).
 * The ObjC-side registration lives in TelegramTabBarViewManager.m.
 */

import Foundation
import React

@objc(TelegramTabBarViewManager)
final class TelegramTabBarViewManager: RCTViewManager {

    // MARK: - Boilerplate

    // Explicitly declare the JS component name.
    // Without this override, RCTComponentData derives the name by stripping the
    // "Manager" suffix from the class name → "TelegramTabBarView", which does NOT
    // match requireNativeComponent('TelegramTabBar') on the JS side.
    @objc override class func moduleName() -> String! { "TelegramTabBar" }

    override class func requiresMainQueueSetup() -> Bool { true }

    override var methodQueue: DispatchQueue { .main }

    override func view() -> UIView! {
        TelegramTabBarView()
    }

    // MARK: - Prop setters

    /// `tabs` — array of tab descriptor dicts from JS.
    /// Each dict: { key: string, title: string, svgPaths?: SvgElement[] }
    @objc func setTabs(_ view: TelegramTabBarView, tabs rawTabs: NSArray?) {
        let dicts = rawTabs as? [[String: Any]] ?? []
        let tabData: [TabData] = dicts.compactMap { dict in
            guard let key   = dict["key"]   as? String,
                  let title = dict["title"] as? String else { return nil }

            let svgRaw  = dict["svgPaths"] as? [[String: Any]] ?? []
            let svgPaths = svgRaw.map { SvgElement(from: $0) }
            return TabData(key: key, title: title, svgPaths: svgPaths)
        }
        view.setTabs(tabData)
    }

    /// `activeIndex` — zero-based index of the currently selected tab.
    @objc func setActiveIndex(_ view: TelegramTabBarView, activeIndex: NSNumber) {
        view.setActiveIndex(activeIndex.intValue)
    }

    /// `theme` — colour theme dict: { backgroundColor, activeColor, inactiveColor, indicatorColor }
    @objc func setTheme(_ view: TelegramTabBarView, theme rawTheme: NSDictionary?) {
        guard let dict = rawTheme as? [String: Any] else { return }
        var t = TabBarTheme()
        if let v = dict["backgroundColor"] as? String { t.backgroundColor = v }
        if let v = dict["activeColor"]     as? String { t.activeColor     = v }
        if let v = dict["inactiveColor"]   as? String { t.inactiveColor   = v }
        if let v = dict["indicatorColor"]  as? String { t.indicatorColor  = v }
        view.setThemeColors(t)
    }

    /// `bottomInset` — safe-area bottom inset in **points** from JS (useSafeAreaInsets).
    @objc func setBottomInset(_ view: TelegramTabBarView, bottomInset: NSNumber) {
        view.setBottomInset(CGFloat(bottomInset.floatValue))
    }

    /// `isVisible` — show / hide the tab bar (for hide-on-scroll).
    @objc func setIsVisible(_ view: TelegramTabBarView, isVisible: Bool) {
        view.setTabVisible(isVisible)
    }

    /// `tabBadges` — array of badge descriptors.
    /// Stub: forwarded to the view's badge handler for future implementation.
    @objc func setTabBadges(_ view: TelegramTabBarView, tabBadges: NSArray?) {
        let dicts = tabBadges as? [[String: Any]] ?? []
        view.setBadges(dicts)
    }

    /// `onTabPress` — bubbling event fired when a tab is tapped.
    @objc func setOnTabPress(
        _ view: TelegramTabBarView,
        onTabPress: @escaping RCTBubblingEventBlock
    ) {
        view.onTabPress = onTabPress
    }

    /// `onTabLongPress` — bubbling event fired when a tab is long-pressed.
    @objc func setOnTabLongPress(
        _ view: TelegramTabBarView,
        onTabLongPress: @escaping RCTBubblingEventBlock
    ) {
        view.onTabLongPress = onTabLongPress
    }
}
