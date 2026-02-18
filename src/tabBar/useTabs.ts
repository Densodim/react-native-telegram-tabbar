/**
 * @file tabBar/useTabs.ts
 * @description Хук для построения массива TabItem из роутов React Navigation.
 *              Hook to build the TabItem array from React Navigation routes.
 *
 * Каждый видимый роут преобразуется в объект TabItem, содержащий:
 *   - key       — имя роута (используется нативным слоем для идентификации)
 *   - title     — отображаемое название вкладки
 *   - icon      — имя drawable-ресурса (устаревший способ)
 *   - svgPaths  — SVG-элементы иконки (предпочтительный способ)
 *
 * Each visible route is converted to a TabItem containing:
 *   - key       — route name (used by the native layer for identification)
 *   - title     — display label for the tab
 *   - icon      — drawable resource name (legacy approach)
 *   - svgPaths  — SVG elements for the icon (preferred approach)
 */

import { useMemo } from 'react';
import type { Route } from '@react-navigation/native';
import type { BottomTabDescriptorMap } from '@react-navigation/bottom-tabs';
import type { TabItem } from '../types/tab';
import type { IconNode } from './types';
import { iconNodesToSvg } from './iconNodesToSvg';

/**
 * Строит массив TabItem для передачи в NativeTelegramTabBarView.
 * Builds the TabItem array to pass to NativeTelegramTabBarView.
 *
 * @param visibleRoutes - Видимые роуты (от useVisibleRoutes).
 *                        Visible routes (from useVisibleRoutes).
 * @param descriptors   - Дескрипторы роутов React Navigation.
 *                        React Navigation route descriptors.
 * @param icons         - Устаревшее: соответствие имён роутов и drawable-ресурсов.
 *                        Legacy: route name to drawable resource name mapping.
 * @param iconNodes     - Соответствие имён роутов и данных иконок Lucide.
 *                        Route name to Lucide icon node data mapping.
 */
export function useTabs(
  visibleRoutes: Route<string>[],
  descriptors: BottomTabDescriptorMap,
  icons?: Record<string, string>,
  iconNodes?: Record<string, IconNode>,
): TabItem[] {
  return useMemo(
    () =>
      visibleRoutes.map(route => {
        const { options } = descriptors[route.key];

        // Определяем label: tabBarLabel > title > имя роута.
        // Resolve label: tabBarLabel > title > route name.
        const label = options.tabBarLabel
          ? String(options.tabBarLabel)
          : (options.title ?? route.name);

        // Если есть данные иконки Lucide — конвертируем в SvgElement[].
        // If Lucide icon data is available — convert to SvgElement[].
        const nodes = iconNodes?.[route.name];
        const svgPaths = nodes ? iconNodesToSvg(nodes) : undefined;

        return {
          key: route.name,
          title: label,
          // Устаревший fallback: имя drawable или имя роута.
          // Legacy fallback: drawable name or route name.
          icon: icons?.[route.name] ?? route.name,
          svgPaths,
        };
      }),
    [visibleRoutes, descriptors, icons, iconNodes],
  );
}
