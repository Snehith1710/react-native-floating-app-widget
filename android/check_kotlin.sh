#!/bin/bash
echo "🔍 Kotlin Syntax Validation"
echo "================================"

# Check for duplicate companion objects
echo -e "\n1️⃣  Checking for duplicate companion objects..."
for file in android/src/main/java/com/floatingappwidget/*.kt; do
    count=$(grep -c "companion object" "$file" 2>/dev/null || echo 0)
    filename=$(basename "$file")
    if [ "$count" -gt 1 ]; then
        echo "   ❌ $filename has $count companion objects (should be 1)"
    elif [ "$count" -eq 1 ]; then
        echo "   ✅ $filename"
    fi
done

# Check for incorrect enum references
echo -e "\n2️⃣  Checking enum references..."
bad_refs=$(grep -rn "\.DismissZonePosition\|\.BadgePosition" android/src/main/java/com/floatingappwidget/ 2>/dev/null | grep -v "\.Position\." | wc -l)
if [ "$bad_refs" -gt 0 ]; then
    echo "   ❌ Found incorrect enum references"
    grep -rn "\.DismissZonePosition\|\.BadgePosition" android/src/main/java/com/floatingappwidget/ | grep -v "\.Position\."
else
    echo "   ✅ All enum references use correct format"
fi

# Check Long vs Int consistency
echo -e "\n3️⃣  Checking type consistency..."
echo "   Checking snapDuration..."
put_long_snap=$(grep -c "putLong.*snapDuration" android/src/main/java/com/floatingappwidget/FloatingWidgetService.kt)
get_long_snap=$(grep -c "getLong.*snapDuration" android/src/main/java/com/floatingappwidget/FloatingWidgetService.kt)
if [ "$put_long_snap" -eq "$get_long_snap" ]; then
    echo "   ✅ snapDuration type consistent"
else
    echo "   ❌ snapDuration type mismatch (put:$put_long_snap, get:$get_long_snap)"
fi

echo "   Checking checkInterval..."
put_long_interval=$(grep -c "putLong.*checkInterval" android/src/main/java/com/floatingappwidget/FloatingWidgetService.kt)
get_long_interval=$(grep -c "getLong.*checkInterval" android/src/main/java/com/floatingappwidget/FloatingWidgetService.kt)
if [ "$put_long_interval" -eq "$get_long_interval" ]; then
    echo "   ✅ checkInterval type consistent"
else
    echo "   ❌ checkInterval type mismatch (put:$put_long_interval, get:$get_long_interval)"
fi

# Check sendEvent usage
echo -e "\n4️⃣  Checking sendEvent references..."
sendEvent_defined=$(grep -c "fun sendEvent" android/src/main/java/com/floatingappwidget/FloatingAppWidgetModule.kt)
if [ "$sendEvent_defined" -eq 1 ]; then
    echo "   ✅ sendEvent function defined in FloatingAppWidgetModule"
else
    echo "   ❌ sendEvent function not found or duplicated"
fi

echo -e "\n================================"
echo "✨ Validation complete!"
