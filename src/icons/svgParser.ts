/**
 * @file icons/svgParser.ts
 * @description Простой парсер SVG-строк в формат IconNode.
 *              Simple SVG string parser that produces IconNode data.
 *
 * Предназначен для быстрого создания иконок из SVG-строк без необходимости
 * вручную разбирать атрибуты. Поддерживает элементы: path, circle, line.
 *
 * Intended for quickly creating icons from SVG strings without manually
 * parsing attributes. Supports elements: path, circle, line.
 *
 * Ограничения / Limitations:
 * - Не поддерживает вложенные элементы (groups, transforms).
 * - Не поддерживает polygon/polyline (используйте createLucideIconData).
 * - Использует регулярные выражения, что ненадёжно для сложных SVG.
 *
 * Does not support:
 * - Nested elements (groups, transforms)
 * - polygon/polyline (use createLucideIconData instead)
 * - Robust SVG parsing (regex-based, may fail on complex SVG)
 */

import type { IconNode } from '../tabBar/types';

/**
 * Создаёт данные иконки из SVG-строки.
 * Creates icon data from an SVG string.
 *
 * @param svgString - Строка SVG-разметки (может содержать весь тег <svg>).
 *                    SVG markup string (may contain the full <svg> tag).
 * @returns Массив кортежей IconNode, совместимый с TelegramTabBar.
 *          IconNode tuple array compatible with TelegramTabBar.
 *
 * @example
 * ```ts
 * const myIcon = createCustomIconData(`
 *   <svg viewBox="0 0 24 24">
 *     <path d="M10 10 L20 20"/>
 *     <circle cx="5" cy="5" r="3"/>
 *   </svg>
 * `);
 * ```
 */
export function createCustomIconData(svgString: string): IconNode {
  const elements: [string, Record<string, string>][] = [];

  // --- Парсим <path d="..."> ---
  // --- Parse <path d="..."> ---
  const pathRegex = /<path\s+([^>]+)>/g;
  let match: RegExpExecArray | null;

  while ((match = pathRegex.exec(svgString)) !== null) {
    const attrs = match[1];
    const dMatch = /d="([^"]+)"/.exec(attrs);
    if (dMatch) {
      elements.push(['path', { d: dMatch[1] }]);
    }
  }

  // --- Парсим <circle cx="..." cy="..." r="..."> ---
  // --- Parse <circle cx="..." cy="..." r="..."> ---
  const circleRegex = /<circle\s+([^>]+)>/g;

  while ((match = circleRegex.exec(svgString)) !== null) {
    const attrs = match[1];
    const cxMatch = /cx="([^"]+)"/.exec(attrs);
    const cyMatch = /cy="([^"]+)"/.exec(attrs);
    const rMatch = /r="([^"]+)"/.exec(attrs);
    if (cxMatch && cyMatch && rMatch) {
      elements.push([
        'circle',
        { cx: cxMatch[1], cy: cyMatch[1], r: rMatch[1] },
      ]);
    }
  }

  // --- Парсим <line x1="..." y1="..." x2="..." y2="..."> ---
  // --- Parse <line x1="..." y1="..." x2="..." y2="..."> ---
  const lineRegex = /<line\s+([^>]+)>/g;

  while ((match = lineRegex.exec(svgString)) !== null) {
    const attrs = match[1];
    const x1Match = /x1="([^"]+)"/.exec(attrs);
    const y1Match = /y1="([^"]+)"/.exec(attrs);
    const x2Match = /x2="([^"]+)"/.exec(attrs);
    const y2Match = /y2="([^"]+)"/.exec(attrs);
    if (x1Match && y1Match && x2Match && y2Match) {
      elements.push([
        'line',
        {
          x1: x1Match[1],
          y1: y1Match[1],
          x2: x2Match[1],
          y2: y2Match[1],
        },
      ]);
    }
  }

  return elements;
}
