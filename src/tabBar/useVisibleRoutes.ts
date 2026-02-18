/**
 * @file tabBar/useVisibleRoutes.ts
 * @description Хук для фильтрации скрытых роутов из панели навигации.
 *              Hook to filter hidden routes from the tab bar.
 *
 * React Navigation и Expo Router поддерживают несколько способов скрыть
 * вкладку из панели навигации:
 *   1. Установить href={null} (Expo Router)
 *   2. Установить tabBarItemStyle={{ display: 'none' }}
 *
 * React Navigation and Expo Router support multiple ways to hide a tab:
 *   1. Set href={null} (Expo Router)
 *   2. Set tabBarItemStyle={{ display: 'none' }}
 */

import { useMemo } from 'react';
import type { Route } from '@react-navigation/native';
import type { BottomTabDescriptorMap } from '@react-navigation/bottom-tabs';

/**
 * Возвращает только видимые роуты, исключая скрытые через Expo Router или
 * tabBarItemStyle.
 *
 * Returns only visible routes, excluding ones hidden via Expo Router or
 * tabBarItemStyle.
 *
 * @param routes      - Все роуты из state.routes React Navigation.
 *                      All routes from React Navigation state.routes.
 * @param descriptors - Дескрипторы роутов React Navigation.
 *                      React Navigation route descriptors.
 */
export function useVisibleRoutes(
  routes: Route<string>[],
  descriptors: BottomTabDescriptorMap,
): Route<string>[] {
  return useMemo(
    () =>
      routes.filter(route => {
        const { options } = descriptors[route.key];

        // Expo Router: href={null} полностью скрывает вкладку.
        // Expo Router: href={null} completely hides the tab.
        if ((options as Record<string, unknown>).href === null) return false;

        // tabBarItemStyle={{ display: 'none' }} скрывает вкладку.
        // tabBarItemStyle={{ display: 'none' }} hides the tab.
        const itemStyle = options.tabBarItemStyle as
          | { display?: string }
          | undefined;
        if (itemStyle?.display === 'none') return false;

        return true;
      }),
    [routes, descriptors],
  );
}
