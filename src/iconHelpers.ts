/**
 * @file iconHelpers.ts
 * @description Обратно совместимый реэкспорт иконочных утилит.
 *              Backward-compatible re-export of icon utilities.
 *
 * Этот файл сохранён для обратной совместимости с импортами вида:
 *   import { LUCIDE_ICONS } from 'react-native-telegram-tabbar/iconHelpers';
 *
 * Все реальные реализации теперь находятся в src/icons/.
 *
 * This file is kept for backward compatibility with imports like:
 *   import { LUCIDE_ICONS } from 'react-native-telegram-tabbar/iconHelpers';
 *
 * All actual implementations now live in src/icons/.
 */

export {
  LUCIDE_ICONS,
  createLucideIconData,
  getLucideIconData,
  createCustomIconData,
  type IconMap,
} from './icons/index';
