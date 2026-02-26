# 🛰️ StressVision — Android Studio Setup Guide

## ⚡ Quick Start (3 Steps)

---

### STEP 1: Extract & Open in Android Studio

1. **Extract** the `StressVision.zip` file
2. Open **Android Studio** (Arctic Fox or newer)
3. Click **"Open"** (NOT "New Project")
4. Navigate to the extracted `StressVision` folder
5. Click **OK** — let Gradle sync finish (2–3 minutes)

---

### STEP 2: Get Your Free Google Maps API Key

> ⚠️ The app needs a Maps API key to show satellite imagery.

1. Go to: **https://console.cloud.google.com/**
2. Create a new project (or select existing)
3. Go to **APIs & Services → Library**
4. Search and **Enable: "Maps SDK for Android"**
5. Go to **APIs & Services → Credentials**
6. Click **"+ Create Credentials" → API Key**
7. Copy your API key

---

### STEP 3: Add API Key to App

1. Open file: `app/src/main/AndroidManifest.xml`
2. Find this line:
   ```xml
   android:value="YOUR_GOOGLE_MAPS_API_KEY_HERE"
   ```
3. Replace `YOUR_GOOGLE_MAPS_API_KEY_HERE` with your actual key:
   ```xml
   android:value="AIzaSyABC123...your_key_here"
   ```
4. **Save** the file

---

### STEP 4: Run the App

1. Connect an Android device (USB debugging ON) **OR** start an emulator
2. Click the **▶ Run** button (green triangle) in Android Studio
3. Select your device
4. App installs and launches!

---

## 📱 Using the App

| Action | What Happens |
|--------|-------------|
| App opens | Splash screen → Satellite map of farmland near Nagpur |
| Tap **"Enable Stress Vision"** FAB | Colored polygons appear over the map |
| Green zone | Healthy crop (NDVI: 0.65–0.72) |
| Yellow zone | Moderate stress (NDVI: 0.48–0.52) |
| Red zone | Severe stress (NDVI: 0.31, temp 38°C) |
| Tap any colored zone | Opens detailed zone info screen |
| Tap FAB again | Removes overlay |

---

## 🗂️ Project Files Explained

```
StressVision/
├── app/src/main/
│   ├── java/com/orbital/stressvision/
│   │   ├── MainActivity.java         ← Main map screen
│   │   ├── SplashActivity.java       ← Launch screen
│   │   ├── ZoneDetailActivity.java   ← Zone info popup
│   │   ├── StressCalculator.java     ← Rule-based AI logic
│   │   ├── MapUtils.java             ← Map + polygon drawing
│   │   ├── StressZone.java           ← Data model
│   │   └── StressResult.java        ← Stress level enum
│   ├── res/
│   │   ├── layout/                   ← XML screen layouts
│   │   ├── values/                   ← Colors, strings, themes
│   │   ├── drawable/                 ← Icons, shapes
│   │   └── menu/                     ← Toolbar menus
│   └── AndroidManifest.xml          ← ← ADD API KEY HERE
├── app/build.gradle                  ← Dependencies
└── settings.gradle                   ← Project config
```

---

## 🔬 Stress Detection Logic (No AI)

```
StressCalculator.java:

If NDVI < 0.4  AND  Temp > 35°C  →  🔴 SEVERE STRESS
If NDVI < 0.6                    →  🟡 MODERATE STRESS  
Otherwise                        →  🟢 HEALTHY
```

### 5 Demo Zones

| Zone | Location | NDVI | Temp | Classification |
|------|----------|------|------|----------------|
| A    | North    | 0.72 | 28°C | ✅ Healthy |
| B    | East     | 0.48 | 31°C | 🟡 Moderate Stress |
| C    | South    | 0.31 | 38°C | 🔴 Severe Stress |
| D    | West     | 0.65 | 27°C | ✅ Healthy |
| E    | Central  | 0.52 | 33°C | 🟡 Moderate Stress |

---

## ❓ Troubleshooting

**Map shows grey tiles:**
→ API key is missing or incorrect. Re-check Step 3.

**"Google Play Services not available" error:**
→ Use a physical Android device, or an emulator with Google APIs

**Build fails with "cannot find symbol":**
→ Clean project: Build → Clean Project, then Build → Rebuild Project

**Gradle sync fails:**
→ Ensure you have internet connection for first sync (downloads dependencies)

---

## 🏆 Hackathon Demo Tips

1. Pre-open the app on your phone before the demo
2. Have the map zoomed in on Zone C (red — severe stress) for visual impact
3. Talk about the **pre-visual detection** concept while showing the overlay
4. Tap Zone C to show the detailed analysis popup
5. Toggle the overlay off/on to dramatically show the difference

---

*Built for SDG 2 — Zero Hunger | No AI/ML used — Pure rule-based logic*
