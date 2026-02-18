/**
 * @file icons/iconTypes.ts
 * @description Общие типы для работы с иконками.
 *              Common types for working with icons.
 */

import type { IconNode } from '../tabBar/types';

/**
 * Псевдоним типа для словаря иконок: имя роута → данные иконки.
 * Type alias for an icon dictionary: route name → icon data.
 *
 * Принимает как readonly (из LUCIDE_ICONS as const), так и изменяемые значения.
 * Accepts both readonly (from LUCIDE_ICONS as const) and mutable values.
 *
 * @example
 * ```ts
 * const myIcons: IconMap = {
 *   home: LUCIDE_ICONS.home,
 *   profile: createLucideIconData([["path", { d: "..." }]]),
 * };
 * ```
 */
export type IconMap = Record<string, IconNode>;
