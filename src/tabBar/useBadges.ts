/**
 * @file tabBar/useBadges.ts
 * @description Хук для подготовки данных бейджей из опций роутов.
 *              Hook to prepare badge data from route options.
 *
 * React Navigation позволяет задавать бейджи через tabBarBadge.
 * Этот хук разбирает значения бейджей на две категории:
 *   - числовые бейджи (badges) — цифра или строка с числом
 *   - точечные бейджи (dotBadges) — пустая строка или 'dot'
 *
 * React Navigation allows setting badges via tabBarBadge.
 * This hook splits badge values into two categories:
 *   - numeric badges (badges) — a number or numeric string
 *   - dot badges (dotBadges) — empty string or 'dot'
 */

import { useMemo } from 'react';
import type { Route } from '@react-navigation/native';
import type { BottomTabDescriptorMap } from '@react-navigation/bottom-tabs';

/**
 * Результат хука useBadges.
 * Result of the useBadges hook.
 */
interface BadgesResult {
  /**
   * Числовые бейджи: { routeName: count }.
   * Значения > 99 нужно обрезать на стороне нативного вида.
   *
   * Numeric badges: { routeName: count }.
   * Values > 99 are clamped to "99+" by the native view.
   */
  badges: Record<string, number>;

  /**
   * Имена роутов, на которых отображается точка-бейдж.
   * Route names that should display a dot badge.
   */
  dotBadges: string[];
}

/**
 * Вычисляет badges и dotBadges из опций видимых роутов.
 * Computes badges and dotBadges from visible route options.
 *
 * @param visibleRoutes - Видимые роуты (уже отфильтрованные от скрытых).
 *                        Visible routes (already filtered from hidden ones).
 * @param descriptors   - Дескрипторы роутов React Navigation.
 *                        React Navigation route descriptors.
 */
export function useBadges(
  visibleRoutes: Route<string>[],
  descriptors: BottomTabDescriptorMap,
): BadgesResult {
  return useMemo(() => {
    const numericBadges: Record<string, number> = {};
    const dots: string[] = [];

    for (const route of visibleRoutes) {
      const { options } = descriptors[route.key];

      if (options.tabBarBadge === undefined) continue;

      const badge = options.tabBarBadge;

      if (badge === '' || badge === 'dot') {
        // Пустая строка или 'dot' → точечный бейдж.
        // Empty string or 'dot' → dot badge.
        dots.push(route.name);
      } else {
        // Число или строка с числом → числовой бейдж.
        // Убираем суффикс '+' перед парсингом (например, "99+").
        //
        // Number or numeric string → numeric badge.
        // Strip the '+' suffix before parsing (e.g., "99+").
        numericBadges[route.name] =
          typeof badge === 'number'
            ? badge
            : Number.parseInt(String(badge).replace('+', ''), 10) || 0;
      }
    }

    return { badges: numericBadges, dotBadges: dots };
  }, [visibleRoutes, descriptors]);
}
