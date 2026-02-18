/**
 * @file tabBar/useTabHandlers.ts
 * @description Хуки-обработчики событий нажатия на вкладки.
 *              Hooks for tab press and long press event handlers.
 *
 * Обработчики соединяют нативные события TelegramTabBarView с системой
 * навигации React Navigation:
 *   - handleTabPress: эмитирует событие tabPress, выполняет навигацию
 *     если вкладка не активна и событие не было перехвачено.
 *   - handleTabLongPress: эмитирует событие tabLongPress.
 *
 * Handlers connect native TelegramTabBarView events with React Navigation:
 *   - handleTabPress: emits a tabPress event, navigates if the tab is not
 *     focused and the event was not prevented.
 *   - handleTabLongPress: emits a tabLongPress event.
 */

import { useCallback } from 'react';
import type { NavigationHelpers, ParamListBase, Route } from '@react-navigation/native';
import type { BottomTabNavigationEventMap } from '@react-navigation/bottom-tabs';
import type { TabLongPressEvent, TabPressEvent } from '../types/events';

/**
 * Создаёт обработчики событий нажатия/долгого нажатия на вкладку.
 * Creates press and long press event handlers for tab items.
 *
 * @param visibleRoutes   - Список видимых роутов.
 *                          List of visible routes.
 * @param activeRouteKey  - Ключ текущего активного роута.
 *                          Key of the currently active route.
 * @param navigation      - Объект навигации от React Navigation.
 *                          React Navigation navigation object.
 */
export function useTabHandlers(
  visibleRoutes: Route<string>[],
  activeRouteKey: string,
  navigation: NavigationHelpers<ParamListBase, BottomTabNavigationEventMap>,
) {
  /**
   * Обрабатывает нажатие на вкладку.
   * Handles a tab press.
   *
   * Логика:
   * 1. Находит роут по имени из нативного события.
   * 2. Эмитирует событие 'tabPress' в систему навигации.
   * 3. Выполняет навигацию, если вкладка не активна и событие не перехвачено.
   *
   * Logic:
   * 1. Finds the route by name from the native event.
   * 2. Emits a 'tabPress' event into the navigation system.
   * 3. Navigates if the tab is not focused and the event was not prevented.
   */
  const handleTabPress = useCallback(
    (event: TabPressEvent) => {
      const key = event.nativeEvent.key;
      const route = visibleRoutes.find(r => r.name === key);
      if (!route) return;

      const isFocused = route.key === activeRouteKey;

      const navEvent = navigation.emit({
        type: 'tabPress',
        target: route.key,
        canPreventDefault: true,
      });

      // Переходим только если вкладка не активна и событие не остановлено.
      // Navigate only if the tab is not active and the event was not prevented.
      if (!isFocused && !navEvent.defaultPrevented) {
        navigation.navigate(route.name, route.params);
      }
    },
    [visibleRoutes, activeRouteKey, navigation],
  );

  /**
   * Обрабатывает долгое нажатие на вкладку.
   * Handles a tab long press.
   *
   * Эмитирует событие 'tabLongPress', которое может быть перехвачено
   * пользовательским обработчиком через listeners в React Navigation.
   *
   * Emits a 'tabLongPress' event, which can be intercepted by a custom
   * listener via React Navigation's listeners prop.
   */
  const handleTabLongPress = useCallback(
    (event: TabLongPressEvent) => {
      const key = event.nativeEvent.key;
      const route = visibleRoutes.find(r => r.name === key);
      if (!route) return;

      navigation.emit({
        type: 'tabLongPress',
        target: route.key,
      });
    },
    [visibleRoutes, navigation],
  );

  return { handleTabPress, handleTabLongPress };
}
