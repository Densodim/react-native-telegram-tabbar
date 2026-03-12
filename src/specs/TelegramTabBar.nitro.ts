import { type HybridView } from 'react-native-nitro-modules'

// ─────────────────────────────────────────────────────────────────────────────
// SVG element — one primitive shape from a Lucide icon path set
// ─────────────────────────────────────────────────────────────────────────────

export interface SvgElement {
  /** 'path' | 'circle' | 'line' | 'polyline' | 'polygon' | 'rect' */
  type: string
  // path
  d?: string
  // circle
  cx?: string
  cy?: string
  r?: string
  // line
  x1?: string
  y1?: string
  x2?: string
  y2?: string
  // polyline / polygon
  points?: string
  // rect
  x?: string
  y?: string
  width?: string
  height?: string
  rx?: string
  ry?: string
}

// ─────────────────────────────────────────────────────────────────────────────
// Tab item — one entry in the tab bar
// ─────────────────────────────────────────────────────────────────────────────

export interface TabItem {
  key: string
  title: string
  svgPaths: SvgElement[]
}

// ─────────────────────────────────────────────────────────────────────────────
// Theme — colors for the tab bar
// ─────────────────────────────────────────────────────────────────────────────

export interface TabBarTheme {
  backgroundColor: string
  activeColor: string
  inactiveColor: string
  indicatorColor: string
}

// ─────────────────────────────────────────────────────────────────────────────
// Badge — numeric or dot indicator on a tab
// ─────────────────────────────────────────────────────────────────────────────

export interface TabBadge {
  key: string
  /** Badge number to show. Ignored when isDot is true. */
  count: number
  isDot: boolean
}

// ─────────────────────────────────────────────────────────────────────────────
// HybridView — the native floating tab bar view
// ─────────────────────────────────────────────────────────────────────────────

export interface TelegramTabBarSpec
  extends HybridView<{ ios: 'swift'; android: 'kotlin' }> {
  // Props
  tabs: TabItem[]
  activeIndex: number
  theme: TabBarTheme
  bottomInset: number
  isVisible: boolean
  tabBadges: TabBadge[]

  // Event callbacks
  onTabPress?: (key: string) => void
  onTabLongPress?: (key: string) => void
}
