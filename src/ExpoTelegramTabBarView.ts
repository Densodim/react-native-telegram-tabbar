/**
 * @file ExpoTelegramTabBarView.ts
 * @description Мост к нативному Android-виду через Expo Modules.
 *              Bridge to the native Android view via Expo Modules.
 *
 * Этот файл регистрирует нативный вид 'ExpoTelegramTabBar' (объявленный
 * в Kotlin-классе ExpoTelegramTabBarModule) как React-компонент и
 * реэкспортирует все публичные типы, чтобы потребители не зависели от
 * внутренней структуры папки types/.
 *
 * This file registers the native view 'ExpoTelegramTabBar' (declared in
 * the Kotlin ExpoTelegramTabBarModule class) as a React component and
 * re-exports all public types so consumers don't depend on the internal
 * types/ folder structure.
 */

import { requireNativeView } from 'expo';
import type { NativeTelegramTabBarProps } from './types/nativeProps';

// --- Реэкспорт типов для удобства потребителей ---
// --- Re-export types for consumer convenience ---

export type { SvgElement } from './types/svg';
export type { TabItem, TabBarTheme } from './types/tab';
export type { TabPressEvent, TabLongPressEvent } from './types/events';
export type { NativeTelegramTabBarProps } from './types/nativeProps';

/**
 * Нативный React-компонент, обёртывающий Kotlin-вид TelegramTabBarView.
 *
 * Имя 'ExpoTelegramTabBar' должно точно совпадать со строкой, переданной
 * в вызов View { ... } внутри ExpoTelegramTabBarModule.kt.
 *
 * Native React component wrapping the Kotlin TelegramTabBarView.
 *
 * The name 'ExpoTelegramTabBar' must exactly match the string passed to
 * the View { ... } call inside ExpoTelegramTabBarModule.kt.
 */
export const NativeTelegramTabBarView =
  requireNativeView<NativeTelegramTabBarProps>('ExpoTelegramTabBar');
