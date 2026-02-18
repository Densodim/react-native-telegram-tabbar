/**
 * @file icons/lucideIcons.ts
 * @description Предопределённые иконки в формате Lucide для общих случаев.
 *              Pre-defined Lucide-format icons for common use cases.
 *
 * Все иконки взяты из lucide-react-native и сохранены как статические данные,
 * чтобы избежать зависимости от пакета lucide-react-native и
 * динамического импорта.
 *
 * All icons are taken from lucide-react-native and stored as static data to
 * avoid a dependency on the lucide-react-native package and dynamic imports.
 *
 * Использование / Usage:
 * ```ts
 * import { LUCIDE_ICONS } from 'react-native-telegram-tabbar';
 *
 * <TelegramTabBar iconNodes={LUCIDE_ICONS} />
 * // или выборочно / or selectively:
 * <TelegramTabBar iconNodes={{ home: LUCIDE_ICONS.home, search: LUCIDE_ICONS.search }} />
 * ```
 */

import type { IconNode } from '../tabBar/types';

/**
 * Набор готовых иконок Lucide для использования с TelegramTabBar.
 * A set of ready-to-use Lucide icons for use with TelegramTabBar.
 *
 * Каждый ключ соответствует рекомендуемому имени роута.
 * Each key corresponds to a recommended route name.
 *
 * Доступные иконки / Available icons:
 * home, search, user, plus, message, settings, bell, heart,
 * login, logout, star, map, calendar, camera, cart, filter, menu
 */
export const LUCIDE_ICONS = {
  // --- Навигация / Navigation ---

  /** Иконка дома (House). House icon. */
  home: [
    ['path', { d: 'M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8' }],
    [
      'path',
      {
        d: 'M3 10a2 2 0 0 1 .709-1.528l7-6a2 2 0 0 1 2.582 0l7 6A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z',
      },
    ],
  ],

  /** Иконка поиска (Search). Search icon. */
  search: [
    ['circle', { cx: '11', cy: '11', r: '8' }],
    ['path', { d: 'm21 21-4.35-4.35' }],
  ],

  /** Иконка профиля пользователя (User). User profile icon. */
  user: [
    ['path', { d: 'M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2' }],
    ['circle', { cx: '12', cy: '7', r: '4' }],
  ],

  /** Иконка добавления (CirclePlus). Add/create icon (CirclePlus). */
  plus: [
    ['circle', { cx: '12', cy: '12', r: '10' }],
    ['path', { d: 'M8 12h8' }],
    ['path', { d: 'M12 8v8' }],
  ],

  // --- Коммуникации / Communication ---

  /** Иконка сообщений (MessageCircle). Messages icon (MessageCircle). */
  message: [['path', { d: 'M7.9 20A9 9 0 1 0 4 16.1L2 22Z' }]],

  /** Иконка уведомлений (Bell). Notifications icon (Bell). */
  bell: [
    ['path', { d: 'M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9' }],
    ['path', { d: 'M10.3 21a1.94 1.94 0 0 0 3.4 0' }],
  ],

  // --- Настройки и состояние / Settings and state ---

  /** Иконка настроек (Settings). Settings icon. */
  settings: [
    [
      'path',
      {
        d: 'M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z',
      },
    ],
    ['circle', { cx: '12', cy: '12', r: '3' }],
  ],

  /** Иконка избранного (Heart). Favorites icon (Heart). */
  heart: [
    [
      'path',
      {
        d: 'M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z',
      },
    ],
  ],

  // --- Аутентификация / Authentication ---

  /** Иконка входа (LogIn). Login icon. */
  login: [
    ['path', { d: 'M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4' }],
    ['polyline', { points: '10 17 15 12 10 7' }],
    ['line', { x1: '15', x2: '3', y1: '12', y2: '12' }],
  ],

  /** Иконка выхода (LogOut). Logout icon. */
  logout: [
    ['path', { d: 'M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4' }],
    ['polyline', { points: '16 17 21 12 16 7' }],
    ['line', { x1: '21', x2: '9', y1: '12', y2: '12' }],
  ],

  // --- Прочее / Miscellaneous ---

  /** Иконка звезды (Star). Star icon. */
  star: [
    [
      'polygon',
      {
        points:
          '12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2',
      },
    ],
  ],

  /** Иконка местоположения (MapPin). Location icon (MapPin). */
  map: [
    ['path', { d: 'M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z' }],
    ['circle', { cx: '12', cy: '10', r: '3' }],
  ],

  /** Иконка календаря (Calendar). Calendar icon. */
  calendar: [
    ['rect', { width: '18', height: '18', x: '3', y: '4', rx: '2', ry: '2' }],
    ['line', { x1: '16', x2: '16', y1: '2', y2: '6' }],
    ['line', { x1: '8', x2: '8', y1: '2', y2: '6' }],
    ['line', { x1: '3', x2: '21', y1: '10', y2: '10' }],
  ],

  /** Иконка камеры (Camera). Camera icon. */
  camera: [
    [
      'path',
      {
        d: 'M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z',
      },
    ],
    ['circle', { cx: '12', cy: '13', r: '3' }],
  ],

  /** Иконка корзины (ShoppingCart). Shopping cart icon. */
  cart: [
    ['circle', { cx: '8', cy: '21', r: '1' }],
    ['circle', { cx: '19', cy: '21', r: '1' }],
    [
      'path',
      {
        d: 'M2.05 2.05h2l2.66 12.42a2 2 0 0 0 2 1.58h9.78a2 2 0 0 0 1.95-1.57l1.65-7.43H5.12',
      },
    ],
  ],

  /** Иконка фильтра (Filter). Filter icon. */
  filter: [
    ['polygon', { points: '22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3' }],
  ],

  /** Иконка меню (Menu / Hamburger). Menu (hamburger) icon. */
  menu: [
    ['line', { x1: '4', x2: '20', y1: '12', y2: '12' }],
    ['line', { x1: '4', x2: '20', y1: '6', y2: '6' }],
    ['line', { x1: '4', x2: '20', y1: '18', y2: '18' }],
  ],
} as const satisfies Record<string, IconNode>;
