/**
 * @file TelegramTabBar.tsx
 * @description Главный React-компонент панели вкладок в стиле Telegram.
 *              Main React component for the Telegram-style tab bar.
 *
 * Является drop-in заменой стандартной панели вкладок React Navigation.
 * На Android использует нативный Kotlin-вид TelegramTabBarView —
 * плавающую pill-панель с размытым фоном (API 31+), скруглёнными углами,
 * тенью, скользящим индикатором и рябью Material.
 *
 * Acts as a drop-in replacement for React Navigation's bottom tab bar.
 * On Android, renders the native Kotlin TelegramTabBarView — a floating
 * pill with blur background (API 31+), rounded corners, elevation shadow,
 * sliding indicator, and Material ripple.
 *
 * На iOS/Web возвращает null (используйте собственный компонент).
 * On iOS/Web returns null (bring your own tab bar component).
 *
 * @example Использование с Expo Router / Usage with Expo Router:
 * ```tsx
 * import { Tabs } from 'expo-router';
 * import { TelegramTabBar, LUCIDE_ICONS } from 'react-native-telegram-tabbar';
 *
 * export default function Layout() {
 *   return (
 *     <Tabs
 *       tabBar={props => (
 *         <TelegramTabBar
 *           {...props}
 *           iconNodes={LUCIDE_ICONS}
 *           theme={{ backgroundColor: '#1C1C1E', activeColor: '#0A84FF',
 *                    inactiveColor: '#636366', indicatorColor: '#0A84FF' }}
 *         />
 *       )}
 *     />
 *   );
 * }
 * ```
 */

import React from 'react';
import { Platform, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { NativeTelegramTabBarView } from './ExpoTelegramTabBarView';
import { DEFAULT_THEME, EXTRA_PADDING, FLOATING_MARGIN_BOTTOM, TAB_BAR_HEIGHT } from './tabBar/constants';
import { useBadges } from './tabBar/useBadges';
import { useTabs } from './tabBar/useTabs';
import { useTabHandlers } from './tabBar/useTabHandlers';
import { useVisibleRoutes } from './tabBar/useVisibleRoutes';

// Реэкспорт публичного API tabBar-подмодуля.
// Re-export public API from the tabBar sub-module.
export { iconNodesToSvg } from './tabBar/iconNodesToSvg';
export type { IconNode, TelegramTabBarCustomProps, TelegramTabBarProps } from './tabBar/types';

import type { TelegramTabBarProps } from './tabBar/types';

/**
 * Панель вкладок в стиле Telegram для React Navigation / Expo Router.
 * Telegram-style tab bar for React Navigation / Expo Router.
 *
 * @see TelegramTabBarProps для полного описания пропсов.
 * @see TelegramTabBarProps for full prop documentation.
 */
export function TelegramTabBar({
  state,
  descriptors,
  navigation,
  theme: customTheme,
  icons,
  iconNodes,
  isVisible,
}: TelegramTabBarProps) {
  // Получаем отступ нижней системной панели навигации.
  // Get the bottom system navigation bar inset.
  const { bottom } = useSafeAreaInsets();

  // Шаг 1: Отфильтровываем скрытые роуты.
  // Step 1: Filter out hidden routes.
  const visibleRoutes = useVisibleRoutes(state.routes, descriptors);

  // Шаг 2: Строим массив вкладок для нативного вида.
  // Step 2: Build the tab item array for the native view.
  const tabs = useTabs(visibleRoutes, descriptors, icons, iconNodes);

  // Шаг 3: Находим индекс активной вкладки среди видимых.
  // Step 3: Find the active tab index among visible routes.
  const activeIndex = visibleRoutes.findIndex(
    r => r.key === state.routes[state.index].key,
  );

  // Шаг 4: Разбиваем бейджи на числовые и точечные.
  // Step 4: Split badges into numeric and dot categories.
  const { badges, dotBadges } = useBadges(visibleRoutes, descriptors);

  // Шаг 5: Создаём обработчики событий нажатия.
  // Step 5: Create tab press event handlers.
  const activeRouteKey = state.routes[state.index].key;
  const { handleTabPress, handleTabLongPress } = useTabHandlers(
    visibleRoutes,
    activeRouteKey,
    navigation,
  );

  // Применяем тему или используем тему по умолчанию.
  // Apply provided theme or fall back to the default.
  const theme = customTheme ?? DEFAULT_THEME;

  // Общая высота = высота панели + нижний отступ + безопасный отступ + доп. отступ.
  // Total height = bar height + floating bottom margin + safe area inset + extra padding.
  const totalHeight =
    TAB_BAR_HEIGHT + FLOATING_MARGIN_BOTTOM + bottom + EXTRA_PADDING;

  // Android: рендерим нативный плавающий вид.
  // Android: render the native floating view.
  if (Platform.OS === 'android') {
    return (
      <View
        style={{
          position: 'absolute',
          bottom: 0,
          left: 0,
          right: 0,
          // box-none: View не перехватывает касания, но дочерние элементы — да.
          // box-none: View doesn't intercept touches, but children do.
          pointerEvents: 'box-none',
        }}
      >
        <NativeTelegramTabBarView
          tabs={tabs}
          activeIndex={activeIndex}
          theme={theme}
          badges={badges}
          dotBadges={dotBadges}
          bottomInset={bottom}
          isVisible={isVisible ?? true}
          onTabPress={handleTabPress}
          onTabLongPress={handleTabLongPress}
          style={{
            height: totalHeight,
            width: '100%',
          }}
        />
      </View>
    );
  }

  // iOS / Web: возвращаем null — используйте свой компонент панели вкладок.
  // iOS / Web: return null — use your own tab bar component.
  return null;
}
