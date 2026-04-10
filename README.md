# SecureSnap Vault

A secure photo and video vault for Android with PIN and biometric authentication. Keep your private media protected with military-grade security in a clean, modern interface.

## Features

- **PIN Protection** — 4-digit PIN with SHA-256 hashing secures your vault
- **Biometric Unlock** — Fingerprint/face authentication for quick access (PRO)
- **Photo & Video Storage** — Import and securely store photos and videos
- **Zoomable Image Viewer** — Pinch-to-zoom with pan support (1x–5x)
- **Video Playback** — Built-in ExoPlayer-powered video player
- **Batch Operations** — Multi-select, batch delete (PRO)
- **Media Filtering** — Filter vault by photos or videos (PRO)
- **Secure Sharing** — Share media directly from the vault via FileProvider (PRO)
- **Dark Mode** — Full Material 3 dynamic theming with system dark mode support
- **Material You** — Dynamic color support on Android 12+
- **Accessibility** — Content descriptions and semantics for screen readers
- **Haptic Feedback** — Tactile feedback on PIN entry, selections, and destructive actions

## Requirements

- Android 7.0 (API 24) or higher
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Gradle 8.x

## Build Instructions

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd "SecureSnap Vault"
   ```

2. Open the project in Android Studio.

3. Sync Gradle and build:
   ```bash
   ./gradlew assembleDebug
   ```

4. Run on a device or emulator from Android Studio, or install the APK:
   ```bash
   ./gradlew installDebug
   ```

## Project Structure

```
app/src/main/java/com/factory/securesnapvault/
├── MainActivity.kt                  # Entry point activity
├── SecureSnapApp.kt                 # Application class
├── billing/
│   ├── BillingManager.kt            # Google Play Billing integration
│   └── PremiumManager.kt            # Premium state management
├── data/
│   ├── db/
│   │   ├── MediaDao.kt              # Room DAO for media queries
│   │   └── VaultDatabase.kt         # Room database definition
│   ├── model/
│   │   └── MediaItem.kt             # Media entity model
│   └── repository/
│       └── MediaRepository.kt       # Data access layer
├── ui/
│   ├── navigation/
│   │   └── NavGraph.kt              # Navigation routes & graph
│   ├── screens/
│   │   ├── auth/
│   │   │   ├── AuthScreen.kt        # PIN/biometric auth UI
│   │   │   └── AuthViewModel.kt     # Auth state management
│   │   ├── paywall/
│   │   │   └── PaywallScreen.kt     # Premium subscription UI
│   │   ├── settings/
│   │   │   ├── SettingsScreen.kt    # Settings UI
│   │   │   └── SettingsViewModel.kt # Settings state management
│   │   ├── vault/
│   │   │   ├── VaultScreen.kt       # Main media grid UI
│   │   │   └── VaultViewModel.kt    # Vault state & logic
│   │   └── viewer/
│   │       └── MediaViewerScreen.kt # Image/video viewer
│   └── theme/
│       ├── Color.kt                 # Color definitions
│       ├── Theme.kt                 # Material 3 theme setup
│       └── Type.kt                  # Typography definitions
└── util/
    ├── FileManager.kt               # File import/export, thumbnails
    └── PreferencesManager.kt        # Encrypted preferences (PIN, settings)
```

## Architecture

- **UI**: Jetpack Compose with Material 3
- **Architecture Pattern**: MVVM with ViewModels
- **Database**: Room (media metadata)
- **Preferences**: DataStore with encryption
- **Navigation**: Jetpack Navigation Compose
- **Image Loading**: Coil
- **Video Playback**: Media3 ExoPlayer
- **Billing**: Google Play Billing Library 6.x

## Premium Tiers

| Feature             | Free  | PRO   |
|---------------------|-------|-------|
| Photo storage       | 10    | Unlimited |
| Video support       | —     | Yes   |
| Biometric unlock    | —     | Yes   |
| Secure sharing      | —     | Yes   |
| Filters & batch ops | —     | Yes   |

PRO is available as weekly, monthly, yearly subscriptions or a one-time lifetime purchase.
