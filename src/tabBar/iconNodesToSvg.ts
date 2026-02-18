/**
 * @file tabBar/iconNodesToSvg.ts
 * @description Утилита для преобразования Lucide IconNode в SvgElement[].
 *              Utility to convert Lucide IconNode format to SvgElement[].
 *
 * Lucide-иконки хранятся как массив кортежей [elementType, attrs],
 * где attrs может содержать React-проп key. Нативный Android-слой не
 * понимает проп key, поэтому его нужно удалить перед передачей данных.
 *
 * Lucide icons are stored as an array of [elementType, attrs] tuples,
 * where attrs may contain the React key prop. The native Android layer
 * does not understand the key prop, so it must be stripped before sending.
 */

import type { SvgElement } from '../types/svg';
import type { IconNode } from './types';

/**
 * Преобразует массив Lucide icon nodes в массив SvgElement для нативного вида.
 * Converts Lucide icon nodes array to SvgElement array for the native view.
 *
 * @param nodes - Данные иконки в формате Lucide (массив кортежей).
 *                Icon data in Lucide format (array of tuples).
 * @returns Массив SvgElement без React-пропа key.
 *          Array of SvgElement without the React key prop.
 *
 * @example
 * ```ts
 * const nodes: IconNode = [
 *   ["path", { d: "M15 21v-8...", key: "path-0" }],
 *   ["circle", { cx: "12", cy: "12", r: "4", key: "circle-0" }],
 * ];
 * const svgElements = iconNodesToSvg(nodes);
 * // => [{ type: "path", d: "M15 21v-8..." }, { type: "circle", cx: "12", cy: "12", r: "4" }]
 * ```
 */
export function iconNodesToSvg(nodes: IconNode): SvgElement[] {
  return nodes.map(([type, attrs]) => {
    // Удаляем React-проп key — нативный слой его не ожидает.
    // Strip the React key prop — the native layer does not expect it.
    const { key, ...rest } = attrs;
    return { type, ...rest };
  });
}
