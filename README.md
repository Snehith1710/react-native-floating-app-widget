# react-native-floating-app-widget

A customizable, Play Store-safe React Native library for creating system-level floating widgets (chat-head style) on Android. The widget appears when your app is backgrounded or killed and hides when the app is in foreground.

## Features

- **System-level overlay widget** using `SYSTEM_ALERT_WINDOW`
- **Foreground service** with persistent notification (Play Store compliant)
- **Automatic visibility management** - shows in background, hides in foreground
- **Fully customizable** - icon, size, shape, position, colors
- **Draggable widget** with click-to-open functionality
- **Survives app kill** and optionally starts on device boot
- **New Architecture compatible** (TurboModules)
- **Android only** (explicitly no iOS)

## Installation

```bash
npm install react-native-floating-app-widget
# or
yarn add react-native-floating-app-widget
```

### Auto-linking (React Native 0.60+)

The library will be automatically linked. Just rebuild your app:

```bash
npx react-native run-android
```

### Android Manifest Setup

The library includes all required permissions and components in its manifest, which will be automatically merged. However, you should understand what's being added:

**Permissions:**
- `SYSTEM_ALERT_WINDOW` - Required for overlay
- `FOREGROUND_SERVICE` - Required for foreground service
- `FOREGROUND_SERVICE_SPECIAL_USE` - Required for Android 14+
- `POST_NOTIFICATIONS` - Required for Android 13+
- `RECEIVE_BOOT_COMPLETED` - Optional, for auto-start on boot

**Components:**
- `FloatingWidgetService` - Foreground service
- `AppStateReceiver` - Detects app state changes
- `BootReceiver` - Handles device boot (optional)

### Proguard Rules (if using Proguard)

Add to your `android/app/proguard-rules.pro`:

```proguard
-keep class com.floatingappwidget.** { *; }
```

## Usage

### Basic Example

```typescript
import FloatingAppWidget from 'react-native-floating-app-widget';
import { useEffect } from 'react';

function App() {
  useEffect(() => {
    initializeWidget();
  }, []);

  async function initializeWidget() {
    try {
      // 1. Initialize the widget
      await FloatingAppWidget.init({
        notification: {
          title: 'My App',
          text: 'Tap to return to app',
          channelId: 'floating_widget',
          channelName: 'Floating Widget',
        },
      });

      // 2. Check permission
      const hasPermission = await FloatingAppWidget.hasPermission();

      if (!hasPermission) {
        // 3. Request permission
        await FloatingAppWidget.requestPermission();
        // User will be taken to settings - check again when they return
        // You might want to use AppState to detect when user returns
        return;
      }

      // 4. Start the widget
      await FloatingAppWidget.start();
    } catch (error) {
      console.error('Failed to initialize widget:', error);
    }
  }

  return (
    // Your app UI
  );
}
```

### Advanced Example with Full Customization

```typescript
import FloatingAppWidget from 'react-native-floating-app-widget';
import { Image } from 'react-native';

async function setupCustomWidget() {
  // Convert icon to base64
  const iconBase64 = await convertImageToBase64('path/to/icon.png');

  await FloatingAppWidget.init({
    // Widget icon (base64 encoded image)
    icon: iconBase64,

    // Widget size in dp
    size: 64,

    // Widget shape ('circle' or 'rounded')
    shape: 'circle',

    // Enable dragging
    draggable: true,

    // Initial position (x, y in pixels)
    initialPosition: {
      x: 100,
      y: 200,
    },

    // Foreground service notification
    notification: {
      title: 'My App is Running',
      text: 'Tap the widget to return',
      channelId: 'my_widget_channel',
      channelName: 'Widget Notifications',
      icon: 'ic_notification', // drawable resource name
    },

    // Auto-start on device boot
    autoStartOnBoot: true,

    // Hide when app is opened
    hideOnAppOpen: true,
  });

  await FloatingAppWidget.start();
}

// Helper function to convert image to base64
async function convertImageToBase64(imagePath: string): Promise<string> {
  // Implementation depends on your image source
  // You can use react-native-fs or similar library
  return 'data:image/png;base64,...';
}
```

### Handling Permission Flow

```typescript
import { AppState } from 'react-native';
import FloatingAppWidget from 'react-native-floating-app-widget';

async function requestAndStart() {
  // Check current permission status
  const hasPermission = await FloatingAppWidget.hasPermission();

  if (!hasPermission) {
    // Request permission (opens system settings)
    await FloatingAppWidget.requestPermission();

    // Listen for app state changes to detect when user returns
    const subscription = AppState.addEventListener('change', async (nextAppState) => {
      if (nextAppState === 'active') {
        // User returned from settings, check permission again
        const nowHasPermission = await FloatingAppWidget.hasPermission();

        if (nowHasPermission) {
          await FloatingAppWidget.start();
          subscription.remove();
        }
      }
    });
  } else {
    // Permission already granted
    await FloatingAppWidget.start();
  }
}
```

### Updating Widget Configuration

```typescript
// Update widget while it's running
await FloatingAppWidget.update({
  size: 72,
  shape: 'rounded',
  notification: {
    title: 'Updated Title',
    text: 'Updated text',
    channelId: 'floating_widget',
    channelName: 'Floating Widget',
  },
});
```

### Stopping the Widget

```typescript
// Stop and remove the widget
await FloatingAppWidget.stop();
```

## API Reference

### Methods

#### `init(config: WidgetConfig): Promise<void>`

Initialize the widget with configuration. Must be called before `start()`.

**Parameters:**
- `config`: Widget configuration object (see WidgetConfig below)

**Throws:**
- Error if platform is not Android
- Error if configuration is invalid

---

#### `start(): Promise<void>`

Start the floating widget service. The widget will appear when the app goes to background.

**Throws:**
- Error if not initialized (call `init()` first)
- Error if permission is not granted

---

#### `stop(): Promise<void>`

Stop the widget service and remove the widget from screen.

---

#### `update(config: WidgetConfig): Promise<void>`

Update the widget configuration. Widget must be initialized first.

**Parameters:**
- `config`: New widget configuration

---

#### `hasPermission(): Promise<boolean>`

Check if `SYSTEM_ALERT_WINDOW` permission is granted.

**Returns:**
- Promise resolving to `true` if permission is granted

---

#### `requestPermission(): Promise<void>`

Request `SYSTEM_ALERT_WINDOW` permission. Opens the system settings screen where user can grant the permission.

**Note:** You should check permission status after the user returns from settings using `hasPermission()`.

---

#### `getPermissionStatus(): Promise<PermissionStatus>`

Get detailed permission status.

**Returns:**
- Promise resolving to `{ granted: boolean }`

---

#### `getConfig(): WidgetConfig | null`

Get the current widget configuration.

**Returns:**
- Current configuration or `null` if not initialized

---

#### `isInitialized(): boolean`

Check if the widget is initialized.

**Returns:**
- `true` if initialized

---

### Types

#### `WidgetConfig`

```typescript
interface WidgetConfig {
  icon?: string;                    // Base64 encoded image
  size?: number;                    // Widget size in dp (default: 56)
  shape?: 'circle' | 'rounded';    // Widget shape (default: 'circle')
  draggable?: boolean;              // Enable dragging (default: true)
  initialPosition?: {               // Initial position in pixels
    x: number;
    y: number;
  };
  notification: NotificationConfig; // Required
  autoStartOnBoot?: boolean;        // Auto-start on boot (default: false)
  hideOnAppOpen?: boolean;          // Hide when app opens (default: true)
}
```

#### `NotificationConfig`

```typescript
interface NotificationConfig {
  title: string;              // Required
  text: string;               // Required
  channelId?: string;         // Default: 'floating_widget_channel'
  channelName?: string;       // Default: 'Floating Widget'
  icon?: string;              // Drawable resource name (optional)
}
```

#### `PermissionStatus`

```typescript
interface PermissionStatus {
  granted: boolean;
}
```

## Play Store Compliance

This library is designed to be Play Store compliant:

1. **Explicit user opt-in**: Permission must be manually granted by the user
2. **Foreground service with notification**: Required for Android 8.0+
3. **No ads or monetization**: The widget is purely functional
4. **Clear user benefit**: Provides quick access to your app
5. **User control**: Widget can be stopped at any time

### Best Practices

1. **Explain the permission**: Before requesting `SYSTEM_ALERT_WINDOW`, explain to users why you need it
2. **Make it optional**: Don't force users to enable the widget
3. **Provide controls**: Add UI in your app to start/stop the widget
4. **Respect user choice**: If permission is denied, don't repeatedly ask

## Troubleshooting

### Widget doesn't appear

1. Check if permission is granted: `await FloatingAppWidget.hasPermission()`
2. Ensure `init()` was called before `start()`
3. Check if app is in background (widget hides when app is foreground by default)
4. Check Android logs for errors: `adb logcat *:E`

### Permission request doesn't work

- On Android 11+, permission requests go to system settings, not a dialog
- User must manually toggle the permission in settings
- Use AppState to detect when user returns from settings

### Widget doesn't survive app kill

- Ensure foreground service is running (check notification)
- Check if battery optimization is enabled for your app
- Some manufacturers (Xiaomi, Huawei) aggressively kill background services

### Widget doesn't start on boot

- Ensure `autoStartOnBoot: true` in config
- Check if `RECEIVE_BOOT_COMPLETED` permission is granted
- Some manufacturers require additional settings (e.g., "Autostart" permission)

## Platform Support

- **Android**: API 21+ (Android 5.0+)
- **iOS**: Not supported (library is Android-only)

## License

MIT

## Contributing

Contributions are welcome! Please open an issue or submit a PR.

## Author

Your Name

## Acknowledgments

Built with React Native and Kotlin.
