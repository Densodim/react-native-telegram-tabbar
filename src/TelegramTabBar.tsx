/**
 * @file TelegramTabBar.tsx
 * @description Telegram-style floating tab bar for React Navigation / Expo Router.
 *
 * Icons and labels are driven by `tabBarIcon` / `tabBarLabel` from screen options —
 * the same API as Expo Router's built-in tab bar. The native layer handles only the
 * pill shape, blur, visibility animation, and badge rendering.
 */

import React from 'react'
import { Platform, StyleSheet, Text, View } from 'react-native'
import { useSafeAreaInsets } from 'react-native-safe-area-context'
import { NativeTelegramTabBarView } from './NativeTelegramTabBar'
import {
  DEFAULT_THEME,
  EXTRA_PADDING,
  FLOATING_MARGIN_BOTTOM,
  FLOATING_MARGIN_H,
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

  // Native layer receives only route keys — no icon/label data needed.
  // Icon + label are rendered by the RN overlay below using tabBarIcon from
  // screen options, identical to how Expo Router's own tab bar works.
  const nativeTabs = React.useMemo(
    () => visibleRoutes.map(r => ({ key: r.name, title: '' })),
    [visibleRoutes],
  )

  // Clamp to 0: findIndex returns -1 when state.index points to a hidden route
  // (e.g., auth tab just after login). Prevents native from receiving -1.
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
      {/* Native layer: pill shape, blur, visibility animation, badges */}
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

      {/* RN overlay: icons + labels only — purely visual, no touch handling.
          pointerEvents='none' lets touches fall through to the native view below,
          where ContentOverlayView wrappers invoke onTabPress/onTabLongPress. */}
      <View
        pointerEvents='none'
        style={[
          styles.overlay,
          { bottom: FLOATING_MARGIN_BOTTOM + bottom, height: TAB_BAR_HEIGHT },
        ]}
      >
        {visibleRoutes.map((route, index) => {
          const { options } = descriptors[route.key]
          const isActive = index === activeIndex
          const color = isActive ? theme.activeColor : theme.inactiveColor

          const icon = options.tabBarIcon?.({ focused: isActive, color, size: 22 })
          const labelStr =
            typeof options.tabBarLabel === 'string'
              ? options.tabBarLabel
              : (options.title ?? route.name)

          return (
            <View key={route.key} style={styles.tabCell}>
              {icon}
              {options.tabBarShowLabel !== false && (
                <Text
                  style={[
                    styles.label,
                    {
                      color,
                      fontWeight: isActive ? '600' : '400',
                    },
                  ]}
                  numberOfLines={1}
                >
                  {labelStr}
                </Text>
              )}
            </View>
          )
        })}
      </View>
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
  overlay: {
    position: 'absolute',
    left: FLOATING_MARGIN_H,
    right: FLOATING_MARGIN_H,
    flexDirection: 'row',
  },
  tabCell: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  label: {
    fontSize: 10,
    marginTop: 2,
  },
})
