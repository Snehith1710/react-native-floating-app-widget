# React Native Floating App Widget - Feature Overview

A production-ready React Native library for creating persistent, draggable floating widgets (like Facebook Messenger's chat heads) that work system-wide across all apps on Android.

## 🎯 What Can Developers Do?

### 1. **Persistent Overlay Widgets**
Create floating widgets that stay on top of all apps, even when your app is in the background.

**Use Cases:**
- Quick access buttons (like screen recording, timer controls)
- Persistent notifications (ongoing calls, music player)
- Productivity tools (floating notes, calculator)
- Gaming overlays (stats, controls)
- Accessibility features (quick shortcuts)

```typescript
await FloatingWidget.init({
  size: 60,
  hideOnAppOpen: true, // Auto-hide when your app is open
});
```

---

### 2. **Full Widget Customization**

#### Visual Appearance
- **Shapes**: Circle or rounded rectangle
- **Size**: Any size in dp (default 56dp)
- **Custom Icons**: Base64 images or app icon
- **Colors**: Background, border, opacity
- **Shadows & Effects**: Border width, corner radius, padding

```typescript
appearance: {
  backgroundColor: '#007AFF',
  borderColor: '#0056b3',
  borderWidth: 2,
  cornerRadius: 30,
  opacity: 0.95,
  padding: 8,
}
```

#### Gradient Dismiss Zone (Advanced)
- **Multi-color gradients** (2-5 colors)
- **4 orientations**: Vertical, horizontal, diagonal
- **Curved corners**: Customizable corner radius
- **Smart triggers**: Show on drag or long press only

```typescript
dismissZone: {
  enabled: true,
  showOn: 'longPress', // Only show after long press
  gradientColors: ['#667eea', '#764ba2', '#f093fb'],
  activeGradientColors: ['#4568dc', '#b06ab3', '#dd5e89'],
  gradientOrientation: 'vertical',
  cornerRadius: 30,
}
```

---

### 3. **Interactive Behaviors**

#### Drag & Drop
- **Free dragging**: Move widget anywhere on screen
- **Snap to edge**: Auto-snap to screen edges
- **Position constraints**: Min/max X/Y boundaries
- **Keep on screen**: Prevent off-screen movement
- **Grid snapping**: Snap to grid positions

```typescript
draggable: true,
snapToEdge: true,
constraints: {
  keepOnScreen: true,
  snapToGrid: 10, // 10dp grid
  minY: 100,
  maxY: 800,
}
```

#### Dismiss Actions
- **Drag-to-dismiss**: Remove widget by dragging to zone
- **Long press trigger**: Show dismiss zone only on long press
- **Custom positioning**: Top or bottom of screen
- **Visual feedback**: Color changes when in dismiss zone

---

### 4. **Rich User Interactions**

**Event Callbacks:**
- `onWidgetClick` - Tap detection
- `onWidgetLongPress` - Long press with duration
- `onWidgetDrag` - Drag position tracking
- `onWidgetDismiss` - User dismissed widget
- `onWidgetShow` / `onWidgetHide` - Lifecycle events
- `onWidgetPositionChange` - Final position after drag
- `onAppForeground` / `onAppBackground` - App state monitoring

```typescript
FloatingWidget.addEventListener('onWidgetClick', () => {
  // Open your app or perform action
});

FloatingWidget.addEventListener('onWidgetDrag', (event) => {
  console.log(`Position: ${event.x}, ${event.y}`);
  console.log(`In dismiss zone: ${event.inDismissZone}`);
});
```

---

### 5. **Badge System**

Display notification counts or status indicators on the widget.

**Features:**
- Text or numeric badges
- 4 positions: top-right, top-left, bottom-right, bottom-left
- Custom colors and sizes
- Auto-formatting (99+ for large numbers)

```typescript
badge: {
  count: 5, // or text: "new"
  position: 'top-right',
  backgroundColor: '#F44336',
  textColor: '#FFFFFF',
  size: 20,
}
```

---

### 6. **Smooth Animations**

#### Built-in Effects
- **Press animation**: Scale down on touch
- **Haptic feedback**: Vibration on interactions
- **Snap animations**: Smooth edge snapping
- **5 interpolators**: Decelerate, accelerate, linear, bounce, overshoot

```typescript
animations: {
  enableScaleOnPress: true,
  pressScale: 0.9, // Shrink to 90% on press
  enableHapticFeedback: true,
  snapDuration: 300, // ms
  snapInterpolator: 'bounce', // Fun bouncy effect
}
```

---

### 7. **App State Monitoring**

Automatically show/hide widget based on app state.

**Features:**
- Auto-hide when app is open
- Custom check intervals
- Lifecycle events for app state changes
- Battery-optimized polling

```typescript
hideOnAppOpen: true,
appStateMonitoring: {
  enabled: true,
  checkInterval: 1000, // Check every second
}
```

---

### 8. **Persistent Foreground Service**

Widget runs as a foreground service with a notification.

**Features:**
- Custom notification title, text, icon
- Notification channels (Android O+)
- Auto-start on boot (optional)
- Survives app closure

```typescript
notification: {
  title: 'Widget Active',
  text: 'Tap to open app',
  channelId: 'widget_channel',
  channelName: 'Widget Service',
  icon: 'ic_notification',
},
autoStartOnBoot: true,
```

---

### 9. **Memory Optimized**

Built for low-memory environments (tested with 268MB heap).

**Optimizations:**
- Bitmap scaling with inSampleSize
- RGB_565 format (50% less memory)
- Memory checks before icon loading
- Automatic bitmap recycling
- Native gradient rendering (no bitmap allocation)

---

### 10. **Developer Experience**

#### Hot Reload Safe
- Works seamlessly with `yarn start`
- No crashes during development
- Events resume after hot reload

#### TypeScript Support
- Full type definitions
- IntelliSense autocomplete
- Type-safe configuration

#### Easy Integration
```typescript
import FloatingWidget from 'react-native-floating-app-widget';

// Initialize
await FloatingWidget.init({ /* config */ });

// Request permission
const hasPermission = await FloatingWidget.hasPermission();
if (!hasPermission) {
  await FloatingWidget.requestPermission();
}

// Start widget
await FloatingWidget.start();

// Update anytime
await FloatingWidget.updateConfig({ size: 80 });

// Stop widget
await FloatingWidget.stop();
```

---

## 📊 Real-World Use Cases

### 1. **Call Manager App**
```typescript
// Persistent call controls overlay
FloatingWidget.init({
  icon: callIconBase64,
  size: 70,
  badge: { count: 1, text: '00:45' }, // Call duration
  dismissZone: { enabled: true, showOn: 'longPress' },
  hideOnAppOpen: false, // Always visible
});
```

### 2. **Screen Recorder App**
```typescript
// Floating record button
FloatingWidget.init({
  icon: recordIconBase64,
  appearance: { backgroundColor: '#FF0000' },
  animations: { enableHapticFeedback: true },
  snapToEdge: true,
});

FloatingWidget.addEventListener('onWidgetClick', startRecording);
```

### 3. **Music Player**
```typescript
// Mini player controls
FloatingWidget.init({
  icon: albumArtBase64,
  shape: 'circle',
  badge: { text: '▶️', position: 'bottom-right' },
  hideOnAppOpen: true,
});
```

### 4. **Quick Notes**
```typescript
// Floating note pad
FloatingWidget.init({
  icon: noteIconBase64,
  badge: { count: notesCount },
  dismissZone: {
    enabled: true,
    gradientColors: ['#667eea', '#764ba2'],
    cornerRadius: 25,
  },
});
```

### 5. **Fitness Tracker**
```typescript
// Step counter overlay
FloatingWidget.init({
  icon: stepsIconBase64,
  badge: { text: '10k', position: 'top-right' },
  appStateMonitoring: { enabled: true },
  autoStartOnBoot: true,
});
```

---

## 🎨 Visual Customization Examples

### Modern Glass Effect
```typescript
dismissZone: {
  gradientColors: ['#FFFFFF33', '#FFFFFF1A'],
  activeGradientColors: ['#FF000055', '#FF000033'],
  cornerRadius: 25,
}
```

### Neon Glow
```typescript
dismissZone: {
  gradientColors: ['#00F5FF', '#00D9FF', '#00BDFF'],
  activeGradientColors: ['#FF0080', '#FF0040', '#FF0000'],
  gradientOrientation: 'horizontal',
  cornerRadius: 40,
}
```

### Material Design
```typescript
appearance: {
  backgroundColor: '#6200EE',
  borderColor: '#3700B3',
  borderWidth: 2,
  cornerRadius: 28,
  opacity: 0.95,
}
```

---

## 🔧 Technical Capabilities

### Platform Support
- ✅ React Native 0.60+
- ✅ Android 6.0+ (API 23+)
- ✅ TurboModules/New Architecture ready
- ✅ Kotlin 1.9.22
- ✅ JVM 17

### Performance
- 🚀 Native Android implementation
- 🚀 Zero JavaScript bridge overhead for animations
- 🚀 Optimized for low-memory devices
- 🚀 Battery-efficient app state monitoring

### Reliability
- ✅ Hot reload safe (development mode)
- ✅ Crash recovery (React context safety)
- ✅ Permission handling
- ✅ Service lifecycle management
- ✅ Configuration persistence

---

## 📦 What Makes This Library Unique?

1. **System-wide overlays** - Works across all apps, not just yours
2. **Production-ready** - Battle-tested memory optimizations
3. **Highly customizable** - 50+ configuration options
4. **Rich interactions** - Click, long press, drag, dismiss
5. **Gradient support** - Advanced dismiss zone styling
6. **Developer-friendly** - Hot reload support, TypeScript
7. **Event-driven** - 10+ JavaScript callbacks
8. **Low memory footprint** - Runs on 268MB heap devices
9. **Auto-start on boot** - True persistent experience
10. **Badge system** - Notification counts built-in

---

## 🎯 Manager Summary (30-Second Pitch)

**"A React Native library that creates persistent floating widgets (like Facebook Messenger chat heads) that stay on top of all apps. Developers can:**

- ✅ Create always-visible app shortcuts
- ✅ Build overlay controls for ongoing tasks (calls, recording, music)
- ✅ Add rich interactions (tap, long press, drag-to-dismiss)
- ✅ Customize with gradients, badges, animations
- ✅ Monitor app state and auto-hide/show
- ✅ Run on low-memory devices (optimized for 268MB heap)

**Perfect for productivity apps, media players, accessibility tools, and any app needing persistent UI access.**"

---

## 📚 Documentation

- **Installation & Setup**: See `README.md`
- **Dismiss Zone Styling**: See `DISMISS_ZONE_STYLING.md`
- **Development Mode**: See `DEV_MODE_FIXES.md`
- **Memory Optimizations**: See `MEMORY_OPTIMIZATION.md`
- **Migration Guide**: See `MIGRATION_GUIDE.md`
- **Usage Examples**: See `USAGE_EXAMPLES.md`

---

## 🚀 Quick Start

```bash
npm install react-native-floating-app-widget
# or
yarn add react-native-floating-app-widget
```

```typescript
import FloatingWidget from 'react-native-floating-app-widget';

// Minimal setup
await FloatingWidget.init({
  notification: {
    title: 'Widget Running',
    text: 'Tap to open app',
  },
});

// Request permission
if (!await FloatingWidget.hasPermission()) {
  await FloatingWidget.requestPermission();
}

// Start!
await FloatingWidget.start();
```

---

## 🔮 Future Enhancements (Roadmap Ideas)

- Multi-widget support (multiple widgets at once)
- Widget-to-widget interactions
- Gesture recognizers (swipe, pinch, rotate)
- iOS support (when Apple allows it)
- Widget templates/presets
- Analytics integration
- Accessibility features (TalkBack support)
