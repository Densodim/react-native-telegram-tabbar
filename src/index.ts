/**
 * @file index.ts
 * @description Публичный API библиотеки react-native-telegram-tabbar.
 *              Public API of the react-native-telegram-tabbar library.
 *
 * Структура библиотеки / Library structure:
 *
 *   src/
 *   ├── index.ts                  ← этот файл / this file (public API)
 *   ├── ExpoTelegramTabBarView.ts ← нативный мост / native bridge
 *   ├── TelegramTabBar.tsx        ← главный компонент / main component
 *   ├── iconHelpers.ts            ← обратная совместимость / backward compat
 *   │
 *   ├── types/                    ← базовые типы данных / base data types
 *   │   ├── svg.ts                ← SvgElement
 *   │   ├── tab.ts                ← TabItem, TabBarTheme
 *   │   ├── events.ts             ← TabPressEvent, TabLongPressEvent
 *   │   ├── nativeProps.ts        ← NativeTelegramTabBarProps
 *   │   └── index.ts
 *   │
 *   ├── tabBar/                   ← логика компонента / component logic
 *   │   ├── constants.ts          ← размеры и тема по умолчанию / sizes & default theme
 *   │   ├── types.ts              ← IconNode, TelegramTabBarProps
 *   │   ├── iconNodesToSvg.ts     ← конвертер Lucide → SvgElement
 *   │   ├── useVisibleRoutes.ts   ← фильтрация роутов / route filtering
 *   │   ├── useTabs.ts            ← построение TabItem[] / TabItem[] builder
 *   │   ├── useBadges.ts          ← разбор бейджей / badge parsing
 *   │   └── useTabHandlers.ts     ← обработчики событий / event handlers
 *   │
 *   └── icons/                   ← работа с иконками / icon utilities
 *       ├── iconTypes.ts          ← IconMap
 *       ├── lucideHelpers.ts      ← createLucideIconData, getLucideIconData
 *       ├── lucideIcons.ts        ← LUCIDE_ICONS (17 предустановленных иконок)
 *       ├── svgParser.ts          ← createCustomIconData
 *       └── index.ts
 */

// =============================================================================
// Нативный мост / Native bridge
// =============================================================================

export {
  NativeTelegramTabBarView,
  // Типы нативного слоя / Native layer types
  type SvgElement,
  type TabItem,
  type TabBarTheme,
  type TabPressEvent,
  type TabLongPressEvent,
  type NativeTelegramTabBarProps,
} from './ExpoTelegramTabBarView';

// =============================================================================
// Главный компонент / Main component
// =============================================================================

export {
  TelegramTabBar,
  iconNodesToSvg,
  // Типы компонента / Component types
  type IconNode,
  type TelegramTabBarCustomProps,
  type TelegramTabBarProps,
} from './TelegramTabBar';

// =============================================================================
// Утилиты для иконок / Icon utilities
// =============================================================================

export {
  /** Набор из 17 предустановленных иконок Lucide. / Set of 17 pre-defined Lucide icons. */
  LUCIDE_ICONS,
  /** Типобезопасная обёртка для данных иконки Lucide. / Type-safe wrapper for Lucide icon data. */
  createLucideIconData,
  /** @deprecated Используйте createLucideIconData. / Use createLucideIconData. */
  getLucideIconData,
  /** Парсер SVG-строк в формат IconNode. / SVG string parser to IconNode format. */
  createCustomIconData,
  /** Тип для словаря иконок. / Type for icon dictionary. */
  type IconMap,
} from './icons/index';
