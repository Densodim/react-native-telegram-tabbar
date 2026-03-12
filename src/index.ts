/**
 * @file index.ts
 * @description Public API of react-native-telegram-tabbar.
 *              No Expo dependency — works on iOS and Android.
 */

// =============================================================================
// Native bridge (requireNativeComponent, no Expo)
// =============================================================================

export {
  NativeTelegramTabBarView,
  type NativeTelegramTabBarProps,
  type TabBadgeItem,
} from './NativeTelegramTabBar'

// Legacy type re-exports for backward compat
export type { SvgElement } from './types/svg'
export type { TabItem, TabBarTheme } from './types/tab'
export type { TabPressEvent, TabLongPressEvent } from './types/events'

// =============================================================================
// Main component
// =============================================================================

export {
  TelegramTabBar,
  iconNodesToSvg,
  type IconNode,
  type TelegramTabBarCustomProps,
  type TelegramTabBarProps,
} from './TelegramTabBar'

// =============================================================================
// Icon utilities
// =============================================================================

export {
  LUCIDE_ICONS,
  createLucideIconData,
  getLucideIconData,
  createCustomIconData,
  type IconMap,
} from './icons/index'
