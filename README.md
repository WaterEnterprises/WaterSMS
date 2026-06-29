# WaterSMS 🌊📱

WaterSMS is a premium, high-performance Android application designed for intelligent, compliant bulk SMS dispatching. Empowered by **Google Gemini AI**, WaterSMS optimizes carrier delivery success rates by generating unique semantic variations of your outreach messages, completely bypassing automated anti-spam signature filters.

The application combines beautiful modern UI design, fluid mechanics, and robust engineering to offer a powerful SMS broadcasting platform right from your mobile device.

---

## 🎨 Design Aesthetics & Visual Identity

WaterSMS features a meticulously crafted **Premium Dark theme** with rich glass-morphic elements designed for elite usability:
*   **Cosmic Slate Palette**: Styled using deep, custom dark blue backgrounds (`#0F172A`), rich purple gradients (`#1E1B4B`), and clean emerald accents to prioritize readability and elegance under any lighting conditions.
*   **Glass-morphic Cards & Surface Elevations**: Containers leverage translucent surface elevations with thin white borders to create visual depth and a premium "glass" aesthetic.
*   **Dynamic Pulse & Progress Telemetry**: Send actions trigger rich, real-time visual telemetry, dynamic pulse animations, and interactive SVG progress loops.

---

## 🚀 Key Features

### 1. 🧠 Anti-Spam Gemini AI Paraphraser
*   **20-Phrasing Rotation**: Generates 20 grammatically and semantically distinct variations of a base campaign message.
*   **Spam Filter Evasion**: Cycles through variations randomly during bulk dispatch to prevent telecom carriers from flagging identical message templates as automated spam.
*   **Fallback Reliability**: Seamlessly falls back to structured template replication if API connectivity is offline.

### 2. ⚡ Compliance & Intelligent Dispatching
*   **Randomized Delay Intervals**: Allows configuring minimum and maximum delay limits (e.g., 5-15s) between successive messages. Emulating natural human behavior minimizes delivery failure rates.
*   **Live Campaign Controls**: Full pause, resume, and cancellation mechanics for ongoing background queues.

### 3. 📂 Multi-Channel Contact Importing
*   **Device Address Book Sync**: Fluidly search and multi-select recipients straight from your phone's contact storage.
*   **Manual CSV & Text Paste**: Copy-paste plain lists of names and numbers (e.g., `Name, Phone`) or raw phone numbers with intelligent auto-formatting.

### 4. 📊 Robust Telemetry & Analytics Dashboard
*   **SQLite-Powered Room Database**: Securely persists all campaign profiles, status states, detailed recipient items, and error logs locally on the device.
*   **Aggregate Summary**: View total campaigns, ongoing sends, and successful delivery ratios.
*   **Donut Analytics Canvas**: Custom Jetpack Compose graphics canvas displaying success versus failure delivery ratios.

---

## 🛠️ Architecture & Tech Stack

WaterSMS is built using state-of-the-art Android architectures and developer practices:
*   **Kotlin & Coroutines/Flow**: Powering lightning-fast asynchronous operations, background database queries, and structured concurrency.
*   **Jetpack Compose & Material 3**: Beautiful, hardware-accelerated declarative UI with adaptive viewport designs.
*   **Room Database**: High-performance local SQLite abstraction layer for reliable offline-first data integrity.
*   **Google Gemini Pro REST Integration**: Intelligent, fast API communication managed dynamically via BuildConfig and secured inside the AI Studio Secrets Panel.
*   **Moshi JSON Serialization**: Fast, type-safe serialization of rich structures, including JSON variant lists stored in database rows.

---

## 🔒 Security & Privacy

WaterSMS respects your security and keeps you in full control:
*   **No Cloud Storage**: All campaigns and recipient phone numbers are persisted strictly on-device in a local private SQLite database.
*   **Explicit System Authorization**: Requests standard system permissions (`SEND_SMS`, `READ_CONTACTS`, `POST_NOTIFICATIONS`) gracefully with visual onboarding guides.
*   **Secure API Handling**: Your Gemini API key is securely injected through Gradle build configurations—never hardcoded in source code or resources.
