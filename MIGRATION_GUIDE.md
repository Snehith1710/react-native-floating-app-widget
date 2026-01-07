# Migration Guide

## Upgrading to v2.0 with Enhanced Features

This guide helps you upgrade your existing React Native Floating App Widget implementation to take advantage of the new customization features.

## What's New?

✨ **Custom Click Handler** - Define your own click behavior
✨ **Drag-to-Dismiss** - Users can remove widget by dragging to bottom
✨ **Snap-to-Edge** - Widget automatically snaps to screen edges
✨ **Smooth Animations** - Improved drag feel with velocity tracking
✨ **Event Callbacks** - Track clicks and drag operations

## Breaking Changes

**None!** All changes are backward compatible. Your existing code will continue to work without modifications.

## Step-by-Step Migration

### Step 1: Update Dependencies

```bash
npm install react-native-floating-app-widget@latest
# or
yarn add react-native-floating-app-widget@latest
```

### Step 2: Review Your Current Implementation

**Current Code (Still Works!)**:
```typescript
import FloatingAppWidget from 'react-native-floating-app-widget';

await FloatingAppWidget.init({
  icon: iconBase64,
  size: 56,
  shape: 'circle',
  notification: {
    title: 'My App',
    text: 'Running in background',
  },
});

await FloatingAppWidget.start();
```

This code continues to work exactly as before.

### Step 3: Add New Features (Optional)

#### 3.1 Add Custom Click Handler

**Before**: Widget always opened the app

**After**: You control what happens on click

```typescript
await FloatingAppWidget.init({
  icon: iconBase64,
  notification: {
    title: 'My App',
    text: 'Tap to open',
  },
  // NEW: Custom click handler
  onWidgetClick: (event) => {
    console.log('Widget clicked at:', event.timestamp);

    // Your custom logic here
    // - Track analytics
    // - Open specific screen
    // - Show notification
    // etc.

    // Optionally open app with deep link
    Linking.openURL('myapp://dashboard');
  },
});
```

#### 3.2 Enable Drag-to-Dismiss

**Before**: Widget could only be removed via notification

**After**: Users can drag widget to bottom to remove

```typescript
await FloatingAppWidget.init({
  icon: iconBase64,
  notification: {
    title: 'My App',
    text: 'Drag down to remove',
  },
  // NEW: Enable drag-to-dismiss
  enableDragToDismiss: true,
  dismissZoneHeight: 100, // Optional, defaults to 100dp
});
```

#### 3.3 Enable Snap-to-Edge

**Before**: Widget stayed where user dropped it

**After**: Widget snaps to nearest edge with smooth animation

```typescript
await FloatingAppWidget.init({
  icon: iconBase64,
  notification: {
    title: 'My App',
    text: 'Running',
  },
  // NEW: Enable snap-to-edge
  snapToEdge: true,
});
```

#### 3.4 Track Drag Events

**Before**: No visibility into widget position

**After**: Get real-time position updates

```typescript
await FloatingAppWidget.init({
  icon: iconBase64,
  notification: {
    title: 'My App',
    text: 'Running',
  },
  // NEW: Track drag operations
  onWidgetDrag: (event) => {
    console.log('Widget at:', event.x, event.y);
    console.log('In dismiss zone:', event.inDismissZone);

    // Save position for restoration
    saveWidgetPosition({ x: event.x, y: event.y });
  },
});
```

### Step 4: Combine Features

You can enable multiple features together:

```typescript
await FloatingAppWidget.init({
  icon: await getIconBase64(),
  size: 56,
  shape: 'circle',
  notification: {
    title: 'My App Running',
    text: 'Tap to open, drag to reposition or remove',
    channelId: 'widget_channel',
    channelName: 'Widget Service',
  },
  hideOnAppOpen: true,
  draggable: true,

  // Enable all new features
  onWidgetClick: handleWidgetClick,
  onWidgetDrag: handleWidgetDrag,
  enableDragToDismiss: true,
  dismissZoneHeight: 120,
  snapToEdge: true,
});
```

## Common Patterns

### Pattern 1: Deep Linking on Click

Replace automatic app opening with navigation to specific screen:

```typescript
onWidgetClick: async (event) => {
  const userState = await getUserState();

  if (userState.isLoggedIn) {
    Linking.openURL('myapp://dashboard');
  } else {
    Linking.openURL('myapp://login');
  }
}
```

### Pattern 2: Position Persistence

Save and restore widget position:

```typescript
// Initialize with saved position
const savedPosition = await AsyncStorage.getItem('widgetPos');
await FloatingAppWidget.init({
  icon: iconBase64,
  initialPosition: savedPosition ? JSON.parse(savedPosition) : undefined,
  notification: {
    title: 'My App',
    text: 'Running',
  },
  onWidgetDrag: debounce((event) => {
    if (!event.inDismissZone) {
      AsyncStorage.setItem('widgetPos', JSON.stringify({
        x: event.x,
        y: event.y,
      }));
    }
  }, 1000),
});
```

### Pattern 3: Dynamic Icon Updates

Update widget appearance based on app state:

```typescript
// Initial setup
await FloatingAppWidget.init({
  icon: await getIconForState('idle'),
  notification: {
    title: 'Idle',
    text: 'Tap to start',
  },
});

// Update when state changes
const updateWidgetState = async (newState: string) => {
  await FloatingAppWidget.update({
    icon: await getIconForState(newState),
    notification: {
      title: newState,
      text: 'Tap to open',
    },
  });
};
```

### Pattern 4: Analytics Tracking

Track user interactions without opening app:

```typescript
onWidgetClick: (event) => {
  // Track analytics
  analytics.track('widget_clicked', {
    timestamp: event.timestamp,
    sessionId: getCurrentSessionId(),
  });

  // Still open app if needed
  Linking.openURL('myapp://');
},

onWidgetDrag: (event) => {
  if (event.inDismissZone) {
    analytics.track('widget_dismiss_attempt');
  }
}
```

## TypeScript Support

All new features are fully typed. Your IDE will provide autocomplete and type checking:

```typescript
import FloatingAppWidget, {
  WidgetConfig,
  WidgetClickEvent,
  WidgetDragEvent
} from 'react-native-floating-app-widget';

const config: WidgetConfig = {
  icon: iconBase64,
  notification: {
    title: 'My App',
    text: 'Running',
  },
  onWidgetClick: (event: WidgetClickEvent) => {
    // event.timestamp is typed as number
  },
  onWidgetDrag: (event: WidgetDragEvent) => {
    // event.x, event.y are typed as number
    // event.inDismissZone is typed as boolean
  },
  enableDragToDismiss: true, // typed as boolean
  dismissZoneHeight: 100, // typed as number
  snapToEdge: true, // typed as boolean
};
```

## Troubleshooting

### Issue: Click callback not firing

**Check**:
1. Callback is registered in `init()`, not after `start()`
2. Widget has proper permissions
3. App is not in foreground (if `hideOnAppOpen: true`)

**Solution**:
```typescript
// Correct order
await FloatingAppWidget.init({ onWidgetClick: handler });
await FloatingAppWidget.start();
```

### Issue: Drag events firing too frequently

**Problem**: Drag events fire on every touch move

**Solution**: Use debouncing for expensive operations

```typescript
import { debounce } from 'lodash';

onWidgetDrag: debounce((event) => {
  // This will only fire once per second at most
  saveToDatabase(event);
}, 1000)
```

### Issue: Widget not snapping to edge

**Check**:
1. `snapToEdge` is set to `true`
2. `draggable` is not set to `false`
3. Widget was actually dragged (not just clicked)

### Issue: Dismiss zone not appearing

**Check**:
1. `enableDragToDismiss` is set to `true`
2. Widget is being dragged (ACTION_MOVE received)
3. App has overlay permission

## Performance Tips

1. **Debounce Expensive Operations**
   ```typescript
   onWidgetDrag: debounce(savePosition, 1000)
   ```

2. **Avoid Heavy Computations in Callbacks**
   ```typescript
   onWidgetClick: (event) => {
     // Good: Queue operation
     setTimeout(() => doExpensiveWork(), 0);

     // Bad: Block UI thread
     // doExpensiveWork();
   }
   ```

3. **Clean Up Event Listeners**
   ```typescript
   useEffect(() => {
     FloatingAppWidget.init(config);
     return () => {
       FloatingAppWidget.stop(); // Removes listeners
     };
   }, []);
   ```

## Getting Help

- **Examples**: See [USAGE_EXAMPLES.md](./USAGE_EXAMPLES.md)
- **API Reference**: See main [README.md](./README.md)
- **Changes**: See [CHANGES_SUMMARY.md](./CHANGES_SUMMARY.md)
- **Issues**: Report at [GitHub Issues](https://github.com/your-repo/issues)

## Summary

The migration is completely optional. Your existing code will continue to work, and you can adopt new features incrementally:

1. ✅ **No breaking changes** - existing code works as-is
2. ✅ **Opt-in features** - enable only what you need
3. ✅ **Full TypeScript support** - types for all new APIs
4. ✅ **Comprehensive examples** - see USAGE_EXAMPLES.md
5. ✅ **Backward compatible** - supports Android 5.0+

Start with one feature (e.g., custom click handler) and gradually adopt others as needed.
