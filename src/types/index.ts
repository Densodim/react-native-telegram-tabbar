/**
 * @file types/index.ts
 * @description Точка входа для всех публичных типов библиотеки.
 *              Entry point for all public library types.
 *
 * Реэкспортирует типы из подмодулей, чтобы потребители могли импортировать
 * всё из одного места:
 *   import type { SvgElement, TabItem, TabBarTheme } from 'react-native-telegram-tabbar';
 *
 * Re-exports types from sub-modules so consumers can import from one place:
 *   import type { SvgElement, TabItem, TabBarTheme } from 'react-native-telegram-tabbar';
 */

export type { SvgElement } from './svg';
export type { TabItem, TabBarTheme } from './tab';
export type { TabPressEvent, TabLongPressEvent } from './events';
export type { NativeTelegramTabBarProps } from './nativeProps';
