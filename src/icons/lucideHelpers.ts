/**
 * @file icons/lucideHelpers.ts
 * @description Утилиты для работы с иконками lucide-react-native.
 *              Utilities for working with lucide-react-native icons.
 *
 * Lucide предоставляет иконки в формате массива кортежей:
 *   [["path", { d: "..." }], ["circle", { cx: "12", cy: "12", r: "4" }]]
 *
 * Эти утилиты помогают безопасно создавать и типизировать такие данные.
 *
 * Lucide provides icons as an array of tuples:
 *   [["path", { d: "..." }], ["circle", { cx: "12", cy: "12", r: "4" }]]
 *
 * These utilities help safely create and type-check such data.
 */

import type { IconNode } from '../tabBar/types';

/**
 * Типобезопасная обёртка для определения данных иконки в формате Lucide.
 * Type-safe wrapper for defining icon data in Lucide format.
 *
 * Это функция-идентичность: она не преобразует данные, но позволяет TypeScript
 * корректно вывести тип IconNode из as const литералов.
 *
 * This is an identity function: it does not transform data, but lets TypeScript
 * correctly infer the IconNode type from as const literals.
 *
 * @param iconNode - Данные иконки в формате Lucide (массив кортежей).
 *                   Icon data in Lucide format (array of tuples).
 *
 * @example
 * ```ts
 * const homeIcon = createLucideIconData([
 *   ["path", { d: "M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8" }],
 *   ["path", { d: "M3 10a2 2 0 0 1 .709-1.528l7-6..." }],
 * ]);
 * ```
 */
export function createLucideIconData(iconNode: IconNode): IconNode {
  return iconNode;
}

/**
 * Попытка автоматического извлечения данных иконки из компонента Lucide.
 * Attempt to automatically extract icon data from a Lucide component.
 *
 * @deprecated Автоматическое извлечение не поддерживается в React Native
 * из-за ограничений Metro bundler. Используйте createLucideIconData() с
 * явным указанием данных иконки.
 *
 * @deprecated Auto-extraction is not supported in React Native due to Metro
 * bundler limitations. Use createLucideIconData() with explicit icon data.
 *
 * @param LucideComponent - Компонент-иконка из lucide-react-native.
 *                          Icon component from lucide-react-native.
 * @returns Пустой массив с предупреждением в консоли.
 *          Empty array with a console warning.
 */
export function getLucideIconData(LucideComponent: any): IconNode {
  // Проверяем нестандартное свойство iconData (кастомные иконки могут его добавлять).
  // Check for a non-standard iconData property (custom icons may expose it).
  if (LucideComponent.iconData) {
    return LucideComponent.iconData;
  }

  const componentName =
    LucideComponent.displayName || LucideComponent.name;

  if (!componentName) {
    throw new Error(
      'Unable to extract icon data: component has no displayName. ' +
        'Use createLucideIconData() with explicit IconNode data instead.',
    );
  }

  // Автоматическое извлечение невозможно без динамического импорта.
  // Auto-extraction is impossible without dynamic imports.
  console.warn(
    `getLucideIconData: Auto-extraction is not supported in React Native. ` +
      `Use createLucideIconData() with explicit icon data instead.`,
  );

  return [];
}
