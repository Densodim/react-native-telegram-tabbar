/**
 * @file events.ts
 * @description Типы событий нативного компонента панели навигации.
 *              Native tab bar event types.
 *
 * Эти события передаются из нативного Android-кода в JavaScript через
 * мост Expo Modules. Поле nativeEvent.key содержит имя нажатой вкладки
 * (совпадает с TabItem.key и именем роута React Navigation).
 *
 * These events are dispatched from native Android code to JavaScript via
 * the Expo Modules bridge. The nativeEvent.key field contains the name of
 * the pressed tab (matches TabItem.key and React Navigation route name).
 */

/**
 * Событие нажатия на вкладку.
 * Tab press event.
 */
export interface TabPressEvent {
  nativeEvent: {
    /**
     * Имя (ключ) нажатой вкладки.
     * Name (key) of the pressed tab.
     */
    key: string;
  };
}

/**
 * Событие долгого нажатия на вкладку.
 * Tab long press event.
 */
export interface TabLongPressEvent {
  nativeEvent: {
    /**
     * Имя (ключ) вкладки, на которой было долгое нажатие.
     * Name (key) of the long-pressed tab.
     */
    key: string;
  };
}
