# Usage Examples - React Native Floating App Widget

This guide demonstrates how to use the new customization features added to the library.

## Table of Contents

- [Custom Click Handler](#custom-click-handler)
- [Custom Icon Rendering](#custom-icon-rendering)
- [Drag to Dismiss](#drag-to-dismiss)
- [Snap to Edge](#snap-to-edge)
- [Tracking Drag Events](#tracking-drag-events)
- [Complete Example](#complete-example)

## Custom Click Handler

Instead of the widget always opening your app, you can now define custom behavior when the widget is clicked:

```typescript
import FloatingAppWidget from 'react-native-floating-app-widget';

await FloatingAppWidget.init({
  icon: 'data:image/png;base64,...', // Your icon as base64
  notification: {
    title: 'My App is Running',
    text: 'Tap to return to app',
  },
  onWidgetClick: (event) => {
    console.log('Widget clicked at:', event.timestamp);

    // Custom behavior: Show a notification, log analytics, etc.
    // The app will NOT automatically open unless you explicitly do so

    // You can still open the app programmatically if needed
    // Or perform any other action like opening a deep link
  },
});
```

### Use Cases for Custom Click Handler

1. **Analytics Tracking**: Track widget interactions without opening the app
2. **Deep Linking**: Open specific screens based on widget state
3. **Conditional Navigation**: Decide where to navigate based on app state
4. **Custom Actions**: Trigger background tasks or API calls

```typescript
// Example: Deep linking based on widget state
onWidgetClick: async (event) => {
  const userState = await getUserState();

  if (userState.hasActiveSession) {
    Linking.openURL('myapp://dashboard');
  } else {
    Linking.openURL('myapp://login');
  }
}
```

## Custom Icon Rendering

While the library currently accepts base64-encoded images, you have full control over:
- Icon size and shape
- Padding and styling
- Image source (local assets or remote URLs)

### Using Local Assets

```typescript
import { Image } from 'react-native';
import { Asset } from 'expo-asset';

// Convert local asset to base64
const getBase64Icon = async () => {
  const asset = Asset.fromModule(require('./assets/widget-icon.png'));
  await asset.downloadAsync();

  const response = await fetch(asset.localUri);
  const blob = await response.blob();

  return new Promise((resolve) => {
    const reader = new FileReader();
    reader.onloadend = () => resolve(reader.result.split(',')[1]);
    reader.readAsDataURL(blob);
  });
};

// Use in widget config
const iconBase64 = await getBase64Icon();

await FloatingAppWidget.init({
  icon: iconBase64,
  size: 64, // Custom size in dp
  shape: 'rounded', // or 'circle'
  notification: {
    title: 'My App',
    text: 'Running in background',
  },
});
```

### Dynamic Icon Updates

```typescript
// Update the widget icon based on app state
const updateWidgetIcon = async (status: 'active' | 'idle' | 'busy') => {
  let icon;

  switch (status) {
    case 'active':
      icon = await getBase64Icon(require('./icons/active.png'));
      break;
    case 'idle':
      icon = await getBase64Icon(require('./icons/idle.png'));
      break;
    case 'busy':
      icon = await getBase64Icon(require('./icons/busy.png'));
      break;
  }

  await FloatingAppWidget.update({
    icon,
    notification: {
      title: `Status: ${status}`,
      text: 'Tap to open app',
    },
  });
};
```

## Drag to Dismiss

Enable users to remove the widget by dragging it to the bottom of the screen:

```typescript
await FloatingAppWidget.init({
  icon: iconBase64,
  notification: {
    title: 'My App',
    text: 'Running in background',
  },
  enableDragToDismiss: true, // Enable drag-to-dismiss
  dismissZoneHeight: 120, // Height of dismiss zone in dp (default: 100)
});
```

### How It Works

1. When dragging starts, a red dismiss zone appears at the bottom of the screen
2. The zone highlights when the widget enters it
3. Releasing the widget inside the zone removes it from the screen
4. Releasing outside the zone keeps the widget visible

### Customizing Dismiss Behavior

```typescript
await FloatingAppWidget.init({
  icon: iconBase64,
  notification: {
    title: 'My App',
    text: 'Drag down to remove',
  },
  enableDragToDismiss: true,
  dismissZoneHeight: 150, // Larger dismiss zone
  onWidgetDrag: (event) => {
    if (event.inDismissZone) {
      console.log('Widget entering dismiss zone!');
      // Optionally show UI feedback in your app
    }
  },
});
```

## Snap to Edge

Make the widget automatically snap to the nearest screen edge after dragging:

```typescript
await FloatingAppWidget.init({
  icon: iconBase64,
  notification: {
    title: 'My App',
    text: 'Running in background',
  },
  snapToEdge: true, // Enable snap-to-edge
});
```

### Behavior

- After dragging, the widget smoothly animates to the closest horizontal edge
- Uses a deceleration interpolator for natural-feeling motion
- Animation duration: 300ms
- Automatically cancels if the widget is removed during animation

### Combined with Drag-to-Dismiss

```typescript
await FloatingAppWidget.init({
  icon: iconBase64,
  notification: {
    title: 'My App',
    text: 'Drag to reposition or remove',
  },
  snapToEdge: true, // Snap to edge after dragging
  enableDragToDismiss: true, // Also enable drag-to-dismiss
  dismissZoneHeight: 100,
});
```

## Tracking Drag Events

Monitor widget position and state during dragging:

```typescript
await FloatingAppWidget.init({
  icon: iconBase64,
  notification: {
    title: 'My App',
    text: 'Running',
  },
  enableDragToDismiss: true,
  onWidgetDrag: (event) => {
    console.log('Widget position:', event.x, event.y);
    console.log('In dismiss zone:', event.inDismissZone);

    // Update UI or analytics based on position
    if (event.inDismissZone) {
      // Show warning in app that widget will be removed
      showWidgetRemovalWarning();
    } else {
      hideWidgetRemovalWarning();
    }
  },
});
```

### Use Cases for Drag Tracking

1. **Position Persistence**: Save widget position for restoration
2. **Analytics**: Track how users interact with the widget
3. **UI Feedback**: Show in-app indicators based on widget state
4. **Gesture Recognition**: Detect patterns in user dragging behavior

```typescript
// Example: Save widget position
let widgetPosition = { x: 0, y: 0 };

onWidgetDrag: (event) => {
  widgetPosition = { x: event.x, y: event.y };

  // Debounced save to AsyncStorage
  debouncedSave(widgetPosition);
}

// Restore position on next app launch
const savedPosition = await AsyncStorage.getItem('widgetPosition');
if (savedPosition) {
  const { x, y } = JSON.parse(savedPosition);
  await FloatingAppWidget.init({
    initialPosition: { x, y },
    // ... other config
  });
}
```

## Complete Example

Here's a full implementation using all features together:

```typescript
import React, { useEffect, useState } from 'react';
import { AppState, Linking } from 'react-native';
import FloatingAppWidget from 'react-native-floating-app-widget';
import AsyncStorage from '@react-native-async-storage/async-storage';

const useFloatingWidget = () => {
  const [widgetActive, setWidgetActive] = useState(false);

  useEffect(() => {
    initializeWidget();

    return () => {
      // Cleanup on unmount
      FloatingAppWidget.stop();
    };
  }, []);

  const getIconBase64 = async () => {
    // Convert your icon to base64
    // Implementation depends on your icon source
    return 'data:image/png;base64,...';
  };

  const initializeWidget = async () => {
    try {
      // Check and request permission
      const hasPermission = await FloatingAppWidget.hasPermission();
      if (!hasPermission) {
        await FloatingAppWidget.requestPermission();
        return;
      }

      // Get saved position if available
      const savedPosition = await AsyncStorage.getItem('widgetPosition');
      const initialPosition = savedPosition
        ? JSON.parse(savedPosition)
        : undefined;

      // Initialize widget with all features
      await FloatingAppWidget.init({
        icon: await getIconBase64(),
        size: 56,
        shape: 'circle',
        initialPosition,
        notification: {
          title: 'My App Running',
          text: 'Tap to return, drag to reposition or remove',
          channelId: 'widget_channel',
          channelName: 'Widget Service',
        },
        hideOnAppOpen: true,
        draggable: true,
        snapToEdge: true, // Snap to edges after dragging
        enableDragToDismiss: true, // Enable drag-to-dismiss
        dismissZoneHeight: 120,

        // Custom click handler
        onWidgetClick: (event) => {
          console.log('Widget clicked:', event.timestamp);

          // Track analytics
          trackEvent('widget_clicked');

          // Open app to specific screen
          Linking.openURL('myapp://dashboard');
        },

        // Track dragging
        onWidgetDrag: (event) => {
          // Save position periodically
          if (!event.inDismissZone) {
            debouncedSavePosition({ x: event.x, y: event.y });
          }

          // Log when entering dismiss zone
          if (event.inDismissZone) {
            console.log('Widget in dismiss zone');
          }
        },
      });

      // Start the widget
      await FloatingAppWidget.start();
      setWidgetActive(true);
    } catch (error) {
      console.error('Failed to initialize widget:', error);
    }
  };

  const debouncedSavePosition = debounce(async (position) => {
    await AsyncStorage.setItem('widgetPosition', JSON.stringify(position));
  }, 1000);

  const updateWidgetStatus = async (status: string) => {
    if (!widgetActive) return;

    await FloatingAppWidget.update({
      icon: await getIconForStatus(status),
      notification: {
        title: `Status: ${status}`,
        text: 'Tap to open app',
      },
    });
  };

  return {
    widgetActive,
    updateWidgetStatus,
  };
};

// Utility: Debounce function
const debounce = (func: Function, wait: number) => {
  let timeout: NodeJS.Timeout;
  return (...args: any[]) => {
    clearTimeout(timeout);
    timeout = setTimeout(() => func(...args), wait);
  };
};

export default useFloatingWidget;
```

### Using the Hook in Your App

```typescript
import React from 'react';
import { View, Button, Text } from 'react-native';
import useFloatingWidget from './useFloatingWidget';

const App = () => {
  const { widgetActive, updateWidgetStatus } = useFloatingWidget();

  return (
    <View style={{ flex: 1, justifyContent: 'center', padding: 20 }}>
      <Text>Widget Active: {widgetActive ? 'Yes' : 'No'}</Text>

      <Button
        title="Set Status: Active"
        onPress={() => updateWidgetStatus('active')}
      />
      <Button
        title="Set Status: Idle"
        onPress={() => updateWidgetStatus('idle')}
      />
    </View>
  );
};

export default App;
```

## Key Improvements Summary

### 1. **Developer Control**
- Custom click handlers instead of forced app opening
- Full control over widget behavior and navigation

### 2. **Enhanced UX**
- Smooth drag animations with velocity tracking
- Snap-to-edge for better positioning
- Visual feedback with dismiss zone
- Intuitive drag-to-remove gesture

### 3. **Flexibility**
- Event callbacks for click and drag
- Configurable dismiss zone
- Optional snap-to-edge behavior
- Complete control over icon rendering

### 4. **Better Integration**
- Track widget interactions
- Save/restore widget state
- Dynamic updates based on app state
- Deep linking support

## Migration Guide

If you're upgrading from an older version, here's what changed:

### Before (Old API)
```typescript
await FloatingAppWidget.init({
  icon: iconBase64,
  notification: {
    title: 'App Running',
    text: 'Tap to open',
  },
});
// Widget always opened the app on click
// No drag-to-dismiss
// Basic drag with no smoothing
```

### After (New API)
```typescript
await FloatingAppWidget.init({
  icon: iconBase64,
  notification: {
    title: 'App Running',
    text: 'Tap to open',
  },
  // NEW: Optional custom click handler
  onWidgetClick: (event) => {
    // Custom behavior
  },
  // NEW: Drag-to-dismiss
  enableDragToDismiss: true,
  dismissZoneHeight: 100,
  // NEW: Snap-to-edge
  snapToEdge: true,
  // NEW: Drag tracking
  onWidgetDrag: (event) => {
    // Track position
  },
});
```

All new properties are **optional** and **backward compatible**. If you don't provide them, the widget behaves exactly as before.

## Tips and Best Practices

1. **Permission Handling**: Always check and request permission before starting the widget
2. **Event Throttling**: Use debouncing when saving drag positions to avoid excessive writes
3. **Cleanup**: Stop the widget when your app unmounts to free resources
4. **User Experience**: Provide visual feedback in your app when the widget is in the dismiss zone
5. **Testing**: Test on various Android versions and screen sizes
6. **Icon Quality**: Use high-resolution icons that look good at the configured size
7. **Notification**: Keep notification text concise and informative

## Troubleshooting

### Widget doesn't respond to clicks
- Ensure `onWidgetClick` callback is properly registered
- Check that the widget view is not obscured by other overlays

### Drag-to-dismiss not working
- Verify `enableDragToDismiss` is set to `true`
- Check that `draggable` is not set to `false`

### Snap-to-edge animation is choppy
- This might occur on low-end devices
- The animation uses hardware acceleration when available

### Events not firing
- Ensure your app has proper permissions
- Check that the React Native bridge is properly initialized
- Verify event listener registration in `init()`

## Further Reading

- [Main README](./README.md) - Installation and basic setup
- [API Reference](./API.md) - Complete API documentation
- [Testing Guide](./TESTING.md) - How to test your widget implementation
