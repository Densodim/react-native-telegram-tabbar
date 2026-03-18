/**
 * @file NativeTelegramTabBar.ts
 * @description Native component bridge — works on both iOS and Android.
 *              iOS: ExpoModulesCore (TelegramTabBarModule.swift / ExpoView).
 *              Android: React Native ViewManager (TelegramTabBarViewManager.kt).
 */

import { requireNativeViewManager } from 'expo-modules-core'
import type { ViewProps } from 'react-native'
import type { TabItem, TabBarTheme } from './types/tab'
import type { TabLongPressEvent, TabPressEvent } from './types/events'

// ─────────────────────────────────────────────────────────────────────────────
// tabBadges replaces the old badges + dotBadges split
// ─────────────────────────────────────────────────────────────────────────────

export interface TabBadgeItem {
  key: string
  count: number
  isDot: boolean
}

export interface NativeTelegramTabBarProps extends ViewProps {
  tabs: TabItem[]
  activeIndex: number
  theme?: TabBarTheme
  tabBadges?: TabBadgeItem[]
  bottomInset?: number
  isVisible?: boolean
  onTabPress?: (event: TabPressEvent) => void
  onTabLongPress?: (event: TabLongPressEvent) => void
}

/**
 * Native React component wrapping the TelegramTabBarView on Android/iOS.
 * The name 'TelegramTabBar' must match:
 *   - getName() in TelegramTabBarViewManager.kt (Android)
 *   - Name("TelegramTabBar") in TelegramTabBarModule.swift (iOS)
 */
export const NativeTelegramTabBarView =
  requireNativeViewManager<NativeTelegramTabBarProps>('TelegramTabBar')
