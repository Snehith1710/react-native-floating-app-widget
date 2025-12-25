# Getting Started with react-native-floating-app-widget

Welcome! This library provides a complete, production-ready implementation of floating widgets for React Native Android apps.

## What You Have

This is a **complete, ready-to-publish NPM library** with:

✅ Full Android implementation in Kotlin
✅ React Native bridge (Old & New Architecture compatible)
✅ TypeScript definitions and type safety
✅ Complete documentation and examples
✅ Play Store compliance built-in
✅ Production-grade code with error handling

## File Structure Overview

```
react-native-floating-app-widget/
├── 📱 Android Native (Kotlin)
│   ├── FloatingAppWidgetModule.kt    - React Native bridge
│   ├── FloatingWidgetService.kt      - Foreground service
│   ├── WidgetViewManager.kt          - Widget UI & interactions
│   ├── PermissionHelper.kt           - Permission management
│   ├── AppStateReceiver.kt           - App state detection
│   └── BootReceiver.kt               - Auto-start on boot
│
├── 📝 TypeScript/JavaScript
│   ├── src/index.ts                  - Main API
│   ├── src/types.ts                  - Type definitions
│   └── src/NativeFloatingAppWidget.ts - TurboModule spec
│
├── 📚 Documentation
│   ├── README.md                     - Main documentation
│   ├── QUICKSTART.md                 - 5-minute setup guide
│   ├── SETUP.md                      - Detailed setup
│   ├── EXAMPLE.md                    - Complete examples
│   ├── INTEGRATION_GUIDE.md          - Integration guide
│   ├── PROJECT_STRUCTURE.md          - Architecture overview
│   └── CHANGELOG.md                  - Version history
│
└── ⚙️ Configuration
    ├── package.json                  - NPM package config
    ├── tsconfig.json                 - TypeScript config
    ├── android/build.gradle          - Android build
    └── android/AndroidManifest.xml   - Permissions & services
```

## Quick Start for Developers

### Option 1: Publish to NPM

```bash
# 1. Update package.json with your details
# 2. Build the library
npm install
npm run prepare

# 3. Publish to NPM
npm publish
```

### Option 2: Test Locally

```bash
# 1. Build the library
npm install
npm run prepare

# 2. Link to a test app
npm link

# 3. In your test React Native app
npm link react-native-floating-app-widget
npx react-native run-android
```

### Option 3: Use in Monorepo

Just reference it from your `package.json`:

```json
{
  "dependencies": {
    "react-native-floating-app-widget": "file:../react-native-floating-app-widget"
  }
}
```

## Usage in Your App

See [QUICKSTART.md](QUICKSTART.md) for a complete minimal example.

### Basic Setup

```typescript
import FloatingAppWidget from 'react-native-floating-app-widget';

// 1. Initialize
await FloatingAppWidget.init({
  notification: {
    title: 'My App',
    text: 'Tap to return',
  },
});

// 2. Check/Request Permission
const hasPermission = await FloatingAppWidget.hasPermission();
if (!hasPermission) {
  await FloatingAppWidget.requestPermission();
}

// 3. Start Widget
await FloatingAppWidget.start();
```

## Key Features

### 1. System-Level Overlay
- Uses `SYSTEM_ALERT_WINDOW` permission
- Appears on top of all apps
- Click to open your app

### 2. Foreground Service
- Survives app kill
- Persistent notification (Play Store compliant)
- Configurable notification appearance

### 3. Smart Visibility
- Auto-shows when app is backgrounded
- Auto-hides when app is opened (configurable)
- Detects app state changes

### 4. Fully Customizable
- Custom icon (base64 images)
- Size, shape (circle/rounded)
- Position, draggable behavior
- Notification text and appearance

### 5. Optional Auto-Start
- Start on device boot
- Requires user permission
- Persists configuration

## Architecture Highlights

### Clean Separation of Concerns

**PermissionHelper**: Permission management only
**WidgetViewManager**: UI rendering and interaction
**FloatingWidgetService**: Service lifecycle and state
**AppStateReceiver**: App state detection
**BootReceiver**: Boot event handling

### React Native Bridge

**Old Architecture**: Classic NativeModules
**New Architecture**: TurboModules ready
**Auto-linking**: Zero manual configuration

### Configuration Persistence

Uses SharedPreferences to:
- Store widget configuration
- Persist across app restarts
- Enable boot receiver functionality

## Play Store Compliance

This library is designed to be Play Store safe:

✅ Explicit user opt-in (no auto-enable)
✅ Foreground service with notification
✅ Clear user benefit (quick app access)
✅ No ads or monetization
✅ User can disable anytime

**Important**: You must still:
- Explain why you need the permission
- Make it optional in your app
- Add to your privacy policy

See [INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md) for Play Store guidelines.

## Documentation Guide

Choose your path:

1. **Just want it working?** → [QUICKSTART.md](QUICKSTART.md)
2. **Need detailed setup?** → [SETUP.md](SETUP.md)
3. **Want examples?** → [EXAMPLE.md](EXAMPLE.md)
4. **Integrating into app?** → [INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md)
5. **API reference?** → [README.md](README.md)
6. **Understanding architecture?** → [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md)

## Development Workflow

### Making Changes

1. **TypeScript changes**: Edit files in `src/`
2. **Android changes**: Edit files in `android/src/main/java/`
3. **Rebuild**: `npm run prepare`
4. **Test**: In your test app, rebuild Android

### Testing

```bash
# Run TypeScript compiler
npm run typescript

# Run linter
npm run lint

# Test in a real app
npx react-native run-android
```

### Before Publishing

- [ ] Update version in `package.json`
- [ ] Update `CHANGELOG.md`
- [ ] Run `npm run prepare`
- [ ] Test in a fresh React Native app
- [ ] Update README if API changed

## System Requirements

**React Native**: 0.60+
**Android**: API 21+ (Android 5.0+)
**Target SDK**: 33+ recommended
**Kotlin**: 1.9.0
**Gradle**: 8.1.1+

## What's Included

### Core Components (8 Kotlin files)
- FloatingAppWidgetModule
- FloatingWidgetService
- WidgetViewManager
- WidgetConfig
- PermissionHelper
- AppStateReceiver
- BootReceiver
- FloatingAppWidgetPackage

### JavaScript/TypeScript (3 files)
- Main API wrapper
- Type definitions
- TurboModule interface

### Documentation (7 files)
- Complete API reference
- Setup guides
- Examples
- Troubleshooting

### Configuration (6 files)
- NPM package config
- TypeScript config
- Android build config
- Manifest
- ESLint & Prettier

## Next Steps

1. **Read [QUICKSTART.md](QUICKSTART.md)** - Get running in 5 minutes
2. **Customize** - See [README.md](README.md) for all options
3. **Test thoroughly** - Try all features
4. **Publish** - Share with the community!

## Support & Contribution

This is a complete, production-ready library. Feel free to:

- Use it in your projects
- Publish it to NPM
- Modify for your needs
- Contribute improvements

## License

MIT - See [LICENSE](LICENSE)

---

**Built with ❤️ for the React Native community**

Enjoy your floating widgets! 🎈
