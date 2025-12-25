# Testing Guide - How to Test This Library in Your App

This guide will help you integrate and test the floating widget library in your React Native app.

## Option 1: Test Locally (Recommended for Development)

### Step 1: Link the Library Locally

In your React Native app's `package.json`, add:

```json
{
  "dependencies": {
    "react-native-floating-app-widget": "file:../react-native-floating-app-widget"
  }
}
```

Then install:

```bash
cd /path/to/your/react-native-app
npm install
# or
yarn install
```

### Step 2: Build the Library First

Before testing, build the TypeScript library:

```bash
cd /Users/lsn-snehith/react-native-floating-app-widget
npm install
npm run prepare
```

This will compile TypeScript and create the `lib/` directory.

### Step 3: Rebuild Your App

```bash
cd /path/to/your/react-native-app
npx react-native run-android
```

## Option 2: Test from GitHub

### Install from GitHub

In your app's `package.json`:

```json
{
  "dependencies": {
    "react-native-floating-app-widget": "git+https://github.com/Snehith1710/react-native-floating-app-widget.git"
  }
}
```

Then:

```bash
npm install
npx react-native run-android
```

## Option 3: Test Using npm link (Quick Testing)

### In the library directory:

```bash
cd /Users/lsn-snehith/react-native-floating-app-widget
npm install
npm run prepare
npm link
```

### In your app directory:

```bash
cd /path/to/your/react-native-app
npm link react-native-floating-app-widget
npx react-native run-android
```

## Implementation in Your App

### 1. Basic Implementation

Create a new component or add to your existing `App.tsx`:

```typescript
import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  Button,
  StyleSheet,
  AppState,
  Alert,
  Platform,
  PermissionsAndroid,
} from 'react-native';
import FloatingAppWidget from 'react-native-floating-app-widget';

export default function App() {
  const [hasPermission, setHasPermission] = useState(false);
  const [widgetRunning, setWidgetRunning] = useState(false);
  const [initialized, setInitialized] = useState(false);

  useEffect(() => {
    if (Platform.OS !== 'android') {
      console.log('Floating widget only works on Android');
      return;
    }

    setupWidget();

    // Listen for app state changes
    const subscription = AppState.addEventListener('change', handleAppStateChange);

    return () => subscription.remove();
  }, []);

  async function setupWidget() {
    try {
      // Request notification permission (Android 13+)
      if (Platform.Version >= 33) {
        await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS
        );
      }

      // Initialize widget
      await FloatingAppWidget.init({
        notification: {
          title: 'My App Widget',
          text: 'Tap the floating icon to return to the app',
          channelId: 'floating_widget',
          channelName: 'Floating Widget',
        },
        size: 60,
        shape: 'circle',
        draggable: true,
        hideOnAppOpen: true,
      });

      setInitialized(true);

      // Check permission
      const granted = await FloatingAppWidget.hasPermission();
      setHasPermission(granted);

      console.log('Widget initialized successfully');
    } catch (error) {
      console.error('Failed to initialize widget:', error);
      Alert.alert('Error', 'Failed to initialize widget');
    }
  }

  async function handleAppStateChange(state: string) {
    if (state === 'active') {
      // Check permission when app comes to foreground
      const granted = await FloatingAppWidget.hasPermission();
      setHasPermission(granted);
    }
  }

  async function handleRequestPermission() {
    try {
      await FloatingAppWidget.requestPermission();
      Alert.alert(
        'Permission Required',
        'Please grant overlay permission in the settings and return to the app'
      );
    } catch (error) {
      console.error('Failed to request permission:', error);
    }
  }

  async function handleStartWidget() {
    try {
      await FloatingAppWidget.start();
      setWidgetRunning(true);
      Alert.alert(
        'Widget Started!',
        'Press the home button to see the floating widget'
      );
    } catch (error: any) {
      console.error('Failed to start widget:', error);
      Alert.alert('Error', error.message || 'Failed to start widget');
    }
  }

  async function handleStopWidget() {
    try {
      await FloatingAppWidget.stop();
      setWidgetRunning(false);
      Alert.alert('Success', 'Widget stopped');
    } catch (error) {
      console.error('Failed to stop widget:', error);
    }
  }

  if (Platform.OS !== 'android') {
    return (
      <View style={styles.container}>
        <Text>This library only works on Android</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Floating Widget Test</Text>

      <View style={styles.statusContainer}>
        <StatusRow label="Initialized" value={initialized} />
        <StatusRow label="Permission" value={hasPermission} />
        <StatusRow label="Running" value={widgetRunning} />
      </View>

      <View style={styles.buttonContainer}>
        {!hasPermission && (
          <Button title="Request Permission" onPress={handleRequestPermission} />
        )}

        {hasPermission && !widgetRunning && (
          <Button title="Start Widget" onPress={handleStartWidget} />
        )}

        {widgetRunning && (
          <Button title="Stop Widget" onPress={handleStopWidget} color="red" />
        )}
      </View>

      <Text style={styles.instructions}>
        {!initialized
          ? 'Initializing...'
          : !hasPermission
          ? 'Grant overlay permission to continue'
          : !widgetRunning
          ? 'Start the widget and press home button'
          : 'Widget is running! Press home to see it'}
      </Text>
    </View>
  );
}

function StatusRow({ label, value }: { label: string; value: boolean }) {
  return (
    <View style={styles.statusRow}>
      <Text style={styles.statusLabel}>{label}:</Text>
      <Text style={[styles.statusValue, value ? styles.success : styles.error]}>
        {value ? '✓' : '✗'}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 20,
    justifyContent: 'center',
    backgroundColor: '#fff',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    textAlign: 'center',
    marginBottom: 30,
  },
  statusContainer: {
    marginBottom: 30,
  },
  statusRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 10,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  statusLabel: {
    fontSize: 16,
  },
  statusValue: {
    fontSize: 18,
    fontWeight: 'bold',
  },
  success: {
    color: 'green',
  },
  error: {
    color: 'red',
  },
  buttonContainer: {
    gap: 10,
    marginBottom: 20,
  },
  instructions: {
    textAlign: 'center',
    color: '#666',
    marginTop: 20,
  },
});
```

## Testing Steps

### 1. Build and Run

```bash
cd /path/to/your/react-native-app
npx react-native run-android
```

### 2. Grant Permission

1. Tap **"Request Permission"** button
2. You'll be taken to Android Settings
3. Toggle **"Allow display over other apps"** to ON
4. Press back button to return to your app
5. The permission status should update to ✓

### 3. Start Widget

1. Tap **"Start Widget"** button
2. You should see a success message
3. Check notification drawer - you should see a persistent notification

### 4. Test Widget Visibility

1. Press the **home button** on your device
2. The floating widget should appear (circular icon)
3. Try dragging the widget around
4. Tap the widget - it should open your app
5. Widget should disappear when app is in foreground

### 5. Test Widget Persistence

1. With widget running, force-close your app
2. Widget should still be visible
3. Notification should still be present
4. Tap widget to reopen app

### 6. Stop Widget

1. Tap **"Stop Widget"** button in your app
2. Widget should disappear
3. Notification should be removed

## Debugging

### Check Logs

```bash
# Filter for widget logs
adb logcat | grep -i "floating"

# Check for errors
adb logcat *:E
```

### Common Issues

**Widget doesn't appear:**
- Check permission is granted
- Make sure you pressed home button (widget shows in background only)
- Check logs for errors

**Permission request fails:**
- Ensure you're on Android 6.0+ (API 23+)
- Try manually going to Settings → Apps → Your App → Display over other apps

**Widget doesn't survive app kill:**
- Check notification is visible
- Some manufacturers block background services - check battery optimization settings

**App crashes:**
- Check Android version compatibility (min SDK 21)
- Verify all dependencies are installed
- Check logs for specific errors

## Advanced Testing

### Test Custom Icon

```typescript
import RNFS from 'react-native-fs';

// Convert image to base64
const iconPath = RNFS.MainBundlePath + '/custom_icon.png';
const iconBase64 = await RNFS.readFile(iconPath, 'base64');

await FloatingAppWidget.init({
  icon: `data:image/png;base64,${iconBase64}`,
  // ... other config
});
```

### Test Auto-Start on Boot

```typescript
await FloatingAppWidget.init({
  autoStartOnBoot: true,
  // ... other config
});

// Start widget, then restart device
// Widget should auto-start after reboot
```

### Test Different Shapes

```typescript
// Circle
await FloatingAppWidget.update({
  shape: 'circle',
  size: 56,
  notification: { /* ... */ }
});

// Rounded
await FloatingAppWidget.update({
  shape: 'rounded',
  size: 64,
  notification: { /* ... */ }
});
```

## Next Steps

1. ✅ Test basic widget functionality
2. ✅ Test permission flow
3. ✅ Test widget persistence
4. ✅ Test customization options
5. 📝 Create your own widget design
6. 🚀 Publish your app!

## Need Help?

- Check [README.md](README.md) for API reference
- See [EXAMPLE.md](EXAMPLE.md) for more examples
- Check [TROUBLESHOOTING.md](SETUP.md#troubleshooting) for common issues
- Open an issue on GitHub: https://github.com/Snehith1710/react-native-floating-app-widget/issues
