# Library Compilation Test

This script validates the library's Kotlin and TypeScript code before building in your app.

## Usage

Run from the library root directory:

```bash
./compile-test.sh
```

## What it checks

### 1. TypeScript/JavaScript Build
- Compiles TypeScript to JavaScript
- Generates type definitions
- Creates CommonJS and ES modules

### 2. Kotlin Code Validation
- ✓ Single companion object in FloatingAppWidgetModule
- ✓ sendEvent function is defined
- ✓ Enum references use correct format (BadgeConfig.Position, DismissZoneConfig.Position)
- ✓ Type consistency (Long vs Int) in SharedPreferences
- ✓ JVM 17 configuration

### 3. File Structure
- Verifies all 8 Kotlin source files exist

## Exit Codes

- `0` - All checks passed ✅
- `1` - One or more checks failed ❌

## When to run

Run this script:
- ✅ After making changes to Kotlin code
- ✅ After making changes to TypeScript code
- ✅ Before committing changes
- ✅ Before building in your React Native app

## Example output

```
🔨 React Native Floating Widget - Compilation Test
====================================================

━━━ Step 1: TypeScript/JavaScript Build ━━━
✅ TypeScript/JavaScript build successful

━━━ Step 2: Kotlin Code Validation ━━━

Checking companion objects... ✓
Checking sendEvent function... ✓
Checking enum references... ✓
Checking snapDuration type... ✓ (Long)
Checking checkInterval type... ✓ (Long)
Checking build configuration... ✓ (JVM 17)

━━━ Step 3: File Structure Check ━━━

✓ FloatingAppWidgetModule.kt
✓ FloatingAppWidgetPackage.kt
✓ FloatingWidgetService.kt
... (all files)

====================================================
✅ ALL CHECKS PASSED!
```

## Troubleshooting

If checks fail:
1. Review the error messages
2. Fix the issues in the source files
3. Run `./compile-test.sh` again
4. Only build your app after all checks pass

## Note

This script does NOT run a full Android/Gradle compilation. It validates:
- TypeScript compilation
- Common Kotlin syntax issues that would cause compilation errors
- File structure integrity

For full compilation testing, build in your React Native app.
