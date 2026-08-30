# Document Saathi 📄

**Scan → Read → Understand** — An Android app for intelligent document digitization.

## Features
- 📸 Real-time document scanning via CameraX
- 🔤 Multilingual OCR (English + Hindi/Devanagari) using ML Kit
- 🤖 AI-powered summarization (DeepSeek API)
- 📝 Auto-detection of document types (Aadhaar, PAN, Passport, etc.)
- 🇮🇳 Output in English, Hindi, Gujarati
- 💾 Local Room DB persistence

## Tech Stack
- **Language:** Kotlin
- **Architecture:** Clean Architecture + MVVM
- **DI:** Hilt/Dagger
- **UI:** Jetpack Navigation, Material Design 3
- **Camera:** CameraX
- **OCR:** ML Kit Text Recognition
- **AI:** NVIDIA API (`google/diffusiongemma-26b-a4b-it`)
- **Database:** Room DB
- **Async:** Kotlin Coroutines + StateFlow

## Project Structure
com.rohan.documentsaathi/
├── core/ # Shared utilities
├── data/ # Database, API clients
├── domain/ # Use cases, repositories
├── feature/ # UI features (HomeFragment, OcrResultFragment, etc.)
└── di/ # Hilt dependency injection

## Local Setup

### Prerequisites
- Android Studio 2024.1+
- Android SDK 31+
- Kotlin 1.9+

### Installation
1. Clone the repo:
```bash
   git clone https://github.com/YourUsername/Document-Saathi.git
```

2. Add NVidia API key to `local.properties`:
   NVIDIA_API_KEY=your_key_here
   ⚠️ **Never commit `local.properties`** — it's in `.gitignore`

3. Sync Gradle & build:
```bash
   ./gradlew build
```

4. Run on emulator/device via Android Studio

## Architecture
- **Clean Architecture:** Separation of concerns across data, domain, UI layers
- **MVVM:** ViewModel + StateFlow for reactive UI updates
- **Single-Responsibility:** Each fragment handles one concern
- **Type Safety:** Kotlin's null-safety + data classes

## API Integration
Uses NVIDIA's OpenAI-compatible endpoint:
- Model: `google/diffusiongemma-26b-a4b-it`
- Timeout: 60s connect, 60s write, 90s read
- Supports structured JSON output

## Known Scope Decisions
- **Gujarati OCR:** Not supported by ML Kit; available as summarization output language only
- **Document Types:** Auto-detected via AI (Aadhaar, PAN, Passport, DL, Voter ID, etc.)
- **PDF Export:** Currently scaffolded (MediaStore integration in progress)

## For Viva / Submission
- Lab manual: See `/docs/Lab_Manual.docx`
- Design file: [Figma](https://www.figma.com/design/OrL1AL7hdPuQFKPfuAqdaR/Document_Saathi)

## Future Enhancements
- [ ] Tesseract OCR for Gujarati text recognition
- [ ] Offline summarization model
- [ ] Barcode/QR code scanning
- [ ] Document comparison

## Contributing
Pull requests welcome! Please follow Kotlin style guidelines and add tests for new features.

## License
MIT License — see LICENSE file

## Contact
Rohan | Atmiya University B.Tech Final Year Project
