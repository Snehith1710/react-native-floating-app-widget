# Quick Start Guide

Get your floating widget running in 5 minutes!

## Installation

```bash
npm install react-native-floating-app-widget
npx react-native run-android
```

## Minimal Example

Add this to your `App.tsx`:

```typescript
import React, { useEffect, useState } from 'react';
import { View, Button, Text, AppState } from 'react-native';
import FloatingAppWidget from 'react-native-floating-app-widget';

export default function App() {
  const [hasPermission, setHasPermission] = useState(false);
  const [widgetRunning, setWidgetRunning] = useState(false);

  useEffect(() => {
    // Initialize widget on app start
    FloatingAppWidget.init({
      notification: {
        title: 'My App',
        text: 'Tap to return to app',
      },
    });

    // Check initial permission
    checkPermission();

    // Listen for permission changes (when user returns from settings)
    const subscription = AppState.addEventListener('change', (state) => {
      if (state === 'active') checkPermission();
    });

    return () => subscription.remove();
  }, []);

  async function checkPermission() {
    const granted = await FloatingAppWidget.hasPermission();
    setHasPermission(granted);
  }

  async function requestPermission() {
    await FloatingAppWidget.requestPermission();
  }

  async function startWidget() {
    try {
      await FloatingAppWidget.start();
      setWidgetRunning(true);
    } catch (error: any) {
      alert(error.message);
    }
  }

  async function stopWidget() {
    await FloatingAppWidget.stop();
    setWidgetRunning(false);
  }

  return (
    <View style={{ flex: 1, justifyContent: 'center', padding: 20 }}>
      <Text style={{ fontSize: 24, marginBottom: 20, textAlign: 'center' }}>
        Floating Widget Demo
      </Text>

      <Text style={{ marginBottom: 10 }}>
        Permission: {hasPermission ? '✓ Granted' : '✗ Not granted'}
      </Text>

      <Text style={{ marginBottom: 20 }}>
        Status: {widgetRunning ? '🟢 Running' : '🔴 Stopped'}
      </Text>

      {!hasPermission && (
        <Button title="Grant Permission" onPress={requestPermission} />
      )}

      {hasPermission && !widgetRunning && (
        <Button title="Start Widget" onPress={startWidget} />
      )}

      {widgetRunning && (
        <Button title="Stop Widget" onPress={stopWidget} color="red" />
      )}
    </View>
  );
}
```

## Testing

1. Run the app: `npx react-native run-android`
2. Tap "Grant Permission" and allow overlay permission
3. Tap "Start Widget"
4. Press the **home button** (widget only appears when app is backgrounded)
5. You should see a circular floating widget
6. Tap the widget to return to your app

## That's it!

Your floating widget is now working.

### Next Steps

- Customize the widget appearance (see [README.md](README.md))
- Add custom icons (see [EXAMPLE.md](EXAMPLE.md))
- Configure auto-start on boot
- Create a settings screen

## Common Issues

**Widget doesn't appear?**
- Make sure you pressed the home button (widget only shows in background)
- Check permission is granted
- Look at Android logs: `adb logcat *:E`

**Permission request doesn't work?**
- It opens system settings, not a dialog
- User must manually toggle the permission
- App will check permission when it becomes active again

## Need Help?

- Read the full [README.md](README.md)
- Check out [EXAMPLE.md](EXAMPLE.md) for more examples
- See [SETUP.md](SETUP.md) for troubleshooting
