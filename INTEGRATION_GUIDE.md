# Integration Guide for App Developers

This guide explains how to integrate `react-native-floating-app-widget` into your existing React Native app.

## Prerequisites

- React Native 0.60+ (auto-linking support)
- Android minSdkVersion 21 (Android 5.0)
- Android targetSdkVersion 33+ recommended

## Step-by-Step Integration

### Step 1: Install the Library

```bash
npm install react-native-floating-app-widget
# or
yarn add react-native-floating-app-widget
```

### Step 2: Rebuild the App

The library uses auto-linking, so just rebuild:

```bash
npx react-native run-android
```

### Step 3: Request Notification Permission (Android 13+)

For Android 13 and above, you need to request notification permission:

```typescript
import { PermissionsAndroid, Platform } from 'react-native';

async function requestNotificationPermission() {
  if (Platform.OS === 'android' && Platform.Version >= 33) {
    const granted = await PermissionsAndroid.request(
      PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS
    );
    return granted === PermissionsAndroid.RESULTS.GRANTED;
  }
  return true;
}
```

### Step 4: Initialize the Widget

In your app's entry point (e.g., `App.tsx`):

```typescript
import React, { useEffect } from 'react';
import FloatingAppWidget from 'react-native-floating-app-widget';

function App() {
  useEffect(() => {
    initWidget();
  }, []);

  async function initWidget() {
    try {
      // Request notification permission first
      await requestNotificationPermission();

      // Initialize widget
      await FloatingAppWidget.init({
        notification: {
          title: 'My App',
          text: 'Tap the widget to return',
          channelId: 'widget_service',
          channelName: 'Widget Service',
        },
        size: 60,
        shape: 'circle',
        draggable: true,
        hideOnAppOpen: true,
      });

      console.log('Widget initialized');
    } catch (error) {
      console.error('Failed to initialize widget:', error);
    }
  }

  return (
    // Your app UI
  );
}

export default App;
```

### Step 5: Add Widget Controls

Create a settings screen or toggle to let users enable/disable the widget:

```typescript
import React, { useState, useEffect } from 'react';
import { View, Button, Text, AppState } from 'react-native';
import FloatingAppWidget from 'react-native-floating-app-widget';

function WidgetControl() {
  const [hasPermission, setHasPermission] = useState(false);
  const [isRunning, setIsRunning] = useState(false);

  useEffect(() => {
    checkPermission();

    // Listen for app state changes (user returning from settings)
    const subscription = AppState.addEventListener('change', async (state) => {
      if (state === 'active') {
        await checkPermission();
      }
    });

    return () => subscription.remove();
  }, []);

  async function checkPermission() {
    const granted = await FloatingAppWidget.hasPermission();
    setHasPermission(granted);
  }

  async function handleRequestPermission() {
    await FloatingAppWidget.requestPermission();
    // Permission will be checked when app becomes active again
  }

  async function handleStart() {
    try {
      await FloatingAppWidget.start();
      setIsRunning(true);
    } catch (error: any) {
      alert(error.message);
    }
  }

  async function handleStop() {
    await FloatingAppWidget.stop();
    setIsRunning(false);
  }

  if (!hasPermission) {
    return (
      <View>
        <Text>Widget requires overlay permission</Text>
        <Button title="Grant Permission" onPress={handleRequestPermission} />
      </View>
    );
  }

  return (
    <View>
      <Text>Widget: {isRunning ? 'Running' : 'Stopped'}</Text>
      {isRunning ? (
        <Button title="Stop Widget" onPress={handleStop} />
      ) : (
        <Button title="Start Widget" onPress={handleStart} />
      )}
    </View>
  );
}
```

## Important Considerations

### 1. User Experience

**Always explain why you need the permission:**

```typescript
import { Alert } from 'react-native';

function showPermissionRationale() {
  Alert.alert(
    'Floating Widget',
    'Enable the floating widget to quickly access this app from anywhere on your device.',
    [
      { text: 'Not Now', style: 'cancel' },
      { text: 'Enable', onPress: () => FloatingAppWidget.requestPermission() },
    ]
  );
}
```

### 2. Notification Channel Customization

Customize the notification to match your app's branding:

```typescript
await FloatingAppWidget.init({
  notification: {
    title: 'YourAppName is Active',
    text: 'Tap the floating icon to return',
    channelId: 'your_app_widget',
    channelName: 'Quick Access Widget',
    icon: 'ic_notification', // Your drawable resource
  },
});
```

### 3. Widget Icon

To use a custom widget icon, convert it to base64:

```typescript
import RNFS from 'react-native-fs';

const iconPath = 'path/to/your/icon.png';
const iconBase64 = await RNFS.readFile(iconPath, 'base64');

await FloatingAppWidget.init({
  icon: `data:image/png;base64,${iconBase64}`,
  // ... other config
});
```

### 4. Auto-Start on Boot

If you want the widget to start on device boot:

```typescript
await FloatingAppWidget.init({
  autoStartOnBoot: true,
  // ... other config
});
```

**Note:** This requires the widget to be running before the device reboots.

### 5. Battery Optimization

Some devices (especially Xiaomi, Huawei, Oppo) may kill the foreground service. Inform users to:

1. Disable battery optimization for your app
2. Enable "Autostart" permission (manufacturer-specific)

## Testing Checklist

- [ ] Widget appears when app goes to background
- [ ] Widget disappears when app comes to foreground (if `hideOnAppOpen: true`)
- [ ] Widget is draggable (if `draggable: true`)
- [ ] Tapping widget opens the app
- [ ] Widget survives app kill
- [ ] Widget starts on boot (if enabled)
- [ ] Notification appears in notification drawer
- [ ] Stopping widget removes it completely

## Troubleshooting Common Issues

### Issue: Widget doesn't appear

**Solution:**
1. Check permission: `await FloatingAppWidget.hasPermission()`
2. Ensure widget is started: `await FloatingAppWidget.start()`
3. Press home button (widget only shows when app is backgrounded)

### Issue: Widget appears even when app is open

**Solution:**
Set `hideOnAppOpen: true` in config.

### Issue: Widget doesn't survive app kill

**Solution:**
1. Check if foreground service notification is visible
2. Disable battery optimization for your app
3. Check device manufacturer settings (Xiaomi, Huawei require additional permissions)

### Issue: Widget doesn't start on boot

**Solution:**
1. Ensure `autoStartOnBoot: true` is set
2. Widget must be running before reboot
3. Check if device requires "Autostart" permission
4. Some manufacturers block boot receivers by default

## Play Store Considerations

### Required Disclosures

When publishing to Play Store with `SYSTEM_ALERT_WINDOW` permission:

1. **Declare in app description** why you use overlay permission
2. **Provide in-app explanation** before requesting permission
3. **Make it optional** - don't force users to grant it
4. **Don't show ads** in the overlay

### Permission Declaration

Add this to your app's privacy policy:

> This app uses the Display over other apps permission (SYSTEM_ALERT_WINDOW) to show a floating widget for quick access to the app. The widget only appears when the app is in the background and can be disabled at any time in the app settings.

### Example App Description

> Quick Access Widget: Enable the optional floating widget to access [YourApp] from anywhere on your device. The widget appears as a small icon when the app is in the background, and tapping it brings you back to the app instantly.

## Advanced Usage

### Conditional Widget

Only show widget for certain user types or features:

```typescript
async function setupConditionalWidget(isPremiumUser: boolean) {
  if (isPremiumUser) {
    await FloatingAppWidget.init({
      notification: {
        title: 'Premium Quick Access',
        text: 'Tap to return to app',
      },
    });

    const hasPermission = await FloatingAppWidget.hasPermission();
    if (hasPermission) {
      await FloatingAppWidget.start();
    }
  }
}
```

### Dynamic Widget Updates

Update widget appearance based on app state:

```typescript
async function updateWidgetForState(state: 'idle' | 'active' | 'busy') {
  const configs = {
    idle: { size: 56, shape: 'circle' as const },
    active: { size: 64, shape: 'circle' as const },
    busy: { size: 60, shape: 'rounded' as const },
  };

  await FloatingAppWidget.update({
    ...configs[state],
    notification: {
      title: `App ${state}`,
      text: 'Tap to open',
    },
  });
}
```

## Support

For issues, questions, or feature requests:
- GitHub Issues: https://github.com/yourusername/react-native-floating-app-widget/issues
- Documentation: See README.md and EXAMPLE.md
