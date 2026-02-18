/**
 * @file icons/index.ts
 * @description Точка входа для всех иконочных утилит библиотеки.
 *              Entry point for all icon utilities in the library.
 *
 * Реэкспортирует публичный API из всех подмодулей icons/.
 * Re-exports the public API from all icons/ sub-modules.
 */

export { LUCIDE_ICONS } from './lucideIcons';
export { createLucideIconData, getLucideIconData } from './lucideHelpers';
export { createCustomIconData } from './svgParser';
export type { IconMap } from './iconTypes';
