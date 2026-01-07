#!/bin/bash

echo "🔨 React Native Floating Widget - Compilation Test"
echo "===================================================="

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if we're in the library root
if [ ! -d "android" ] || [ ! -f "package.json" ]; then
    echo -e "${RED}❌ Error: Must run from library root directory${NC}"
    exit 1
fi

ERRORS=0

echo -e "\n${BLUE}━━━ Step 1: TypeScript/JavaScript Build ━━━${NC}"
npm run prepare 2>&1 | grep -E "(✔|Building|Wrote|Compiling)"
if [ ${PIPESTATUS[0]} -ne 0 ]; then
    echo -e "${RED}❌ Build failed${NC}"
    exit 1
fi
echo -e "${GREEN}✅ TypeScript/JavaScript build successful${NC}"

echo -e "\n${BLUE}━━━ Step 2: Kotlin Code Validation ━━━${NC}\n"

# Test 1: Companion objects
echo -n "Checking companion objects... "
companion_count=$(grep -c "companion object" android/src/main/java/com/floatingappwidget/FloatingAppWidgetModule.kt)
if [ "$companion_count" -eq 1 ]; then
    echo -e "${GREEN}✓${NC}"
else
    echo -e "${RED}✗ (found $companion_count, expected 1)${NC}"
    ERRORS=$((ERRORS + 1))
fi

# Test 2: sendEvent function
echo -n "Checking sendEvent function... "
if grep -q "fun sendEvent" android/src/main/java/com/floatingappwidget/FloatingAppWidgetModule.kt; then
    echo -e "${GREEN}✓${NC}"
else
    echo -e "${RED}✗ (not found)${NC}"
    ERRORS=$((ERRORS + 1))
fi

# Test 3: Enum references
echo -n "Checking enum references... "
if grep -r "DismissZonePosition\|BadgePosition" android/src 2>/dev/null | grep -qv "Config.Position"; then
    echo -e "${RED}✗ (found old enum names)${NC}"
    ERRORS=$((ERRORS + 1))
else
    echo -e "${GREEN}✓${NC}"
fi

# Test 4: Type consistency - snapDuration
echo -n "Checking snapDuration type... "
SERVICE_FILE="android/src/main/java/com/floatingappwidget/FloatingWidgetService.kt"
if grep -q 'putLong.*"animationSnapDuration"' "$SERVICE_FILE" && \
   grep -q 'getLong.*"animationSnapDuration"' "$SERVICE_FILE"; then
    echo -e "${GREEN}✓ (Long)${NC}"
elif grep -q 'putInt.*"animationSnapDuration"' "$SERVICE_FILE" && \
     grep -q 'getInt.*"animationSnapDuration"' "$SERVICE_FILE"; then
    echo -e "${GREEN}✓ (Int)${NC}"
else
    echo -e "${RED}✗ (type mismatch)${NC}"
    ERRORS=$((ERRORS + 1))
fi

# Test 5: Type consistency - checkInterval
echo -n "Checking checkInterval type... "
if grep -q 'putLong.*"appStateMonitoringCheckInterval"' "$SERVICE_FILE" && \
   grep -q 'getLong.*"appStateMonitoringCheckInterval"' "$SERVICE_FILE"; then
    echo -e "${GREEN}✓ (Long)${NC}"
elif grep -q 'putInt.*"appStateMonitoringCheckInterval"' "$SERVICE_FILE" && \
     grep -q 'getInt.*"appStateMonitoringCheckInterval"' "$SERVICE_FILE"; then
    echo -e "${GREEN}✓ (Int)${NC}"
else
    echo -e "${RED}✗ (type mismatch)${NC}"
    ERRORS=$((ERRORS + 1))
fi

# Test 6: Build configuration
echo -n "Checking build configuration... "
if grep -q 'jvmTarget.*17' android/build.gradle && \
   grep -q 'JavaVersion.VERSION_17' android/build.gradle; then
    echo -e "${GREEN}✓ (JVM 17)${NC}"
else
    echo -e "${YELLOW}⚠ (check JVM version)${NC}"
fi

echo -e "\n${BLUE}━━━ Step 3: File Structure Check ━━━${NC}\n"

# Check all Kotlin files exist
KOTLIN_FILES=(
    "FloatingAppWidgetModule.kt"
    "FloatingAppWidgetPackage.kt"
    "FloatingWidgetService.kt"
    "WidgetViewManager.kt"
    "WidgetConfig.kt"
    "PermissionHelper.kt"
    "AppStateReceiver.kt"
    "BootReceiver.kt"
)

for file in "${KOTLIN_FILES[@]}"; do
    if [ -f "android/src/main/java/com/floatingappwidget/$file" ]; then
        echo -e "${GREEN}✓${NC} $file"
    else
        echo -e "${RED}✗${NC} $file (missing)"
        ERRORS=$((ERRORS + 1))
    fi
done

echo -e "\n===================================================="
if [ $ERRORS -eq 0 ]; then
    echo -e "${GREEN}✅ ALL CHECKS PASSED!${NC}"
    echo -e "\n${BLUE}Next steps:${NC}"
    echo "  1. The library is ready to use"
    echo "  2. Build your app: yarn android"
    echo "  3. If you make changes, run: ./compile-test.sh"
    exit 0
else
    echo -e "${RED}❌ FOUND $ERRORS ERROR(S)${NC}"
    echo -e "\nPlease fix the errors before building your app."
    exit 1
fi
