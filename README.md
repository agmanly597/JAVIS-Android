# JAVIS — Personal AI Assistant for Android

> "Hello. Javis online. How can I help you?"

JAVIS is a full-featured Android AI assistant built with Kotlin and Jetpack Compose. It works as a system-level companion, accessible from anywhere on your device without needing to open the app.

---

## Features

| Feature | Details |
|---|---|
| 🎙️ Voice Input | Android SpeechRecognizer with error recovery |
| 🔊 Voice Output | Android TTS + cloud voice support |
| 🧠 AI Brain | Groq (Llama 3) + DeepSeek with automatic fallback |
| 📱 App Control | Open/search 20+ apps by voice |
| 📞 Calls | Find contacts and initiate calls by name |
| 💬 WhatsApp | Draft and confirm messages |
| ⏰ Alarms/Timers | Set alarms and timers by voice |
| 🔔 Notifications | Read and summarize your notifications |
| 🧩 Memory | Remembers your name, habits, and preferences (Room DB) |
| 📋 Quick Tile | Activate JAVIS from Quick Settings |
| 🔧 Accessibility | Navigate apps hands-free |
| 📌 Foreground Service | Always-on background assistant |
| 🌑 Dark Theme | Futuristic dark UI optimized for low-end devices |

---

## Quick Start

### 1. Clone this repo
```bash
git clone https://github.com/agmanly597/JAVIS-Android.git
cd JAVIS-Android
```

### 2. Open in Android Studio
- Open Android Studio → File → Open → select this folder
- Wait for Gradle sync to complete

### 3. Add your API key
In Android Studio:
- Go to **Settings screen** in the app after installation  
- Enter your **Groq API key** (free at [console.groq.com](https://console.groq.com))
- Optionally add a **DeepSeek API key** as fallback

Or set them in `local.properties`:
```properties
GROQ_API_KEY=your_key_here
DEEPSEEK_API_KEY=your_key_here
```

### 4. Build and Run
- Connect your Android device (API 26+)
- Click ▶ Run in Android Studio
- Grant all requested permissions

---

## Getting APKs from GitHub Actions

Every push to `main` automatically builds an APK and creates a GitHub Release.

1. Go to your repo → **Actions** → click the latest build
2. Scroll down to **Artifacts** → download `JAVIS-debug-*.zip`
3. Extract the APK and transfer to your Android device
4. Install it (enable Unknown Sources first)

### For signed Release APKs
Add these GitHub repository secrets (Settings → Secrets and variables → Actions):

| Secret | Value |
|---|---|
| `GROQ_API_KEY` | Your Groq API key |
| `DEEPSEEK_API_KEY` | Your DeepSeek API key (optional) |
| `KEYSTORE_BASE64` | `base64 -w0 your-keystore.jks` |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias |
| `KEY_PASSWORD` | Key password |

---

## Activating JAVIS

| Method | How |
|---|---|
| Quick Settings | Pull down notification shade → tap **JAVIS** tile |
| Notification | Tap **Listen** in the persistent notification |
| In-app | Open app → Voice tab → tap microphone |
| Auto-start | Enabled by default on boot |

---

## Voice Commands

```
"Open YouTube"
"Open WhatsApp and message Musa I'll be there at 5"
"Search YouTube for Kano news"
"Call John"
"Set alarm for 6 AM"
"Set timer for 20 minutes"
"Read my notifications"
"What's my name?"
"Search Google for today's weather"
"Open Settings"
```

---

## Permissions Required

| Permission | Reason |
|---|---|
| Microphone | Voice input |
| Contacts | Call/message by name |
| Phone | Initiate calls |
| Notifications | Post JAVIS notification |
| Notification Access | Read app notifications |
| Accessibility | Navigate apps by voice |
| Overlay | Floating button (optional) |

---

## Project Structure

```
app/src/main/java/com/javis/ai/
├── ai/               # AI providers (Groq, DeepSeek)
├── voice/            # STT and TTS managers
├── memory/           # Room database + MemoryManager
├── services/         # Foreground, Notification, Accessibility services
├── tiles/            # Quick Settings tile
├── apps/             # App launcher
├── calls/            # Call manager
├── notifications/    # Notification store
├── domain/           # Intent analyzer, Task planner, Agent router
├── ui/               # Jetpack Compose screens + theme
├── settings/         # DataStore settings manager
└── di/               # Hilt dependency injection
```

---

## Optimizations for Low-End Devices (Redmi A1)

- Minimal background RAM — foreground service uses <20MB
- AI calls only when needed (local actions execute instantly)
- Room DB with coroutines — no main thread blocking
- Compose lazy loading for all lists
- TTS initialized once, reused across sessions
- ProGuard/R8 minification on release builds

---

## Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **DI:** Hilt
- **DB:** Room + DataStore
- **Network:** Retrofit + OkHttp
- **AI:** Groq API (Llama 3 8B), DeepSeek
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 34 (Android 14)

---

## License

MIT — feel free to fork and extend.
