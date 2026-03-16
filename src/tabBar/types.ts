/**
 * @file tabBar/types.ts
 */

import type { BottomTabBarProps } from '@react-navigation/bottom-tabs';
import type { TabBarTheme } from '../types/tab';

export interface TelegramTabBarCustomProps {
  /** Tab bar color theme. */
  theme?: TabBarTheme;

  /**
   * Controls tab bar visibility (for hide-on-scroll animation).
   * Defaults to true.
   */
  isVisible?: boolean;
}

export type TelegramTabBarProps = BottomTabBarProps & TelegramTabBarCustomProps;
