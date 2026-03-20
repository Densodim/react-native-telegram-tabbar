# react-native-telegram-tabbar

Native floating tab bar with frosted glass blur — Telegram-style, for iOS and Android.

- **iOS** — UIKit pill with `UIBlurEffect`, CoreGraphics SVG rendering, SF Symbols
- **Android** — Jetpack Compose pill with `BlurViewGroup`, Lucide drawable icons

Built on [Expo Modules API](https://docs.expo.dev/modules/). New Architecture (Fabric) compatible.

---

## Features

- Floating pill with frosted glass blur (iOS: `systemUltraThinMaterialDark`, Android: 25f radius)
- Drop shadow under the pill
- Active tab — white card background, colored icon and label
- Inactive tabs — muted icon and label color
- Bounce animation on tap (scale 1.2 → 0.93 → 1.0)
- Smooth show/hide via `isVisible` prop (slide off-screen)
- Numeric badges and dot badges
- Long press events
- Haptic feedback on tap and long press (Android)
- SF Symbols on iOS (mapped from Lucide icon names)
- Built-in Lucide icon set (17 icons)
- Custom SVG icons support
- 60 FPS, zero allocations on draw path

---

## Requirements

- React Native 0.73+
- Expo Modules Core 1.0+
- `@react-navigation/bottom-tabs` 7+
- `react-native-safe-area-context` 4+

---

## Installation

```bash
npm install react-native-telegram-tabbar
# or
yarn add react-native-telegram-tabbar
```

Rebuild native:

```bash
npx expo run:ios
npx expo run:android
```

---

## Quick Start

```tsx
// app/(tabs)/_layout.tsx
import { Tabs } from 'expo-router'
import { TelegramTabBar } from 'react-native-telegram-tabbar'

export default function TabLayout() {
  return (
    <Tabs
      tabBar={props => <TelegramTabBar {...props} />}
    >
      <Tabs.Screen
        name="index"
        options={{
          tabBarLabel: 'Home',
          tabBarIconName: 'home',
        }}
      />
      <Tabs.Screen
        name="search"
        options={{
          tabBarLabel: 'Search',
          tabBarIconName: 'search',
        }}
      />
      <Tabs.Screen
        name="messages"
        options={{
          tabBarLabel: 'Messages',
          tabBarIconName: 'message',
          tabBarBadge: 3,
        }}
      />
      <Tabs.Screen
        name="profile"
        options={{
          tabBarLabel: 'Profile',
          tabBarIconName: 'user',
        }}
      />
    </Tabs>
  )
}
```

---

## Icons

Icons are set via the `tabBarIconName` screen option. The value is a Lucide icon name.

### Built-in icons

| Name | Icon |
|------|------|
| `home` | House |
| `search` | Search |
| `user` | User |
| `plus` | CirclePlus |
| `message` | MessageCircle |
| `bell` | Bell |
| `settings` | Settings |
| `heart` | Heart |
| `login` | LogIn |
| `logout` | LogOut |
| `star` | Star |
| `map` | MapPin |
| `calendar` | Calendar |
| `camera` | Camera |
| `cart` | ShoppingCart |
| `filter` | Filter |
| `menu` | Menu |

### iOS — SF Symbols

On iOS, Lucide icon names are mapped to native SF Symbols where possible (e.g. `home` → `house`, `message` → `bubble.left.fill`). When no mapping exists, the SVG path is rendered via CoreGraphics.

### Custom SVG icons

Pass raw SVG path data via `tabBarIconName` if the built-in set is insufficient, or use the `LUCIDE_ICONS` / `createLucideIconData` utilities to build icon data and pass it to the native layer directly if needed for lower-level use.

```tsx
import { createLucideIconData } from 'react-native-telegram-tabbar'

const arrowIcon = createLucideIconData([
  ['path', { d: 'M5 12h14' }],
  ['path', { d: 'm12 5 7 7-7 7' }],
])
```

---

## Props

### `TelegramTabBar`

Extends `BottomTabBarProps` from React Navigation.

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `theme` | `TabBarTheme` | dark theme | Pill colors |
| `isVisible` | `boolean` | `true` | Show/hide with slide animation |

### `NativeTelegramTabBarView` (low-level)

| Prop | Type | Required | Description |
|------|------|----------|-------------|
| `tabs` | `TabItem[]` | Yes | Array of tab data |
| `activeIndex` | `number` | Yes | Currently active tab index |
| `theme` | `TabBarTheme` | No | Theme colors |
| `tabBadges` | `TabBadgeItem[]` | No | Badge items |
| `bottomInset` | `number` | No | Safe area bottom inset in dp/pt |
| `isVisible` | `boolean` | No | Show/hide animation |
| `onTabPress` | `(e: TabPressEvent) => void` | No | Tab press callback |
| `onTabLongPress` | `(e: TabLongPressEvent) => void` | No | Long press callback |

### Screen options

| Option | Type | Description |
|--------|------|-------------|
| `tabBarLabel` | `string` | Tab label text |
| `tabBarIconName` | `string` | Lucide icon name (see built-in list above) |
| `tabBarBadge` | `number \| string` | Badge value — number, `'99+'`, or `'dot'` for dot badge |

### `TabBarTheme`

```ts
interface TabBarTheme {
  backgroundColor: string  // Pill background color
  activeColor: string      // Active tab icon and label color
  inactiveColor: string    // Inactive tab icon and label color
  indicatorColor: string   // Reserved (used on Android indicator)
}
```

Default theme:

```ts
{
  backgroundColor: '#000000',
  activeColor:     '#111111',
  inactiveColor:   '#A9ABB1',
  indicatorColor:  '#111111',
}
```

---

## Theming

```tsx
<TelegramTabBar
  {...props}
  theme={{
    backgroundColor: '#FFFFFF',
    activeColor: '#007AFF',
    inactiveColor: '#8E8E93',
    indicatorColor: '#007AFF',
  }}
/>
```

Dynamic (dark/light mode):

```tsx
const { colors } = useTheme()

<TelegramTabBar
  {...props}
  theme={{
    backgroundColor: colors.card,
    activeColor: colors.primary,
    inactiveColor: colors.border,
    indicatorColor: colors.primary,
  }}
/>
```

---

## Badges

Badges are driven by the standard `tabBarBadge` screen option.

```tsx
// Numeric badge
<Tabs.Screen options={{ tabBarBadge: 5 }} />

// Capped at 99+
<Tabs.Screen options={{ tabBarBadge: 120 }} />  // shows "99+"

// Dot badge (no number)
<Tabs.Screen options={{ tabBarBadge: 'dot' }} />
```

---

## Hide on Scroll

Pass `isVisible` to show/hide the tab bar when the user scrolls.

```tsx
// hooks/useTabBarScroll.ts
import { useCallback, useRef, useState } from 'react'
import type { NativeScrollEvent, NativeSyntheticEvent } from 'react-native'

const THRESHOLD = 10

export function useTabBarScroll() {
  const [isVisible, setIsVisible] = useState(true)
  const lastY = useRef(0)

  const onScroll = useCallback((e: NativeSyntheticEvent<NativeScrollEvent>) => {
    const y = e.nativeEvent.contentOffset.y
    const delta = y - lastY.current
    if (y <= 0) setIsVisible(true)
    else if (delta > THRESHOLD) setIsVisible(false)
    else if (delta < -THRESHOLD) setIsVisible(true)
    lastY.current = y
  }, [])

  return { isVisible, onScroll }
}
```

```tsx
// _layout.tsx
const { isVisible, onScroll } = useTabBarScroll()

<Tabs tabBar={props => <TelegramTabBar {...props} isVisible={isVisible} />}>
  ...
</Tabs>
```

```tsx
// Screen with a list
<FlatList onScroll={onScroll} scrollEventThrottle={16} ... />
```

Works with `FlatList`, `FlashList`, `ScrollView`.

---

## Long Press

React Navigation emits `tabLongPress` events — subscribe from any screen:

```tsx
import { useNavigation } from '@react-navigation/native'

useEffect(() => {
  const unsub = navigation.addListener('tabLongPress', () => {
    // open bottom sheet, etc.
  })
  return unsub
}, [navigation])
```

---

## Architecture

```
Expo Router / React Navigation (TypeScript)
    ↓
TelegramTabBar.tsx
  Reads tabBarIconName, tabBarLabel, tabBarBadge from screen options
  Builds tabs[], tabBadges[], resolves activeIndex
    ↓
NativeTelegramTabBarView  (Expo requireNativeView)
    ↓
    ├── Android: TelegramTabBarView.kt
    │     BlurViewGroup (blur pill background)
    │     + Jetpack Compose overlay
    │       ├── Active tab: white rounded card + colored icon + label
    │       ├── Inactive tab: muted icon + label
    │       ├── Badges: red pill (numeric) or red dot
    │       └── Haptic feedback on tap / long press
    │
    └── iOS: TelegramTabBarView.swift
          UIVisualEffectView (.systemUltraThinMaterialDark)
          + UIView overlay
            ├── TabButtonView per tab
            │     SF Symbol (preferred) or CoreGraphics SVG
            │     Active: white rounded card background
            ├── Bounce animation on tap (keyframe)
            └── Slide animation on isVisible change
```

---

## Platform Support

| Platform | Status |
|----------|--------|
| Android | Native Kotlin + Jetpack Compose |
| iOS | Native Swift + UIKit |

---

## License

MIT
