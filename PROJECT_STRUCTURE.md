# Project Structure

This document outlines the complete structure of the `react-native-floating-app-widget` library.

## Directory Layout

```
react-native-floating-app-widget/
├── android/                          # Android native code
│   ├── src/main/
│   │   ├── AndroidManifest.xml      # Android manifest with permissions & components
│   │   └── java/com/floatingappwidget/
│   │       ├── FloatingAppWidgetModule.kt      # React Native bridge module
│   │       ├── FloatingAppWidgetPackage.kt     # React Native package
│   │       ├── FloatingWidgetService.kt        # Foreground service
│   │       ├── WidgetViewManager.kt            # Widget view management
│   │       ├── WidgetConfig.kt                 # Configuration data classes
│   │       ├── PermissionHelper.kt             # Permission utilities
│   │       ├── AppStateReceiver.kt             # App state detection
│   │       └── BootReceiver.kt                 # Boot event handler
│   └── build.gradle                  # Android build configuration
│
├── ios/                              # iOS placeholder (library is Android-only)
│   └── .gitkeep
│
├── src/                              # TypeScript/JavaScript source
│   ├── index.ts                      # Main export & JS wrapper
│   ├── types.ts                      # TypeScript type definitions
│   └── NativeFloatingAppWidget.ts   # TurboModule interface
│
├── lib/                              # Build output (generated)
│   ├── commonjs/                     # CommonJS build
│   ├── module/                       # ES Module build
│   └── typescript/                   # TypeScript declarations
│
├── docs/                             # Documentation
│   ├── README.md                     # Main documentation
│   ├── SETUP.md                      # Setup guide
│   ├── EXAMPLE.md                    # Usage examples
│   ├── INTEGRATION_GUIDE.md          # Integration guide
│   ├── CHANGELOG.md                  # Version history
│   └── PROJECT_STRUCTURE.md          # This file
│
├── package.json                      # NPM package configuration
├── tsconfig.json                     # TypeScript configuration
├── tsconfig.build.json              # TypeScript build configuration
├── react-native.config.js           # React Native CLI configuration
├── react-native-floating-app-widget.podspec  # iOS podspec (empty)
├── .gitignore                       # Git ignore rules
├── .npmignore                       # NPM publish ignore rules
├── .eslintrc.js                     # ESLint configuration
├── .prettierrc.js                   # Prettier configuration
└── LICENSE                          # MIT License
```

## Component Descriptions

### Android Native Components

#### FloatingAppWidgetModule.kt
- **Purpose**: Main React Native bridge module
- **Responsibilities**:
  - Exposes native methods to JavaScript
  - Manages widget lifecycle (init, start, stop, update)
  - Handles permission checks and requests
  - Stores configuration in SharedPreferences

#### FloatingWidgetService.kt
- **Purpose**: Android Foreground Service
- **Responsibilities**:
  - Runs as a foreground service with persistent notification
  - Manages widget visibility based on app state
  - Handles service lifecycle (onCreate, onStartCommand, onDestroy)
  - Creates and maintains notification channel

#### WidgetViewManager.kt
- **Purpose**: Widget view creation and interaction
- **Responsibilities**:
  - Creates and displays the floating overlay view
  - Handles drag and click interactions
  - Manages WindowManager and layout parameters
  - Opens app when widget is tapped

#### PermissionHelper.kt
- **Purpose**: Permission management utility
- **Responsibilities**:
  - Checks SYSTEM_ALERT_WINDOW permission status
  - Opens system settings for permission grant
  - Handles API level differences

#### WidgetConfig.kt
- **Purpose**: Configuration data structures
- **Responsibilities**:
  - Defines WidgetConfig and NotificationConfig data classes
  - Parses React Native ReadableMap to Kotlin objects
  - Handles default values and validation

#### AppStateReceiver.kt
- **Purpose**: App state monitoring
- **Responsibilities**:
  - Detects when app enters foreground/background
  - Broadcasts state changes to service
  - Handles screen on/off events

#### BootReceiver.kt
- **Purpose**: Device boot handling
- **Responsibilities**:
  - Receives BOOT_COMPLETED broadcast
  - Auto-starts widget if enabled
  - Reconstructs config from SharedPreferences

### TypeScript/JavaScript Components

#### src/index.ts
- **Purpose**: Main library export
- **Responsibilities**:
  - Wraps native module with high-level API
  - Provides error handling and validation
  - Manages initialization state
  - Exports singleton instance

#### src/types.ts
- **Purpose**: Type definitions
- **Responsibilities**:
  - Defines all TypeScript interfaces
  - Documents API types
  - Provides type safety for consumers

#### src/NativeFloatingAppWidget.ts
- **Purpose**: TurboModule interface
- **Responsibilities**:
  - Defines native module spec
  - Compatible with New Architecture
  - Fallback to legacy NativeModules

### Configuration Files

#### package.json
- NPM package metadata
- Dependencies and scripts
- Build configuration for react-native-builder-bob

#### android/build.gradle
- Android build configuration
- Kotlin version and dependencies
- Compilation settings

#### android/AndroidManifest.xml
- Required permissions
- Service and receiver declarations
- Foreground service type specification

#### tsconfig.json
- TypeScript compiler options
- Module resolution settings
- Include/exclude patterns

## Build Process

### TypeScript Build
```bash
npm run prepare
# or
yarn prepare
```

This uses `react-native-builder-bob` to:
1. Compile TypeScript to JavaScript (CommonJS)
2. Generate ES Modules
3. Generate TypeScript declarations

Output goes to `lib/` directory.

### Android Build

The Android code is built automatically by the host app when:
1. Library is installed via npm/yarn
2. Host app runs `npx react-native run-android`

The library's `build.gradle` is included in the host app's build process through auto-linking.

## Data Flow

### Initialization Flow
```
JavaScript (index.ts)
  ↓ init(config)
FloatingAppWidgetModule.kt
  ↓ parse config
WidgetConfig.kt
  ↓ store in SharedPreferences
✓ Ready to start
```

### Start Flow
```
JavaScript (index.ts)
  ↓ start()
FloatingAppWidgetModule.kt
  ↓ check permission
PermissionHelper.kt
  ↓ start service
FloatingWidgetService.kt
  ↓ monitor app state
AppStateReceiver.kt
  ↓ show/hide widget
WidgetViewManager.kt
  ↓ display overlay
WindowManager
```

### Widget Interaction Flow
```
User taps widget
  ↓
WidgetViewManager.kt (onTouch)
  ↓ detect click vs drag
  ↓ if click
openApp()
  ↓
Launch app activity
  ↓
App comes to foreground
  ↓
AppStateReceiver detects state change
  ↓
FloatingWidgetService hides widget
```

## Testing Strategy

### Unit Tests (To be implemented)
- Test config parsing
- Test permission checks
- Test state management

### Integration Tests
- Test service lifecycle
- Test widget visibility
- Test app state detection

### Manual Testing
1. Permission flow
2. Widget appearance/disappearance
3. Drag functionality
4. Click to open
5. Boot receiver
6. Notification display
7. Service persistence

## Development Workflow

### Local Development
1. Clone repository
2. Install dependencies: `npm install`
3. Link to test app: `npm link`
4. In test app: `npm link react-native-floating-app-widget`
5. Build and run: `npx react-native run-android`

### Making Changes
1. Modify TypeScript code in `src/`
2. Modify Kotlin code in `android/src/main/java/`
3. Rebuild: `npm run prepare`
4. Test in host app

### Publishing
1. Update version in `package.json`
2. Update `CHANGELOG.md`
3. Build: `npm run prepare`
4. Publish: `npm publish`

## Dependencies

### Runtime Dependencies
- `react-native`: Peer dependency
- `react`: Peer dependency

### Development Dependencies
- `typescript`: TypeScript compiler
- `react-native-builder-bob`: Build tool
- `@react-native-community/eslint-config`: Linting
- `eslint`: Code quality
- `prettier`: Code formatting

### Android Dependencies
- Kotlin stdlib
- Android SDK (API 21+)
- React Native Android

## Key Design Decisions

### Why Foreground Service?
- Required for overlay to survive app kill
- Play Store compliant
- User-visible notification ensures transparency

### Why SharedPreferences for Config?
- Persists across app restarts
- Simple key-value storage
- No need for complex database
- Accessible from both module and service

### Why Singleton Pattern in JS?
- Simplifies API usage
- Prevents multiple instances
- Maintains single source of truth for state

### Why Base64 for Icons?
- Easy to pass from JS to native
- No need for file storage
- Works with any image source

### Why Auto-linking?
- Modern React Native standard
- Zero manual configuration
- Better developer experience

## Future Improvements

### Potential Features
- [ ] Multiple widget support
- [ ] Custom widget layouts (React Native views)
- [ ] Widget animations
- [ ] Gesture customization
- [ ] Widget groups/tabs
- [ ] Analytics integration
- [ ] A/B testing support

### Technical Debt
- [ ] Add unit tests
- [ ] Add integration tests
- [ ] Improve bitmap persistence
- [ ] Add widget preview in settings
- [ ] Optimize memory usage
- [ ] Add widget size presets

## Contributing

See the main README.md for contribution guidelines.

## License

MIT - See LICENSE file for details.
