# AI Field Medic

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

An on-device Android app that guides users through emergency medical triage using local LLMs — no internet required. Built on top of [Google AI Edge Gallery](https://github.com/google-ai-edge/gallery).

The app captures user context (vitals, medical history), runs a voice-driven triage loop with on-device speech recognition and TTS, and uses a vision model to describe wounds from camera input. All inference runs locally for privacy and offline use.

## Team

| Name | Email |
| --- | --- |
| Emma Shen | emmaxshen@berkeley.edu |
| Jesslyn Gunadi | jesslyngunadi@berkeley.edu |
| Aaron Nguyen | airrock85@berkeley.edu |
| Tyler Sales | tylerdsales@berkeley.edu |

## Setup

### Prerequisites

- **Android Studio** (Ladybug or newer)
- **JDK 17**
- **Android SDK 34+** with an Android 12+ device or emulator
- A **Hugging Face account** (required to download models inside the app)
- A **Hugging Face OAuth application** ([create one here](https://huggingface.co/docs/hub/oauth#creating-an-oauth-app))

### Steps

1. **Clone the repo**
   ```bash
   git clone https://github.com/jesslyng33/ai-field-medic.git
   cd ai-field-medic
   ```

2. **Open in Android Studio**

   Open the `Android/src` directory as the project root. Let Gradle sync and download dependencies.

3. **Configure Hugging Face OAuth**

   In `Android/src/app/src/main/java/com/google/ai/edge/gallery/common/ProjectConfig.kt`, replace the placeholders:
   ```kotlin
   const val clientId = "<your-hf-client-id>"
   const val redirectUri = "<your-hf-redirect-uri>"
   ```

   In `Android/src/app/build.gradle.kts`, update the redirect scheme to match:
   ```kotlin
   manifestPlaceholders["appAuthRedirectScheme"] = "<your-scheme>"
   ```

4. **Build**
   ```bash
   cd Android/src
   ./gradlew assembleDebug
   ```
   Or use **Run ▸ Run 'app'** in Android Studio.

## Run & Usage

1. **Install** the debug APK on a physical device (recommended; emulator works but is slower for inference) or run directly from Android Studio.

2. **First launch — onboarding**: sign in with Hugging Face and complete the trip/profile setup (location, group size, medical history, vitals).

3. **Download a model**: from the home screen, pick a model (e.g. Gemma family) and download it. Models are cached on-device.

4. **Start a triage session**: tap the triage tile and speak naturally. The app uses voice activity detection to know when you're done talking, then responds with on-device TTS guidance.

5. **Wound describer**: point the camera at an injury — the vision model returns a structured description that feeds into the triage loop.

6. **Review**: see the assessment summary and recommended next steps.

All inference is fully on-device. No data leaves the phone.

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE).
