# Jarvish Gesture — Hand Gesture Control (Android 11)

Bina touch kiye phone control: hand gesture se scroll, play/pause, aur swipe.

## Kaise kaam karta hai
- **Front camera** continuously chalu rehta hai (foreground service)
- **MediaPipe Hand Landmarker** haath ke 21 points detect karta hai
- **GestureClassifier** un points se gesture pehchanta hai:
  - ✋ Open palm → Play/Pause
  - 👆 Hand up move → Scroll up
  - 👇 Hand down move → Scroll down
  - 👈 Hand left move → Swipe left / back
  - 👉 Hand right move → Swipe right / next
  - ☝️ Point (sirf index finger) → Tap (screen center)
- **AccessibilityService** scroll/tap actually perform karta hai
- Play/Pause system-wide media key event ke through kaam karta hai (YouTube, Spotify sab pe chalega)

## Files jo tujhe "Jarvish-" repo mein upload karni hain

Yeh poora folder structure waise hi banao GitHub mobile web pe (har file "Create new file" se, full path daal ke):

```
build.gradle
settings.gradle
gradle.properties
app/build.gradle
app/src/main/AndroidManifest.xml
app/src/main/res/layout/activity_main.xml
app/src/main/res/values/strings.xml
app/src/main/res/values/styles.xml
app/src/main/res/xml/accessibility_service_config.xml
app/src/main/java/com/jarvish/gesture/MainActivity.kt
app/src/main/java/com/jarvish/gesture/CameraGestureService.kt
app/src/main/java/com/jarvish/gesture/GestureAccessibilityService.kt
app/src/main/java/com/jarvish/gesture/GestureClassifier.kt
.github/workflows/build.yml
```

**Note:** MediaPipe ka model file (`hand_landmarker.task`) khud download nahi karna — workflow (`build.yml`) usse automatically Google ke server se download kar lega jab build chalega. Isliye tujhe wo file upload nahi karni.

## Build kaise hoga
1. Sab files upload karne ke baad, repo ke **Actions** tab mein jao
2. "Build Jarvish Gesture APK" workflow apne aap chalu ho jayega (push pe) — ya "Run workflow" pe manually bhi chala sakta hai
3. Build complete hone pe, us run ke andar **Artifacts** section mein `jarvish-gesture-debug-apk` milega — wahi download karke phone mein install karna

## Phone pe install karne ke baad 3 steps
1. App khol ke **Camera Permission** do
2. **Accessibility Service** manually ON karo (Settings > Accessibility > Jarvish Gesture) — app button se seedha settings khul jayegi
3. "Gesture Tracking Start Karo" dabao — camera background mein chalu ho jayega, notification dikhega

## Dhyan rakhne wali baatein
- Camera continuously chalu rehta hai isliye **battery drain zyada** hoga
- Achi lighting mein hi accuracy best rahegi
- Yeh Android 11 (API 30) ke liye configured hai — minSdk aur targetSdk dono 30
