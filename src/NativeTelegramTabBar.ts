/**
 * @file NativeTelegramTabBar.ts
 * @description Native component bridge — works on both iOS and Android
 *              via requireNativeComponent (React Native ViewManager).
 *              No Expo dependency.
 */

import { requireNativeComponent } from 'react-native'
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
 * The name 'TelegramTabBar' must match getName() in TelegramTabBarViewManager
 * (Android) and the RCT_EXTERN_MODULE name in TelegramTabBarViewManager.m (iOS).
 */
export const NativeTelegramTabBarView =
  requireNativeComponent<NativeTelegramTabBarProps>('TelegramTabBar')
