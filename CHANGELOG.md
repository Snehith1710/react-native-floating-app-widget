# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2025-01-XX

### Added

- Initial release of react-native-floating-app-widget
- System-level floating widget overlay using `SYSTEM_ALERT_WINDOW`
- Foreground service with persistent notification
- Automatic visibility management (shows in background, hides in foreground)
- Fully customizable widget (icon, size, shape, position)
- Draggable widget with click-to-open functionality
- Widget survives app kill
- Optional auto-start on device boot
- React Native New Architecture (TurboModules) compatible
- TypeScript definitions included
- Comprehensive documentation and examples

### Features

#### Core Functionality
- `init(config)` - Initialize widget with configuration
- `start()` - Start the floating widget service
- `stop()` - Stop and remove the widget
- `update(config)` - Update widget configuration
- `hasPermission()` - Check overlay permission status
- `requestPermission()` - Request overlay permission

#### Widget Customization
- Custom icon support (base64 images)
- Configurable size (dp)
- Shape options: circle or rounded rectangle
- Draggable/non-draggable modes
- Initial position configuration
- Hide/show on app state changes

#### Service & Notification
- Foreground service implementation
- Customizable notification (title, text, icon, channel)
- Play Store compliant notification handling
- Android 13+ notification permission support

#### Lifecycle Management
- App state detection (foreground/background)
- Boot receiver for auto-start
- Clean shutdown and cleanup
- SharedPreferences persistence

### Platform Support
- Android API 21+ (Android 5.0 Lollipop)
- Android 14 (API 34) compatible
- No iOS support (Android-only library)

### Documentation
- Complete API reference
- Setup and integration guides
- Usage examples
- Troubleshooting guide
- Play Store compliance guide

## [Unreleased]

### Planned Features
- Widget click actions customization
- Multiple widget support
- Widget animations
- Custom widget layouts
- Notification action buttons customization
