#!/bin/zsh
#
# Pairs the Wear OS emulator with the phone emulator via adb port forwarding.
# Run this after starting both emulators.
#
# Usage:  ./pair-emulators.sh
#

ADB="$HOME/Library/Android/sdk/platform-tools/adb"

echo "🔍 Looking for emulators..."
DEVICES=$($ADB devices -l 2>&1)
echo "$DEVICES"
echo ""

# Find the phone and watch emulators
PHONE=$(echo "$DEVICES" | grep "sdk_gphone" | awk '{print $1}')
WATCH=$(echo "$DEVICES" | grep "sdk_gwear"  | awk '{print $1}')

if [[ -z "$PHONE" ]]; then
    echo "❌ No phone emulator found. Start it first."
    exit 1
fi
if [[ -z "$WATCH" ]]; then
    echo "❌ No Wear OS emulator found. Start it first."
    exit 1
fi

echo "📱 Phone: $PHONE"
echo "⌚ Watch: $WATCH"
echo ""

# Check if the Wear OS companion app is installed on the phone
WEAR_APP=$($ADB -s "$PHONE" shell pm list packages 2>&1 | grep "com.google.android.wearable.app")
if [[ -z "$WEAR_APP" ]]; then
    echo "⚠️  Wear OS companion app not found on phone emulator."
    echo "   The Wearable Data Layer API requires it."
    echo ""
    echo "   To install: open Play Store on the phone emulator and search for"
    echo "   'Wear OS by Google', or run:"
    echo "   $ADB -s $PHONE shell am start -a android.intent.action.VIEW -d 'market://details?id=com.google.android.wearable.app'"
    echo ""
    echo "   ⓘ  You must be signed into a Google account on the phone emulator first."
    echo ""
fi

# Forward the Wearable Data Layer port (5601)
echo "🔗 Setting up port forwarding (tcp:5601)..."
$ADB -s "$PHONE" forward tcp:5601 tcp:5601

if [[ -n "$WEAR_APP" ]]; then
    echo "✅ Done! The emulators can now communicate via the Wearable Data Layer."
else
    echo "🔗 Port forwarding is set up, but install the Wear OS app first (see above)."
fi
echo ""
echo "To verify, install both apps and check logcat:"
echo "  $ADB -s $WATCH logcat -s WearTaskDataLoader TaskDataListener PriorityTaskTile"
echo "  $ADB -s $PHONE logcat -s WearDataSender WearActionListener"
