/**
 * @file tabBar/types.ts
 * @description Типы, специфичные для React-компонента TelegramTabBar.
 *              Types specific to the TelegramTabBar React component.
 *
 * Здесь определяются типы, которые относятся к публичному API компонента
 * и не являются частью нативного мостового слоя.
 *
 * Here we define types that belong to the component's public API
 * and are not part of the native bridge layer.
 */

import type { BottomTabBarProps } from '@react-navigation/bottom-tabs';
import type { TabBarTheme } from '../types/tab';

/**
 * Формат данных иконки Lucide: массив кортежей [тип_элемента, атрибуты].
 *
 * Соответствует внутреннему формату createLucideIcon из lucide-react-native.
 * Поддерживает как изменяемые, так и readonly-массивы (для использования
 * с as const).
 *
 * Lucide icon data format: array of [elementType, attributes] tuples.
 *
 * Matches the internal format used by createLucideIcon from lucide-react-native.
 * Supports both mutable and readonly arrays (for use with as const).
 */
export type IconNode = readonly (readonly [string, Record<string, string>])[];

/**
 * Дополнительные пропсы, добавляемые поверх стандартных BottomTabBarProps.
 * Additional props added on top of the standard BottomTabBarProps.
 *
 * Используйте этот интерфейс при создании обёртки вокруг TelegramTabBar.
 * Use this interface when wrapping TelegramTabBar in your own component.
 */
export interface TelegramTabBarCustomProps {
  /**
   * Цветовая тема панели.
   * Tab bar color theme.
   */
  theme?: TabBarTheme;

  /**
   * Соответствие: имя роута → имя Android drawable-ресурса.
   * Устаревший способ задания иконок, используйте iconNodes.
   *
   * Route name → Android drawable resource name mapping.
   * Legacy icon approach, prefer iconNodes instead.
   *
   * @deprecated Use iconNodes with SVG data instead.
   */
  icons?: Record<string, string>;

  /**
   * Соответствие: имя роута → данные иконки в формате Lucide.
   * Предпочтительный способ задания иконок на Android.
   *
   * Route name → Lucide icon node data mapping.
   * Preferred way to set icons on Android.
   *
   * @example
   * ```ts
   * import { LUCIDE_ICONS } from 'react-native-telegram-tabbar';
   *
   * <TelegramTabBar iconNodes={LUCIDE_ICONS} />
   * ```
   */
  iconNodes?: Record<string, IconNode>;

  /**
   * Управляет видимостью панели (для анимации скрытия при скролле).
   * Defaults to true.
   *
   * Controls tab bar visibility (for hide-on-scroll animation).
   * Defaults to true.
   */
  isVisible?: boolean;
}

/**
 * Полный набор пропсов компонента TelegramTabBar.
 * Объединяет стандартные пропсы React Navigation с кастомными.
 *
 * Full prop set for the TelegramTabBar component.
 * Combines standard React Navigation props with custom ones.
 */
export type TelegramTabBarProps = BottomTabBarProps & TelegramTabBarCustomProps;
