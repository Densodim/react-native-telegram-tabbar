/**
 * @file tabBar/constants.ts
 * @description Константы размеров панели навигации (React-сторона).
 *              Tab bar layout constants (React side).
 *
 * ВАЖНО: Значения должны точно совпадать с константами в Kotlin-файле
 * TelegramTabBarView.kt, иначе зарезервированное пространство в макете
 * не совпадёт с реальными размерами нативного вида.
 *
 * IMPORTANT: Values must exactly match the constants in the Kotlin file
 * TelegramTabBarView.kt, otherwise the reserved layout space will not
 * align with the actual size of the native view.
 */

/**
 * Высота плавающей панели вкладок в dp.
 * Height of the floating tab bar in dp.
 */
export const TAB_BAR_HEIGHT = 56;

/**
 * Горизонтальный отступ плавающей панели от краёв экрана в dp.
 * Horizontal margin of the floating pill from screen edges in dp.
 */
export const FLOATING_MARGIN_H = 16;

/**
 * Отступ снизу от края экрана до нижнего края панели в dp.
 * Bottom margin from the screen edge to the tab bar bottom in dp.
 */
export const FLOATING_MARGIN_BOTTOM = 12;

/**
 * Дополнительный отступ для учёта системной панели навигации в dp.
 * Additional padding to account for the system navigation bar in dp.
 */
export const EXTRA_PADDING = 8;

/**
 * Тема по умолчанию (светлая).
 * Default (light) theme.
 *
 * Используется, когда пользователь не передаёт проп theme в TelegramTabBar.
 * Used when the consumer does not pass a theme prop to TelegramTabBar.
 */
export const DEFAULT_THEME = {
  backgroundColor: '#FFFFFF',
  activeColor: '#007AFF',
  inactiveColor: '#3C3C43',
  indicatorColor: '#007AFF',
} as const;
