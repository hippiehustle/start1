# Kaos EV Crypto Scanner - Android App

A production-ready Android application built with Jetpack Compose that integrates with the Kaos EV Crypto Scanner backend running on Fly.io.

## Features

### Core Features
- **Real-time Crypto Scanning**: View the latest EV (Expected Value) scan results with trade signals (BUY, WAIT, NO TRADE)
- **Manual Scan Trigger**: Run on-demand scans with a single tap
- **Push Notifications**: Receive FCM notifications for high-quality trade setups
- **Background Auto-Refresh**: Configurable periodic updates (15/30/60/120 minutes)
- **Dark Mode Support**: Material 3 with dynamic color support (Android 12+)

### 5+ Home Screen Widgets
All widgets are resizable and support long-press quick configuration:

1. **Minimal Badge** - State + Coin (compact display)
2. **Compact Card** - State + Readiness + Last Updated
3. **Full Plan Snippet** - First 6 lines of scan output
4. **Targets/Stops View** - Entry, Stop, TP1, TP2 prices
5. **Matrix** - 2x3 grid showing state, coin, readiness, BTC regime, timestamp

### Architecture
- **Clean Architecture**: Separation of concerns (data/domain/ui)
- **Jetpack Compose**: Modern declarative UI
- **Material 3 Design**: Latest design system with dynamic theming
- **Dependency Injection**: Hilt for DI
- **Networking**: Retrofit + OkHttp with auth interceptor
- **State Management**: Kotlin Flow + ViewModel
- **Local Storage**: DataStore for settings, Room for history
- **Background Work**: WorkManager for periodic scans
- **Crash Safety**: Graceful error handling with cached fallbacks

---

## Setup Instructions

### Prerequisites
1. **Android Studio**: Hedgehog (2023.1.1) or later
2. **JDK**: Version 17 or later
3. **Backend**: Your Fly.io backend must be running
4. **Firebase Project**: For push notifications

---

### Step 1: Clone and Open Project
```bash
cd android
# Open in Android Studio
```

### Step 2: Configure Firebase

#### 2.1 Create Firebase Project
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project (or use existing)
3. Add an Android app with package name: `com.kaos.evcryptoscanner`

#### 2.2 Download google-services.json
1. Download `google-services.json` from Firebase Console
2. Replace the placeholder file at:
   ```
   android/app/google-services.json
   ```

#### 2.3 Enable Firebase Cloud Messaging
1. In Firebase Console, go to **Build → Cloud Messaging**
2. Enable Cloud Messaging API
3. Note your **Server Key** for backend configuration

### Step 3: Configure Backend Connection

#### Option A: Via App Settings (Recommended)
1. Build and install the app
2. Open the app → Go to **Settings**
3. Enter your **Backend URL**:
   ```
   https://your-backend-name.fly.dev
   ```
4. Enter your **API Key** (must match backend `API_KEY` env var)
5. Tap **Save** then **Test Connection**

#### Option B: Default Configuration (Optional)
Edit `PreferencesManager.kt` to set defaults:
```kotlin
val BASE_URL = stringPreferencesKey("base_url")
// Modify the default value in baseUrl flow
val baseUrl: Flow<String> = dataStore.data.map { preferences ->
    preferences[BASE_URL] ?: "https://your-backend.fly.dev"
}
```

### Step 4: Build and Run

#### Debug Build
```bash
./gradlew assembleDebug
# Install via Android Studio or:
adb install app/build/outputs/apk/debug/app-debug.apk
```

#### Release Build (Signed)
1. Create a keystore:
   ```bash
   keytool -genkey -v -keystore kaos-release.keystore \
     -alias kaos -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Create `keystore.properties` in project root:
   ```properties
   storePassword=YOUR_STORE_PASSWORD
   keyPassword=YOUR_KEY_PASSWORD
   keyAlias=kaos
   storeFile=../kaos-release.keystore
   ```

3. Update `app/build.gradle.kts`:
   ```kotlin
   android {
       signingConfigs {
           create("release") {
               storeFile = file(keystoreProperties["storeFile"]!!)
               storePassword = keystoreProperties["storePassword"] as String
               keyAlias = keystoreProperties["keyAlias"] as String
               keyPassword = keystoreProperties["keyPassword"] as String
           }
       }
       buildTypes {
           release {
               signingConfig = signingConfigs.getByName("release")
               // ... existing config
           }
       }
   }
   ```

4. Build release:
   ```bash
   ./gradlew assembleRelease
   # Output: app/build/outputs/apk/release/app-release.apk
   ```

---

## How to Use

### Dashboard
- **Big State Badge**: Shows BUY / SETUP FORMING — WAIT / NO TRADE
- **Scan Details**: Coin, readiness score, formatted output
- **Actions**:
  - **Refresh**: Pull-to-refresh or tap button
  - **Run Scan Now**: Manually trigger backend scan
  - **Copy Plan**: Copy output to clipboard
  - **Share Plan**: Share via system share sheet

### Settings
- **Backend URL**: Enter your Fly.io backend URL
- **API Key**: Secure input (hidden by default)
- **Auto Refresh**: Choose interval (off/15/30/60/120 min)
- **Notifications**: Enable/disable push notifications
- **Smart Notifications**: Only notify on BUY or readiness ≥70%
- **Test Connection**: Verify backend connectivity

### Widgets
1. **Adding a Widget**:
   - Long-press home screen → **Widgets**
   - Find **Kaos EV Scanner**
   - Drag desired widget to home screen
   - Configure template, theme, density
   - Tap **Save**

2. **Quick Edit** (Long-press widget):
   - Choose template style
   - Select theme (light/dark/system)
   - Adjust content density
   - Toggle auto-refresh
   - Show/hide readiness & timestamps

### Notifications

#### Setup
1. Go to **Settings** → Enable **Notifications**
2. Grant notification permission (Android 13+)
3. App registers FCM token with backend
4. Backend sends push for:
   - **BUY** signals (Trade Alerts channel)
   - **Readiness ≥70%** (Setup Alerts channel)

#### Channels
- **Trade Alerts**: High priority (BUY signals)
- **Setup Alerts**: Default priority (high-readiness setups)

#### Test Notification
- Tap **Test Notification** in Settings to verify
- Notification tap opens Dashboard

---

## Backend Integration

### Endpoints Used

#### Public (No Auth)
- `GET /health` - Backend health check
- `GET /scan/latest` - Fetch latest scan result

#### Protected (Requires X-API-KEY header)
- `POST /scan/run` - Trigger manual scan
- `POST /device/register` - Register FCM token
  ```json
  { "token": "<fcm-token>" }
  ```
- `POST /device/unregister` - Unregister FCM token
  ```json
  { "token": "<fcm-token>" }
  ```

### Backend Requirements
Your backend must:
1. Return JSON responses matching the schema
2. Accept `X-API-KEY` header for protected endpoints
3. Support FCM token registration/unregistration
4. Send FCM notifications with payload:
   ```json
   {
     "notification": {
       "title": "Trade Alert",
       "body": "BUY signal detected"
     },
     "data": {
       "state": "BUY",
       "readiness": "85"
     }
   }
   ```

---

## Troubleshooting

### No Data Showing
- **Check Backend URL**: Settings → Test Connection
- **Verify API Key**: Must match backend `API_KEY` env var
- **Backend Down**: Check Fly.io dashboard

### Notifications Not Working
- **Permission**: Settings → Enable Notifications (grant permission)
- **FCM Token**: Check logs for "FCM Token: ..." message
- **Backend Registration**: Ensure backend received token via `/device/register`
- **Firebase Config**: Verify `google-services.json` is correct

### Widgets Not Updating
- **Cache**: Widgets load from cached data
- **Auto Refresh**: Check Settings → Auto Refresh Interval
- **Manual Refresh**: Open app → Dashboard → Refresh

### Build Errors
- **Missing google-services.json**: Replace placeholder with real file
- **Gradle Sync**: File → Sync Project with Gradle Files
- **Clean Build**: Build → Clean Project → Rebuild Project

---

## Project Structure

```
android/
├── app/
│   ├── src/main/
│   │   ├── java/com/kaos/evcryptoscanner/
│   │   │   ├── data/
│   │   │   │   ├── api/          # Retrofit services, DTOs
│   │   │   │   ├── local/        # DataStore, Room
│   │   │   │   ├── repository/   # Data repositories
│   │   │   │   ├── fcm/          # FCM service
│   │   │   │   └── worker/       # WorkManager tasks
│   │   │   ├── domain/
│   │   │   │   ├── model/        # Business models
│   │   │   │   └── usecase/      # Use cases
│   │   │   ├── ui/
│   │   │   │   ├── screen/       # Compose screens
│   │   │   │   ├── viewmodel/    # ViewModels
│   │   │   │   ├── widget/       # Glance widgets
│   │   │   │   ├── navigation/   # NavGraph
│   │   │   │   └── theme/        # Material 3 theme
│   │   │   ├── di/               # Hilt modules
│   │   │   ├── MainActivity.kt
│   │   │   └── KaosApplication.kt
│   │   ├── res/
│   │   │   ├── drawable/         # Icons, shapes
│   │   │   ├── layout/           # XML layouts (minimal)
│   │   │   ├── values/           # Strings, colors, themes
│   │   │   └── xml/              # Widget info, backup rules
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── google-services.json      # ⚠️ Replace with your file
├── build.gradle.kts
├── settings.gradle.kts
└── README.md (this file)
```

---

## Technologies

| Category | Library/Tool |
|----------|--------------|
| UI | Jetpack Compose, Material 3 |
| Widgets | Glance |
| DI | Hilt |
| Networking | Retrofit, OkHttp |
| Storage | DataStore Preferences, Room |
| Background | WorkManager |
| Notifications | Firebase Cloud Messaging |
| Logging | Timber |
| Architecture | MVVM + Clean Architecture |

---

## Security Notes

### API Key Storage
- API Key stored in DataStore (not encrypted by default)
- For production, consider using `EncryptedSharedPreferences` or Android Keystore
- Current implementation: **WARNING displayed in logs**

### Network Security
- App uses HTTPS only (no cleartext traffic)
- Certificate pinning not implemented (consider for production)

### ProGuard/R8
- Enabled in release builds
- Keep rules configured for Retrofit, Gson, Glance

---

## Known Limitations

1. **Widget Refresh**: Widgets update via cached data + WorkManager (not real-time)
2. **History**: Room database prepared but not fully implemented (single latest scan cached)
3. **Offline Mode**: Shows last cached result with banner
4. **FCM**: Requires Google Play Services (won't work on AOSP/custom ROMs without GMS)

---

## Future Enhancements

- [ ] Scan history list with Room database
- [ ] Charts/graphs for readiness trends
- [ ] Multiple backend profiles
- [ ] Export scan results to CSV
- [ ] Biometric lock for settings
- [ ] Wear OS companion app
- [ ] Encrypted API key storage (Keystore)

---

## License

This project is part of the Kaos EV Crypto Scanner system. See main repository for license.

---

## Support

For issues or questions:
1. Check backend logs on Fly.io
2. Check Android logcat: `adb logcat -s KaosApp`
3. Verify API endpoints match backend implementation
4. Test connection via Settings → Test Connection

**Happy Trading! 🚀**
