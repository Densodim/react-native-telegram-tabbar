/**
 * @file TelegramTabBar.tsx
 * @description Telegram-style floating tab bar for React Navigation / Expo Router.
 *
 * Icons and labels are driven by custom `tabBarIconName` / `tabBarLabel` screen
 * options. The native Compose layer renders everything — no RN overlay needed.
 */

import React from 'react'
import { Platform, StyleSheet, View } from 'react-native'
import { useSafeAreaInsets } from 'react-native-safe-area-context'
import { NativeTelegramTabBarView } from './NativeTelegramTabBar'
import {
  DEFAULT_THEME,
  EXTRA_PADDING,
  FLOATING_MARGIN_BOTTOM,
  TAB_BAR_HEIGHT,
} from './tabBar/constants'
import { useBadges } from './tabBar/useBadges'
import { useTabHandlers } from './tabBar/useTabHandlers'
import { useVisibleRoutes } from './tabBar/useVisibleRoutes'
import type { TabBadgeItem } from './NativeTelegramTabBar'
import type { TelegramTabBarProps } from './tabBar/types'

export type { TelegramTabBarCustomProps, TelegramTabBarProps } from './tabBar/types'

export function TelegramTabBar({
  state,
  descriptors,
  navigation,
  theme: customTheme,
  isVisible,
}: TelegramTabBarProps) {
  const { bottom } = useSafeAreaInsets()
  const theme = customTheme ?? DEFAULT_THEME

  const visibleRoutes = useVisibleRoutes(state.routes, descriptors)

  // Each tab passes iconName + title to the native Compose layer.
  const nativeTabs = React.useMemo(
    () =>
      visibleRoutes.map(r => {
        const opts = descriptors[r.key].options as Record<string, unknown>
        const iconName = (opts.tabBarIconName as string | undefined) ?? undefined
        const title =
          typeof opts.tabBarLabel === 'string'
            ? opts.tabBarLabel
            : typeof opts.title === 'string'
              ? opts.title
              : r.name
        return { key: r.name, title, iconName }
      }),
    [visibleRoutes, descriptors],
  )

  const rawActiveIndex = visibleRoutes.findIndex(
    r => r.key === state.routes[state.index].key,
  )
  const activeIndex = rawActiveIndex >= 0 ? rawActiveIndex : 0

  const { badges, dotBadges } = useBadges(visibleRoutes, descriptors)
  const activeRouteKey = state.routes[state.index].key
  const { handleTabPress, handleTabLongPress } = useTabHandlers(
    visibleRoutes,
    activeRouteKey,
    navigation,
  )

  const tabBadges: TabBadgeItem[] = React.useMemo(() => {
    const items: TabBadgeItem[] = []
    Object.entries(badges).forEach(([key, count]) =>
      items.push({ key, count, isDot: false }),
    )
    dotBadges.forEach(key => {
      if (!badges[key]) items.push({ key, count: 0, isDot: true })
    })
    return items
  }, [badges, dotBadges])

  const totalHeight = TAB_BAR_HEIGHT + FLOATING_MARGIN_BOTTOM + bottom + EXTRA_PADDING

  if (Platform.OS !== 'android' && Platform.OS !== 'ios') return null

  return (
    <View style={styles.container}>
      {/* Native layer renders pill + blur + icons + labels + white card + badges */}
      <NativeTelegramTabBarView
        tabs={nativeTabs}
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

const styles = StyleSheet.create({
  container: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    pointerEvents: 'box-none',
  } as const,
})
