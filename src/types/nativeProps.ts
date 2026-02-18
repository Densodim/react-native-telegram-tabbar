/**
 * @file nativeProps.ts
 * @description Пропсы нативного React-компонента ExpoTelegramTabBar.
 *              Props for the native React component ExpoTelegramTabBar.
 *
 * Этот интерфейс описывает полный набор свойств, которые принимает нативный
 * вид. Все поля сериализуются через Expo Modules Bridge в Kotlin-код модуля.
 *
 * This interface describes the full set of properties accepted by the native
 * view. All fields are serialized through the Expo Modules Bridge into
 * the Kotlin module code.
 */

import type { ViewProps } from 'react-native';
import type { TabBarTheme, TabItem } from './tab';
import type { TabLongPressEvent, TabPressEvent } from './events';

/**
 * Все пропсы, поддерживаемые нативным видом NativeTelegramTabBarView.
 * All props supported by the NativeTelegramTabBarView native view.
 */
export interface NativeTelegramTabBarProps extends ViewProps {
  /**
   * Список вкладок для отображения.
   * List of tabs to display.
   */
  tabs: TabItem[];

  /**
   * Индекс активной вкладки в массиве tabs.
   * Index of the active tab within the tabs array.
   */
  activeIndex: number;

  /**
   * Цветовая тема панели. Если не задана — используется светлая тема по умолчанию.
   * Tab bar color theme. Falls back to the default light theme if not provided.
   */
  theme?: TabBarTheme;

  /**
   * Числовые бейджи на вкладках: { routeName: count }.
   * Numeric badges on tabs: { routeName: count }.
   */
  badges?: Record<string, number>;

  /**
   * Список ключей вкладок, на которых должна отображаться точка-бейдж.
   * List of tab keys that should show a dot badge.
   */
  dotBadges?: string[];

  /**
   * Отступ снизу для учёта системной панели навигации Android.
   * Передаётся из useSafeAreaInsets().bottom.
   *
   * Bottom inset for Android system navigation bar.
   * Passed from useSafeAreaInsets().bottom.
   */
  bottomInset?: number;

  /**
   * Управляет видимостью панели (для hide-on-scroll).
   * По умолчанию: true.
   *
   * Controls tab bar visibility (for hide-on-scroll).
   * Defaults to true.
   */
  isVisible?: boolean;

  /**
   * Вызывается при нажатии на вкладку.
   * Called when a tab is pressed.
   */
  onTabPress?: (event: TabPressEvent) => void;

  /**
   * Вызывается при долгом нажатии на вкладку.
   * Called when a tab is long-pressed.
   */
  onTabLongPress?: (event: TabLongPressEvent) => void;
}
