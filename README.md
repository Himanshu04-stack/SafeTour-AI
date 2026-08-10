# 🛡️ SafeTour AI

https://appetize.io/app/b_no3dxvdyhhovqarnssc6w76hri

### An Intelligent Location-Based Safety and Navigation Application for Android

[![Android](https://img.shields.io/badge/Platform-Android%2013%2B-brightgreen?logo=android)](https://developer.android.com)
[![JAVA](https://img.shields.io/badge/Language-JAVA-purple?logo=JAVA)](https://javalang.org)
[![OSMDroid](https://img.shields.io/badge/Maps-OSMDroid-blue)](https://github.com/osmdroid/osmdroid)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-orange?logo=firebase)](https://firebase.google.com)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)
[![B.Tech Project](https://img.shields.io/badge/B.Tech-Final%20Year%20Project-red)](https://www.srmist.edu.in)

*B.Tech Final Year Project — SRM University Delhi NCR, 2025–26*

## 📖 Overview

SafeTour AI is a modern, open-source Android application built for personal safety and intelligent travel navigation. Unlike conventional apps that rely on Google Maps, SafeTour AI is fully self-contained — using **OSMDroid** for maps, **OSRM** for routing, **Firebase Firestore** for real-time SOS, and **Nominatim + Wikipedia GeoSearch** for contextual POI discovery.

Built on Android 13+ (API Level 33) with a clean **MVVM + LiveData** architecture, the app targets solo travellers, tourists in unfamiliar cities, and anyone who needs a reliable, privacy-respecting safety companion.

## ✨ Features

### 🗺️ Real-Time Navigation
- Turn-by-turn navigation powered by **OSRM** (no Google Maps dependency)
- Route polyline rendered natively on OSMDroid canvas
- Compass-anchored heading-up map rotation via IMU sensor
- Navigation state machine: `IDLE → SEARCH → NAVIGATE → ARRIVED`

### 🆘 SOS Emergency System
- 3-second hold trigger accessible from any app state
- GPS snapshot captured instantly with SecureRandom tracking ID
- **Sub-3-second** Firestore write latency
- Parallel dispatch: SMS to emergency contacts + push notification to responders
- Live location shared until user manually cancels alert

### 📍 POI Discovery
- Nominatim reverse geocoding with ~1 km radius query
- 500ms debounce to prevent API spam
- Wikipedia GeoSearch enrichment (title + summary for each POI)
- OSMDroid marker pins with Material Design 3 chip UI

### 🌙 Dark Mode Pipeline
- Custom ColorMatrix pipeline: invert → hue rotate 180° → brightness scale
- Deep-black map background with neon-teal (`#00E5FF`) roads
- No third-party dark map tile dependency

```kotlin
val matrix = floatArrayOf(
    -1f,  0f,  0f,  0f, 255f,
     0f, -1f,  0f,  0f, 255f,
     0f,  0f, -1f,  0f, 255f,
     0f,  0f,  0f,  1f,   0f
)
```
## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────┐
│              Presentation Layer                     │
│   MapFragment · ExploreFragment · SOSActivity       │
│   TrackingService (Background GPS)                  │
└──────────────────────┬──────────────────────────────┘
                       │ observe / postValue
┌──────────────────────▼──────────────────────────────┐
│            ViewModel / LiveData Layer               │
│   SharedLocationVM · NavigationVM · SOSVM           │
│   ExploreVM  (single source of truth)               │
└──────────────────────┬──────────────────────────────┘
                       │ API calls / DB ops
┌──────────────────────▼──────────────────────────────┐
│              Data & Service Layer                   │
│   OSRM Engine · OSMDroid Tiles · Firebase Firestore │
│   Nominatim API · Wikipedia GeoSearch API           │
└─────────────────────────────────────────────────────┘
```

## 🛠️ Tech Stack

| Component          | Technology                                |
|--------------------|-------------------------------------------|
| Platform           | Android 13+ (API Level 33)                |
| Language           | Kotlin                                    |
| Architecture       | MVVM + LiveData + ViewModel               |
| Map Engine         | OSMDroid (open-source)                    |
| Routing            | OSRM / OpenRouteService                   |
| Real-time Backend  | Firebase Firestore                        |
| Geocoding          | Nominatim                                 |
| POI Enrichment     | Wikipedia GeoSearch API                   |
| UI Framework       | Material Design 3                         |
| Background Service | Android Foreground Service (START_STICKY) |

---

## 📁 Project Structure

```
SafeTourAI/
├── app/
│   ├── src/main/
│   │   ├── java/com/safetour/
│   │   │   ├── ui/
│   │   │   │   ├── MapFragment.kt
│   │   │   │   ├── ExploreFragment.kt
│   │   │   │   └── SOSActivity.kt
│   │   │   ├── viewmodel/
│   │   │   │   ├── SharedLocationViewModel.kt
│   │   │   │   ├── NavigationViewModel.kt
│   │   │   │   └── SOSViewModel.kt
│   │   │   ├── service/
│   │   │   │   └── TrackingService.kt
│   │   │   ├── data/
│   │   │   │   ├── OsrmRepository.kt
│   │   │   │   └── FirestoreRepository.kt
│   │   │   └── utils/
│   │   │       ├── DarkModeHelper.kt
│   │   │       └── LocationUtils.kt
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   ├── values/
│   │   │   └── drawable/
│   │   └── AndroidManifest.xml
├── gradle/
├── build.gradle
└── README.md
```

---

## ⚙️ Setup & Installation

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17+
- Android device or emulator running API 33+
- Firebase project (for SOS functionality)

### Steps

**1. Clone the repository**
```bash
git clone https://github.com/Himanshu04-stack/SafeTour-AI.git
cd SafeTour-AI
```

**2. Open in Android Studio**
```
File → Open → Select the cloned folder
```

**3. Configure Firebase**
- Create a project at [console.firebase.google.com](https://console.firebase.google.com)
- Download `google-services.json`
- Place it in the `app/` directory
- Enable **Firestore Database** in your Firebase console

**4. Add required permissions to `AndroidManifest.xml`**
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.INTERNET" />
```

**5. Build and run**
```
Build → Make Project → Run on device/emulator
```
## 🧪 Testing

| Layer             | Framework            | Count |
|-------------------|----------------------|-------|
| Unit Tests        | JUnit4 + Mockito     | 47    |
| Integration Tests | Fragment + ViewModel | 22    |   
| End-to-End Tests  | Espresso UI          | 8     |   
| **Total**         |                      |**77** |

**Key test scenarios covered:**
- Permission grant/deny handling
- SOS Firestore write under 3 seconds
- Route loss graceful degradation
- START_STICKY background service restart
- Off-route auto step advance

Run all tests:
```bash
./gradlew test                  # Unit tests
./gradlew connectedAndroidTest  # Instrumentation tests
```

## 📊 Performance Benchmarks

| Metric | Result | Target |
|---|---|---|
| Route computation (50 km) | 800ms | ≤ 1000ms ✅ |
| SOS Firestore write | < 3s | < 3s ✅ |
| Map tile load (cached) | 400ms | ≤ 500ms ✅ |
| POI Nominatim query | 550ms | ≤ 600ms ✅ |
| Battery (8hr continuous GPS) | ~34% drain | — |

*Benchmarked on Qualcomm Snapdragon 680, Android 13*


## 🗺️ UI/UX Design System

**Color Tokens (Material Design 3)**

| Token | Value | Usage |
|---|---|---|
| Primary | `#1A3C6E` | Navigation, headers |
| Secondary | `#2E7D8A` | Accents, chips |
| SOS Red | `#E53935` | Emergency elements |
| Dark BG | `#121212` | Dark mode base |
| Neon Teal | `#00E5FF` | Dark mode roads |

**Typography:** 57sp Display → 28sp Headline → 14sp Body → 11sp Label

**Touch targets:** Minimum 48×48dp (WCAG AA compliant)


## 🔭 Future Scope

| Phase   | Feature                                |
|---------|----------------------------------------|
| Phase 1 | Voice-guided turn-by-turn navigation   |
| Phase 1 | Offline map tile download & caching    |
| Phase 2 | TensorFlow Lite safety area classifier |
| Phase 2 | Multi-peer SOS mesh network            |
| Phase 2 | Traffic-aware route optimisation       |
| Phase 3 | Smartwatch / wearable integration      |
| Phase 3 | AI-powered area risk scoring           |
| Phase 3 | Cross-platform iOS port                |


## 📚 References

1. Jain & Choudhury (2019) — *LBS Architecture Survey*, IEEE Transactions on Mobile Computing
2. Kumar et al. — *MVVM + LiveData Decoupling for Android*, ACM MobiSys
3. Gupta & Malhotra (2022) — *Geotagged Data and Urban Safety*, Springer
4. [OSMDroid Documentation](https://github.com/osmdroid/osmdroid/wiki)
5. [OSRM Project](http://project-osrm.org)
6. [Firebase Firestore Docs](https://firebase.google.com/docs/firestore)
7. [Nominatim API](https://nominatim.org/release-docs/develop/api/Overview/)


## 👨‍💻 Author

**Himanshu**
B.Tech — Computer Science & Engineering
SRM Institute of Science and Technology
Academic Year 2025–26


## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
