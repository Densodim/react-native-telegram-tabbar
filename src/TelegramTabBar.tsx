/**
 * @file TelegramTabBar.tsx
 * @description Telegram-style floating tab bar for React Navigation / Expo Router.
 *              Works on iOS and Android — no Expo dependency.
 */

import React from 'react'
import { Platform, View } from 'react-native'
import { useSafeAreaInsets } from 'react-native-safe-area-context'
import { NativeTelegramTabBarView } from './NativeTelegramTabBar'
import {
  DEFAULT_THEME,
  EXTRA_PADDING,
  FLOATING_MARGIN_BOTTOM,
  TAB_BAR_HEIGHT,
} from './tabBar/constants'
import { useBadges } from './tabBar/useBadges'
import { useTabs } from './tabBar/useTabs'
import { useTabHandlers } from './tabBar/useTabHandlers'
import { useVisibleRoutes } from './tabBar/useVisibleRoutes'
import type { TabBadgeItem } from './NativeTelegramTabBar'

export { iconNodesToSvg } from './tabBar/iconNodesToSvg'
export type {
  IconNode,
  TelegramTabBarCustomProps,
  TelegramTabBarProps,
} from './tabBar/types'

import type { TelegramTabBarProps } from './tabBar/types'

export function TelegramTabBar({
  state,
  descriptors,
  navigation,
  theme: customTheme,
  icons,
  iconNodes,
  isVisible,
}: TelegramTabBarProps) {
  const { bottom } = useSafeAreaInsets()

  const visibleRoutes = useVisibleRoutes(state.routes, descriptors)
  const tabs = useTabs(visibleRoutes, descriptors, icons, iconNodes)
  const activeIndex = visibleRoutes.findIndex(
    r => r.key === state.routes[state.index].key,
  )
  const { badges, dotBadges } = useBadges(visibleRoutes, descriptors)
  const activeRouteKey = state.routes[state.index].key
  const { handleTabPress, handleTabLongPress } = useTabHandlers(
    visibleRoutes,
    activeRouteKey,
    navigation,
  )

  const theme = customTheme ?? DEFAULT_THEME

  // Convert badges + dotBadges -> unified tabBadges array
  const tabBadges: TabBadgeItem[] = React.useMemo(() => {
    const items: TabBadgeItem[] = []
    Object.entries(badges).forEach(([key, count]) => {
      items.push({ key, count, isDot: false })
    })
    dotBadges.forEach(key => {
      if (!badges[key]) items.push({ key, count: 0, isDot: true })
    })
    return items
  }, [badges, dotBadges])

  const totalHeight =
    TAB_BAR_HEIGHT + FLOATING_MARGIN_BOTTOM + bottom + EXTRA_PADDING

  if (Platform.OS === 'android' || Platform.OS === 'ios') {
    return (
      <View
        style={{
          position: 'absolute',
          bottom: 0,
          left: 0,
          right: 0,
          pointerEvents: 'box-none',
        }}
      >
        <NativeTelegramTabBarView
          tabs={tabs}
          activeIndex={activeIndex}
          theme={theme}
          tabBadges={tabBadges}
          bottomInset={bottom}
          isVisible={isVisible ?? true}
          onTabPress={handleTabPress}
          onTabLongPress={handleTabLongPress}
          style={{ height: totalHeight, width: '100%' }}
        />
      </View>
    )
  }

  return null
}
