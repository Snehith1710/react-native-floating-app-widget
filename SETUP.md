# Setup Guide

This guide covers the complete setup process for integrating `react-native-floating-app-widget` into your React Native application.

## Installation

### 1. Install the package

```bash
npm install react-native-floating-app-widget
# or
yarn add react-native-floating-app-widget
```

### 2. Rebuild your Android app

```bash
npx react-native run-android
```

That's it! The library uses auto-linking, so no manual linking is required.

## Android Configuration

### Permissions

The library automatically adds all required permissions to your app's manifest through manifest merging. No manual configuration needed.

The following permissions are added:

```xml
<!-- Required -->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Optional - only if using autoStartOnBoot -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

### Proguard (Release Builds)

If you're using Proguard for code obfuscation, add these rules to `android/app/proguard-rules.pro`:

```proguard
# FloatingAppWidget
-keep class com.floatingappwidget.** { *; }
-keepclassmembers class com.floatingappwidget.** { *; }
```

### Android 13+ Notification Permission

For Android 13 (API 33) and above, you need to request notification permission at runtime:

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

## Common Issues

### Build Errors

#### "Manifest merger failed"

This usually happens if there are conflicts with permissions or services. Check your app's `AndroidManifest.xml` for duplicate entries.

**Solution:**
1. Clean the build: `cd android && ./gradlew clean`
2. Rebuild: `cd .. && npx react-native run-android`

#### "Could not find com.floatingappwidget"

This means the library wasn't linked properly.

**Solution:**
1. Check that the library is in your `package.json` dependencies
2. Delete `node_modules` and reinstall: `rm -rf node_modules && npm install`
3. Clear Metro cache: `npx react-native start --reset-cache`

### Runtime Errors

#### "Native module not found"

The native module isn't being loaded.

**Solution:**
1. Ensure you've rebuilt the app after installing: `npx react-native run-android`
2. Check that the package is in `node_modules/react-native-floating-app-widget`
3. Verify auto-linking: `npx react-native config`

#### "Permission denied" when starting widget

The `SYSTEM_ALERT_WINDOW` permission wasn't granted.

**Solution:**
```typescript
const hasPermission = await FloatingAppWidget.hasPermission();
if (!hasPermission) {
  await FloatingAppWidget.requestPermission();
}
```

## Next Steps

1. See [README.md](README.md) for API documentation
2. Check out the [EXAMPLE.md](EXAMPLE.md) for complete examples
3. Review Play Store guidelines before publishing

## Support

If you encounter issues:
1. Check the [Troubleshooting](README.md#troubleshooting) section
2. Review existing [GitHub issues](https://github.com/yourusername/react-native-floating-app-widget/issues)
3. Open a new issue with:
   - React Native version
   - Android version
   - Full error message
   - Steps to reproduce
