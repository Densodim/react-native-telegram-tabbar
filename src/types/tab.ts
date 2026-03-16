/**
 * @file tab.ts
 * @description Типы для вкладок и темы панели навигации.
 *              Types for tab items and the tab bar theme.
 *
 * Эти типы используются как в TypeScript-слое (React), так и в нативном
 * Android-слое (через сериализацию Expo Modules Bridge).
 *
 * These types are shared between the TypeScript (React) layer and the native
 * Android layer (via Expo Modules Bridge serialization).
 */

import type { SvgElement } from './svg';

/**
 * Описание одной вкладки, передаваемой в нативный вид.
 * Descriptor for a single tab item passed to the native view.
 */
export interface TabItem {
  /**
   * Уникальный ключ вкладки — совпадает с именем роута React Navigation.
   * Unique tab key — matches the React Navigation route name.
   */
  key: string;

  /**
   * Отображаемое название вкладки под иконкой.
   * Display label shown below the icon.
   */
  title: string;

  /**
   * Устаревший вариант: имя Android drawable-ресурса (без расширения).
   * Используется как запасной вариант, если svgPaths не задан.
   *
   * @deprecated Legacy: Android drawable resource name (without extension).
   * Used as fallback when svgPaths is not provided.
   */
  icon?: string;

  /**
   * Массив SVG-элементов, описывающих иконку вкладки.
   * Имеет приоритет над полем icon.
   *
   * Array of SVG elements describing the tab icon.
   * Takes priority over the icon field.
   */
  svgPaths?: SvgElement[];

  /**
   * Lucide icon name (camelCase). E.g. "house", "search", "messageCircle".
   * Used by the native Android layer (compose-icons/lucide) to render the icon.
   */
  iconName?: string;
}

/**
 * Цветовая тема панели навигации.
 * Color theme for the tab bar.
 *
 * Все цвета задаются в формате CSS-строк (#RRGGBB, rgba(...), и т.д.),
 * которые нативный слой парсит через android.graphics.Color.parseColor().
 *
 * All colors are CSS-style strings (#RRGGBB, rgba(...), etc.) parsed
 * by the native layer via android.graphics.Color.parseColor().
 */
export interface TabBarTheme {
  /**
   * Цвет фона панели (за вычетом размытия).
   * Background color of the tab bar (behind the blur overlay).
   */
  backgroundColor: string;

  /**
   * Цвет активной (выбранной) вкладки — иконка и текст.
   * Color of the active (selected) tab — icon and label.
   */
  activeColor: string;

  /**
   * Цвет неактивных вкладок — иконка и текст.
   * Color of inactive tabs — icon and label.
   */
  inactiveColor: string;

  /**
   * Цвет полоски-индикатора под активной вкладкой.
   * Color of the sliding indicator strip under the active tab.
   */
  indicatorColor: string;
}
