# Complete Examples

This document contains complete, copy-paste ready examples for common use cases.

## Table of Contents

1. [Basic Setup](#basic-setup)
2. [Complete App Example](#complete-app-example)
3. [Permission Handling](#permission-handling)
4. [Custom Widget Icon](#custom-widget-icon)
5. [Boot Receiver](#boot-receiver)
6. [Settings Screen](#settings-screen)

## Basic Setup

The simplest way to integrate the widget:

```typescript
import React, { useEffect } from 'react';
import { View, Text, Button } from 'react-native';
import FloatingAppWidget from 'react-native-floating-app-widget';

export default function App() {
  useEffect(() => {
    setupWidget();
  }, []);

  async function setupWidget() {
    try {
      await FloatingAppWidget.init({
        notification: {
          title: 'My App',
          text: 'Tap to open',
        },
      });

      const hasPermission = await FloatingAppWidget.hasPermission();
      if (hasPermission) {
        await FloatingAppWidget.start();
      }
    } catch (error) {
      console.error(error);
    }
  }

  return (
    <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
      <Text>Widget is running!</Text>
    </View>
  );
}
```

## Complete App Example

A full-featured app with all widget controls:

```typescript
import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  Button,
  StyleSheet,
  AppState,
  Alert,
} from 'react-native';
import FloatingAppWidget from 'react-native-floating-app-widget';

export default function App() {
  const [hasPermission, setHasPermission] = useState(false);
  const [isWidgetRunning, setIsWidgetRunning] = useState(false);
  const [isInitialized, setIsInitialized] = useState(false);

  useEffect(() => {
    initializeWidget();

    // Listen for app state changes (user returning from settings)
    const subscription = AppState.addEventListener('change', handleAppStateChange);

    return () => {
      subscription.remove();
    };
  }, []);

  async function initializeWidget() {
    try {
      // Initialize widget
      await FloatingAppWidget.init({
        size: 60,
        shape: 'circle',
        draggable: true,
        notification: {
          title: 'My App Widget',
          text: 'Tap the floating icon to return to the app',
          channelId: 'widget_channel',
          channelName: 'Widget Service',
        },
        hideOnAppOpen: true,
        autoStartOnBoot: false,
      });

      setIsInitialized(true);

      // Check permission
      const permission = await FloatingAppWidget.hasPermission();
      setHasPermission(permission);
    } catch (error) {
      console.error('Failed to initialize widget:', error);
      Alert.alert('Error', 'Failed to initialize widget');
    }
  }

  async function handleAppStateChange(nextAppState: string) {
    if (nextAppState === 'active') {
      // App came to foreground, check permission again
      const permission = await FloatingAppWidget.hasPermission();
      setHasPermission(permission);

      // Auto-start if permission was just granted
      if (permission && !isWidgetRunning) {
        handleStartWidget();
      }
    }
  }

  async function handleRequestPermission() {
    try {
      await FloatingAppWidget.requestPermission();
      // Permission will be checked when user returns (AppState listener)
    } catch (error) {
      console.error('Failed to request permission:', error);
      Alert.alert('Error', 'Failed to request permission');
    }
  }

  async function handleStartWidget() {
    try {
      await FloatingAppWidget.start();
      setIsWidgetRunning(true);
      Alert.alert('Success', 'Widget started! Go to home screen to see it.');
    } catch (error: any) {
      console.error('Failed to start widget:', error);
      Alert.alert('Error', error.message || 'Failed to start widget');
    }
  }

  async function handleStopWidget() {
    try {
      await FloatingAppWidget.stop();
      setIsWidgetRunning(false);
      Alert.alert('Success', 'Widget stopped');
    } catch (error) {
      console.error('Failed to stop widget:', error);
      Alert.alert('Error', 'Failed to stop widget');
    }
  }

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Floating Widget Example</Text>

      <View style={styles.statusContainer}>
        <StatusItem
          label="Initialized"
          value={isInitialized}
        />
        <StatusItem
          label="Permission Granted"
          value={hasPermission}
        />
        <StatusItem
          label="Widget Running"
          value={isWidgetRunning}
        />
      </View>

      <View style={styles.buttonContainer}>
        {!hasPermission && (
          <Button
            title="Request Permission"
            onPress={handleRequestPermission}
          />
        )}

        {hasPermission && !isWidgetRunning && (
          <Button
            title="Start Widget"
            onPress={handleStartWidget}
          />
        )}

        {isWidgetRunning && (
          <Button
            title="Stop Widget"
            onPress={handleStopWidget}
            color="red"
          />
        )}
      </View>

      <Text style={styles.instructions}>
        {!hasPermission
          ? 'Grant permission to display the widget'
          : !isWidgetRunning
          ? 'Start the widget and press home button to see it'
          : 'Widget is running! Press home to see it.'}
      </Text>
    </View>
  );
}

function StatusItem({ label, value }: { label: string; value: boolean }) {
  return (
    <View style={styles.statusItem}>
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
  statusItem: {
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

## Permission Handling

Robust permission handling with user guidance:

```typescript
import { Alert, Linking, AppState } from 'react-native';
import FloatingAppWidget from 'react-native-floating-app-widget';

class WidgetPermissionManager {
  private appStateSubscription: any;
  private onPermissionGranted?: () => void;

  async checkAndRequestPermission(
    onGranted: () => void,
    onDenied?: () => void
  ): Promise<void> {
    const hasPermission = await FloatingAppWidget.hasPermission();

    if (hasPermission) {
      onGranted();
      return;
    }

    // Show explanation first
    Alert.alert(
      'Permission Required',
      'This app needs permission to display a floating widget. This allows you to quickly access the app from anywhere.',
      [
        {
          text: 'Cancel',
          style: 'cancel',
          onPress: onDenied,
        },
        {
          text: 'Grant Permission',
          onPress: () => this.requestWithCallback(onGranted, onDenied),
        },
      ]
    );
  }

  private async requestWithCallback(
    onGranted: () => void,
    onDenied?: () => void
  ): Promise<void> {
    this.onPermissionGranted = onGranted;

    // Listen for app returning from settings
    this.appStateSubscription = AppState.addEventListener(
      'change',
      this.handleAppStateChange.bind(this)
    );

    // Request permission (opens settings)
    await FloatingAppWidget.requestPermission();
  }

  private async handleAppStateChange(nextAppState: string) {
    if (nextAppState === 'active') {
      // User returned from settings
      const hasPermission = await FloatingAppWidget.hasPermission();

      if (hasPermission && this.onPermissionGranted) {
        this.onPermissionGranted();
      }

      // Cleanup
      this.cleanup();
    }
  }

  private cleanup() {
    if (this.appStateSubscription) {
      this.appStateSubscription.remove();
      this.appStateSubscription = null;
    }
    this.onPermissionGranted = undefined;
  }
}

// Usage
const permissionManager = new WidgetPermissionManager();

permissionManager.checkAndRequestPermission(
  async () => {
    // Permission granted
    await FloatingAppWidget.start();
  },
  () => {
    // Permission denied
    console.log('User denied permission');
  }
);
```

## Custom Widget Icon

Using a custom icon from your app's assets:

```typescript
import { Image } from 'react-native';
import RNFS from 'react-native-fs';
import FloatingAppWidget from 'react-native-floating-app-widget';

async function setupWidgetWithCustomIcon() {
  // Method 1: Using a local image file
  const iconPath = `${RNFS.DocumentDirectoryPath}/widget-icon.png`;
  const iconBase64 = await RNFS.readFile(iconPath, 'base64');

  // Method 2: Using a bundled asset
  // First, get the asset and convert it to base64
  const asset = Image.resolveAssetSource(require('./assets/widget-icon.png'));
  // You'll need to fetch and convert this URL to base64

  await FloatingAppWidget.init({
    icon: `data:image/png;base64,${iconBase64}`,
    size: 64,
    shape: 'circle',
    notification: {
      title: 'Custom Widget',
      text: 'With custom icon',
    },
  });
}

// Helper: Convert network image to base64
async function imageUrlToBase64(url: string): Promise<string> {
  const response = await fetch(url);
  const blob = await response.blob();

  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onloadend = () => {
      const base64 = reader.result as string;
      resolve(base64);
    };
    reader.onerror = reject;
    reader.readAsDataURL(blob);
  });
}
```

## Boot Receiver

Enable widget to start automatically on device boot:

```typescript
import FloatingAppWidget from 'react-native-floating-app-widget';

async function setupAutoStartWidget() {
  await FloatingAppWidget.init({
    notification: {
      title: 'My App',
      text: 'Tap to open',
    },
    // Enable auto-start on boot
    autoStartOnBoot: true,
  });

  // Start the widget
  await FloatingAppWidget.start();

  // Now the widget will automatically start when device reboots
}

// Note: User must grant SYSTEM_ALERT_WINDOW permission before reboot
// Widget won't start on boot if permission is not granted
```

## Settings Screen

A complete settings screen for widget configuration:

```typescript
import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  Switch,
  Button,
  StyleSheet,
  ScrollView,
} from 'react-native';
import FloatingAppWidget, { WidgetConfig } from 'react-native-floating-app-widget';

export default function WidgetSettings() {
  const [config, setConfig] = useState<Partial<WidgetConfig>>({
    size: 60,
    shape: 'circle',
    draggable: true,
    hideOnAppOpen: true,
    autoStartOnBoot: false,
    notification: {
      title: 'My App Widget',
      text: 'Tap to open app',
    },
  });

  const [isRunning, setIsRunning] = useState(false);

  useEffect(() => {
    loadCurrentConfig();
  }, []);

  async function loadCurrentConfig() {
    const currentConfig = FloatingAppWidget.getConfig();
    if (currentConfig) {
      setConfig(currentConfig);
    }
    setIsRunning(FloatingAppWidget.isInitialized());
  }

  async function handleSave() {
    try {
      if (FloatingAppWidget.isInitialized()) {
        await FloatingAppWidget.update(config as WidgetConfig);
      } else {
        await FloatingAppWidget.init(config as WidgetConfig);
      }
      alert('Settings saved!');
    } catch (error) {
      alert('Failed to save settings');
    }
  }

  function updateConfig<K extends keyof WidgetConfig>(
    key: K,
    value: WidgetConfig[K]
  ) {
    setConfig((prev) => ({ ...prev, [key]: value }));
  }

  return (
    <ScrollView style={styles.container}>
      <Text style={styles.title}>Widget Settings</Text>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Appearance</Text>

        <SettingRow label="Size (dp)">
          <Text>{config.size}</Text>
        </SettingRow>

        <SettingRow label="Shape">
          <Button
            title={config.shape === 'circle' ? 'Circle' : 'Rounded'}
            onPress={() =>
              updateConfig('shape', config.shape === 'circle' ? 'rounded' : 'circle')
            }
          />
        </SettingRow>
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Behavior</Text>

        <SettingRow label="Draggable">
          <Switch
            value={config.draggable}
            onValueChange={(value) => updateConfig('draggable', value)}
          />
        </SettingRow>

        <SettingRow label="Hide when app opens">
          <Switch
            value={config.hideOnAppOpen}
            onValueChange={(value) => updateConfig('hideOnAppOpen', value)}
          />
        </SettingRow>

        <SettingRow label="Auto-start on boot">
          <Switch
            value={config.autoStartOnBoot}
            onValueChange={(value) => updateConfig('autoStartOnBoot', value)}
          />
        </SettingRow>
      </View>

      <Button title="Save Settings" onPress={handleSave} />
    </ScrollView>
  );
}

function SettingRow({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <View style={styles.settingRow}>
      <Text style={styles.settingLabel}>{label}</Text>
      {children}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 20,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 20,
  },
  section: {
    marginBottom: 30,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '600',
    marginBottom: 15,
  },
  settingRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  settingLabel: {
    fontSize: 16,
  },
});
```

## Testing the Widget

Steps to test the widget functionality:

1. **Start the app**
2. **Grant permission** when prompted
3. **Start the widget** using the button
4. **Press the home button** to background the app
5. **Verify the widget appears** on screen
6. **Drag the widget** around (if draggable is enabled)
7. **Tap the widget** to open the app
8. **Verify widget disappears** when app is in foreground

For boot testing:
1. Enable `autoStartOnBoot`
2. Start the widget
3. Restart the device
4. Verify widget starts automatically
