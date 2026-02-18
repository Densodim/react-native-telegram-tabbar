# react-native-telegram-tabbar

Native Android bottom tab bar inspired by Telegram's new tab navigation. Built with **Expo Modules API** and **Kotlin**.

## Features

- ✨ **Native Kotlin ViewGroup** with manual Canvas drawing
- 🎯 **Sliding indicator** at the top (Telegram-style)
- 🎭 **Smooth animations** - bounce on press, Telegram-style interpolators
- 💫 **Material ripple** effect per tab
- 🔔 **Numeric & dot badges** with spring animations
- 📍 **SVG Icons** - Lucide icons built-in, or use custom SVG
- 👆 **Long press** event support
- 👁️ **Hide on scroll** with smooth slide animation
- 📳 **Haptic feedback** on tap and long press
- 🎨 **Theme-aware** - dynamic colors, dark/light mode support
- 📱 **Edge-to-edge** support (system bar insets)
- ⚡ **60fps stable** - zero allocations in onDraw
- 🔄 **Drop-in replacement** for React Navigation's tabBar
- 🏗️ **New Architecture** compatible (Fabric)

## Requirements

- Expo SDK 50+
- React Native 0.73+
- `@react-navigation/bottom-tabs` 7+
- `react-native-safe-area-context` 4+

## Installation

```bash
npm install react-native-telegram-tabbar
# or
yarn add react-native-telegram-tabbar
```

Then rebuild your native app:

```bash
npx expo run:android
```

---

## Quick Start

### 1. Basic Usage with Expo Router

```tsx
import { Tabs } from 'expo-router'
import { Platform } from 'react-native'
import { TelegramTabBar, LUCIDE_ICONS } from 'react-native-telegram-tabbar'
import { GlassTabBar } from '@/components/navigation/GlassTabBar' // Your iOS tab bar

// ✅ Use pre-built Lucide icons (no manual SVG typing!)
const TAB_ICONS = {
  index: LUCIDE_ICONS.home,
  search: LUCIDE_ICONS.search,
  messages: LUCIDE_ICONS.message,
  profile: LUCIDE_ICONS.user,
}

export default function TabLayout() {
  return (
    <Tabs
      tabBar={props =>
        Platform.OS === 'android' ? (
          <TelegramTabBar
            {...props}
            iconNodes={TAB_ICONS}
            theme={{
              backgroundColor: '#FFFFFF',
              activeColor: '#007AFF',
              inactiveColor: '#8E8E93',
              indicatorColor: '#007AFF',
            }}
          />
        ) : (
          <GlassTabBar {...props} />
        )
      }
    >
      <Tabs.Screen name="index" options={{ tabBarLabel: 'Home' }} />
      <Tabs.Screen name="search" options={{ tabBarLabel: 'Search' }} />
      <Tabs.Screen name="messages" options={{ tabBarLabel: 'Messages', tabBarBadge: 5 }} />
      <Tabs.Screen name="profile" options={{ tabBarLabel: 'Profile' }} />
    </Tabs>
  )
}
```

### 2. Real-World Example (with Theme Context)

```tsx
import { Tabs } from 'expo-router'
import { Platform } from 'react-native'
import { TelegramTabBar, LUCIDE_ICONS } from 'react-native-telegram-tabbar'
import { useAuth } from '@/hooks'
import { useTabBarScroll } from '@/hooks/useTabBarScroll'
import { useTheme } from '@/theme/ThemeContext'

const TAB_ICONS = {
  index: LUCIDE_ICONS.home,
  search: LUCIDE_ICONS.search,
  auth: LUCIDE_ICONS.login,
  announcement: LUCIDE_ICONS.plus,
  messages: LUCIDE_ICONS.message,
  profile: LUCIDE_ICONS.user,
}

export default function TabLayout() {
  const { isAuthenticated } = useAuth()
  const { theme } = useTheme()
  const { isTabBarVisible } = useTabBarScroll()

  const telegramTheme = {
    backgroundColor: theme.bgPrimaryWeaker,
    activeColor: theme.primaryProjectNormal,
    inactiveColor: theme.textPrimaryStrong,
    indicatorColor: theme.primaryProjectNormal,
  }

  return (
    <Tabs
      tabBar={props =>
        Platform.OS === 'android' ? (
          <TelegramTabBar
            {...props}
            theme={telegramTheme}
            iconNodes={TAB_ICONS}
            isVisible={isTabBarVisible}  // Hide on scroll
          />
        ) : (
          <GlassTabBar {...props} />
        )
      }
    >
      <Tabs.Screen name="index" options={{ tabBarLabel: 'Home' }} />
      <Tabs.Screen name="search" options={{ tabBarLabel: 'Search' }} />
      <Tabs.Screen
        name="auth"
        options={{
          tabBarLabel: 'Login',
          href: isAuthenticated ? null : undefined,  // Hide when authenticated
        }}
      />
      <Tabs.Screen
        name="announcement"
        options={{
          tabBarLabel: 'Post',
          tabBarBadge: '9+',  // Numeric badge
          href: isAuthenticated ? undefined : null,
        }}
      />
      <Tabs.Screen
        name="messages"
        options={{
          tabBarLabel: 'Messages',
          href: isAuthenticated ? undefined : null,
        }}
      />
      <Tabs.Screen
        name="profile"
        options={{
          tabBarLabel: 'Profile',
          href: isAuthenticated ? undefined : null,
        }}
      />
    </Tabs>
  )
}
```

---

## Icons

### ✅ Recommended: Use Built-in Lucide Icons

The library includes **all common Lucide icons**. No manual SVG path typing!

```tsx
import { LUCIDE_ICONS } from 'react-native-telegram-tabbar'

const TAB_ICONS = {
  home: LUCIDE_ICONS.home,
  search: LUCIDE_ICONS.search,
  profile: LUCIDE_ICONS.user,
  messages: LUCIDE_ICONS.message,
  settings: LUCIDE_ICONS.settings,
  notifications: LUCIDE_ICONS.bell,
  favorites: LUCIDE_ICONS.heart,
  calendar: LUCIDE_ICONS.calendar,
  camera: LUCIDE_ICONS.camera,
  cart: LUCIDE_ICONS.cart,
  // ... see iconHelpers.ts for full list
}

<TelegramTabBar iconNodes={TAB_ICONS} />
```

**Available icons:** `home`, `search`, `user`, `plus`, `message`, `settings`, `bell`, `heart`, `login`, `logout`, `star`, `map`, `calendar`, `camera`, `cart`, `filter`, `menu`, and more.

See [`src/iconHelpers.ts`](./src/iconHelpers.ts) for the complete list.

### Custom Icons

```tsx
import { createLucideIconData } from 'react-native-telegram-tabbar'

const TAB_ICONS = {
  // Built-in icon
  home: LUCIDE_ICONS.home,

  // Custom icon
  customArrow: createLucideIconData([
    ['path', { d: 'M5 12h14' }],
    ['path', { d: 'm12 5 7 7-7 7' }],
  ]),
}
```

### Custom SVG from String

```tsx
import { createCustomIconData } from 'react-native-telegram-tabbar'

const customIcon = createCustomIconData(`
  <svg viewBox="0 0 24 24">
    <path d="M10 10 L20 20"/>
    <circle cx="15" cy="15" r="5"/>
  </svg>
`)
```

---

## Theming

### Static Theme

```tsx
const theme = {
  backgroundColor: '#FFFFFF',
  activeColor: '#007AFF',
  inactiveColor: '#8E8E93',
  indicatorColor: '#007AFF',
}

<TelegramTabBar theme={theme} />
```

### Dark/Light Mode

```tsx
const { theme } = useTheme()

const telegramTheme = {
  backgroundColor: theme.bgPrimaryWeaker,      // Dynamic from context
  activeColor: theme.primaryProjectNormal,     // e.g. '#51cfc4'
  inactiveColor: theme.textPrimaryStrong,      // e.g. '#434355'
  indicatorColor: theme.primaryProjectNormal,
}

<TelegramTabBar theme={telegramTheme} />
```

**Telegram's color scheme (reference):**

| Mode | Background | Active | Inactive | Indicator |
|------|-----------|--------|----------|-----------|
| Light | `#FFFFFF` | `#2EA6FF` | `#8E8E93` | `#2EA6FF` |
| Dark | `#1C1C1E` | `#0A84FF` | `#AEAEB2` | `#0A84FF` |

---

## Badges

### Numeric Badges

```tsx
<Tabs.Screen
  name="messages"
  options={{
    tabBarLabel: 'Messages',
    tabBarBadge: 5,          // Shows "5"
  }}
/>

<Tabs.Screen
  name="notifications"
  options={{
    tabBarLabel: 'Alerts',
    tabBarBadge: '99+',      // Shows "99+"
  }}
/>
```

Numbers over 99 automatically display as `"99+"`.

### Dot Badges

Small red circle without a number:

```tsx
<Tabs.Screen
  name="updates"
  options={{
    tabBarLabel: 'Updates',
    tabBarBadge: 'dot',      // Shows dot badge
  }}
/>
```

**Dot badge triggers:** `tabBarBadge` set to `true`, `''` (empty string), or `'dot'`.

---

## Hide on Scroll

The tab bar can automatically hide when scrolling down and reappear when scrolling up.

### Step 1: Create a scroll hook

```tsx
// hooks/useTabBarScroll.ts
import { useCallback, useRef, useState } from 'react'
import type { NativeScrollEvent, NativeSyntheticEvent } from 'react-native'

const SCROLL_THRESHOLD = 10

export function useTabBarScroll() {
  const [isTabBarVisible, setIsTabBarVisible] = useState(true)
  const lastOffsetY = useRef(0)

  const onScroll = useCallback((event: NativeSyntheticEvent<NativeScrollEvent>) => {
    const currentY = event.nativeEvent.contentOffset.y
    const deltaY = currentY - lastOffsetY.current

    if (currentY <= 0) {
      setIsTabBarVisible(true)       // At top
    } else if (deltaY > SCROLL_THRESHOLD) {
      setIsTabBarVisible(false)      // Scrolling down - hide
    } else if (deltaY < -SCROLL_THRESHOLD) {
      setIsTabBarVisible(true)       // Scrolling up - show
    }

    lastOffsetY.current = currentY
  }, [])

  return { isTabBarVisible, onScroll }
}
```

### Step 2: Use in tab layout

```tsx
export default function TabLayout() {
  const { isTabBarVisible } = useTabBarScroll()

  return (
    <Tabs
      tabBar={props => (
        <TelegramTabBar {...props} isVisible={isTabBarVisible} />
      )}
    >
      {/* screens */}
    </Tabs>
  )
}
```

### Step 3: Pass onScroll to your lists

```tsx
function MyListScreen() {
  const { onScroll } = useTabBarScroll()

  return (
    <FlatList
      data={data}
      renderItem={renderItem}
      onScroll={onScroll}
      scrollEventThrottle={16}
    />
  )
}
```

Works with `FlatList`, `FlashList`, `ScrollView`, and any scrollable component.

---

## Long Press Events

React Navigation emits `tabLongPress` events which you can listen to:

```tsx
import { useNavigation } from '@react-navigation/native'

function MyScreen() {
  const navigation = useNavigation()

  useEffect(() => {
    const unsubscribe = navigation.addListener('tabLongPress', () => {
      // Show context menu, bottom sheet, etc.
      console.log('Long press on this tab!')
    })
    return unsubscribe
  }, [navigation])

  return <View />
}
```

---

## Props

### `TelegramTabBar` (Drop-in wrapper)

Extends `BottomTabBarProps` from React Navigation:

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `theme` | `TabBarTheme` | Blue/gray theme | Colors for the tab bar |
| `iconNodes` | `Record<string, IconNode>` | — | Map route name to icon data (use `LUCIDE_ICONS`) |
| `icons` | `Record<string, string>` | — | *(Deprecated)* Android drawable names |
| `isVisible` | `boolean` | `true` | Show/hide with slide animation |

### `NativeTelegramTabBarView` (Native view)

| Prop | Type | Required | Description |
|------|------|----------|-------------|
| `tabs` | `TabItem[]` | Yes | Array of tab items |
| `activeIndex` | `number` | Yes | Currently active tab index |
| `theme` | `TabBarTheme` | No | Theme colors |
| `badges` | `Record<string, number>` | No | Numeric badge counts by tab key |
| `dotBadges` | `string[]` | No | Tab keys that show a dot badge |
| `bottomInset` | `number` | No | Bottom safe area inset (dp, fallback) |
| `isVisible` | `boolean` | No | Animate show/hide via `translationY` |
| `onTabPress` | `(event: TabPressEvent) => void` | No | Tab press callback |
| `onTabLongPress` | `(event: TabLongPressEvent) => void` | No | Tab long press callback |

### Types

```ts
import type { IconNode, TabBarTheme, IconMap } from 'react-native-telegram-tabbar'

interface TabItem {
  key: string
  title: string
  icon?: string          // Android drawable (legacy)
  svgPaths?: SvgElement[]  // SVG icon data
}

interface TabBarTheme {
  backgroundColor: string  // Hex color, e.g. '#FFFFFF'
  activeColor: string
  inactiveColor: string
  indicatorColor: string
}

type IconNode = [string, Record<string, string>][]
type IconMap = Record<string, IconNode>
```

---

## Performance

Optimized for **60 FPS** on all devices:

- ✅ **Zero allocations** in `onDraw()` - all objects pre-allocated
- ✅ **Path caching** - SVG paths transformed once, cached
- ✅ **Hardware layers** - GPU acceleration for static content
- ✅ **PorterDuff filters** - GPU color blending
- ✅ **Partial invalidation** - only redraw changed areas

**Benchmarks** (Pixel 5, Android 12):

| Operation | Time |
|-----------|------|
| First icon render | ~2-3ms |
| Cached icon render | ~0.5ms |
| Color change | ~0.1ms |
| Tab switch animation | 250ms @ 60fps |

**Memory:**
- ~2KB per icon (cached paths)
- ~15KB total for 5 tabs

---

## Animations

| Animation | Duration | Interpolator | Trigger |
|-----------|----------|--------------|---------|
| **Indicator slide** | 250ms | Telegram easing | Tab switch |
| **Icon bounce** | 300ms (3-phase) | Spring overshoot | Tab press |
| **Badge in** | 250ms | Spring | Badge appears |
| **Badge out** | 150ms | Telegram easing | Badge removed |
| **Color transition** | 200ms | Telegram easing | Active state change |
| **Show/hide** | 250ms | Telegram easing | `isVisible` change |
| **Haptic tap** | Instant | — | Tab press |
| **Haptic long** | Instant | — | Tab long press |

**Telegram interpolator:** `PathInterpolator(0.4, 0, 0.2, 1)` - smooth, natural motion.

---

## Architecture

```
React Navigation / Expo Router (TypeScript)
    ↓
TelegramTabBar.tsx (Drop-in wrapper)
  • Filters visible routes
  • Converts iconNodes to native format
  • Builds tabs, badges, dotBadges
  • Handles tab press/long press events
    ↓
NativeTelegramTabBarView (Expo requireNativeView bridge)
    ↓
ExpoTelegramTabBarModule.kt (Expo Module definition)
  Props: tabs, activeIndex, badges, dotBadges, theme, isVisible
  Events: onTabPress, onTabLongPress
    ↓
TelegramTabBarView.kt (Custom FrameLayout)
  ├─ BlurBackgroundView (Layer 1)
  │    • Blur effect (Android 12+)
  │    • Semi-transparent background
  │    • Elevation shadow
  └─ ContentOverlayView (Layer 2)
       • SvgIconView (Canvas-rendered icons)
       • TextViews (labels)
       • Sliding indicator
       • Animated badges
```

---

## Platform Support

| Platform | Status |
|----------|--------|
| **Android** | ✅ Native Kotlin implementation |
| **iOS** | ⚪ Falls back to `null` (bring your own tab bar) |

The library is Android-only. For iOS, use a custom tab bar (e.g., GlassTabBar with Reanimated).

---

## Migration from Old API

### Before (❌ Manual SVG typing)

```tsx
const LUCIDE_ICONS: Record<string, IconNode> = {
  index: [
    ['path', { d: 'M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8' }],
    ['path', { d: 'M3 10a2 2 0 0 1 .709-1.528l7-6a2 2 0 0 1 2.582 0l7 6...' }],
  ],
  // ... 50+ lines of manual typing
}
```

### After (✅ Clean)

```tsx
import { LUCIDE_ICONS } from 'react-native-telegram-tabbar'

const TAB_ICONS = {
  index: LUCIDE_ICONS.home,
  search: LUCIDE_ICONS.search,
}
```

---

## Troubleshooting

### Icons not showing

**Check:**
1. `iconNodes` prop is passed to `TelegramTabBar`
2. Route names match keys in `TAB_ICONS` object
3. Rebuild after changes: `npx expo run:android`

**Debug logs:**
```bash
npx react-native log-android | grep "TelegramTabBar"
```

Should see:
```
TelegramTabBar: Tab[index] → SvgIconView with 2 elements, color=#FF51CFC4
```

### Icons are blurry

Make sure you're using the **latest version** with the stroke width fix applied.

### Tab bar not hiding on scroll

Check that `isVisible` prop changes when scrolling:
```tsx
console.log('isTabBarVisible:', isTabBarVisible)  // Should toggle
```

---

## Contributing

Contributions welcome!

## License

MIT

---

## Credits

Built with ❤️ using:
- [Expo Modules API](https://docs.expo.dev/modules/)
- [Lucide Icons](https://lucide.dev/)
- Inspired by [Telegram for Android](https://github.com/DrKLO/Telegram)