# AGENTS.md

## Project

FaceFound — offline face recognition Android app. Single-module Kotlin + Jetpack Compose + Material 3. Package: `com.Enco.facefound`. All inference on-device via ONNX Runtime, no network dependency.

## Build Commands

```bash
.\gradlew.bat assembleDebug          # Debug APK
.\gradlew.bat assembleRelease        # Release APK (unsigned)
.\gradlew.bat installDebug           # Build + install on connected device
python build_tool.py                 # Build with structured logging to build_logs/
python build_tool.py -r              # Release build via build_tool
python build_tool.py -r --clean      # Clean + release
python build_tool.py -r --bundle     # Release AAB
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

**No lint, typecheck, or test runner scripts exist.** Run `.\gradlew.bat lint` or `.\gradlew.bat test` directly. `build_tool.py` also supports `--extra-tasks lint test` to run these alongside a build.

## Prerequisites

- **JDK 17** — required, won't build with 8 or 11
- **Android SDK** — compileSdk 34, minSdk 24, targetSdk 34
- **ONNX models** — NOT in repo (`.onnx` and `.npz` in `.gitignore`). Must be placed manually in `app/src/main/assets/`:
  - `det_10g.onnx` (~16MB) — SCRFD face detection
  - `w600k_r50.onnx` (~166MB) — ArcFace recognition
  - Obtain from InsightFace `buffalo_l` package
- Build will fail without models — the app loads them at startup from assets

## Architecture

Single `:app` module. Source root: `app/src/main/java/com/Enco/facefound/`

| Path | Role |
|------|------|
| `ml/OnnxFaceRecognition.kt` | Core pipeline: detect → align → embed → match. Loads both ONNX models. |
| `video/VideoProcessor.kt` | Batch video frame processing, H.264 output encoding |
| `ui/viewmodel/FaceRecognitionViewModel.kt` | State management, orchestrates all flows |
| `ui/screens/MainScreen.kt` | Main Compose UI, navigation drawer, settings |
| `ui/screens/VideoScreen.kt` | Video recognition UI |
| `ui/screens/AboutScreen.kt` | About page: app info, developers, dependencies, license, links |
| `util/NpzParser.kt` | NPZ/ZIP template import, NPY parsing, UTF-32LE name decoding |
| `util/TemplateRepository.kt` | Binary template serialization, atomic writes |
| `FaceRecognitionApp.kt` | Application class |
| `MainActivity.kt` | Single Activity, Compose entry point |

### Pipeline Details

- Detection: SCRFD model, 640x640 RGB input, BGR channel order, `(pixel - 127.5) / 128.0` normalization
- Alignment: 5-point similarity transform to ArcFace 112x112 template
- Recognition: ArcFace model, 112x112 RGB input → 512-dim L2-normalized embedding
- Matching: cosine similarity (dot product on L2-normed vectors), default threshold 0.30
- Parallel: multi-face async with `Semaphore(2)` for ONNX inference concurrency

## Gotchas

- **Gradle repos use Alibaba Cloud mirrors** (in `settings.gradle.kts`) for China network. If builds hang on dependency download, check mirror availability.
- **`largeHeap=true`** in manifest — models need ~300MB runtime memory. Low-memory devices may OOM.
- **NDK ABI filters**: arm64-v8a, armeabi-v7a, x86_64 only.
- **Template .npz format**: ZIP with `names.npy` (dtype `<U9`, UTF-32LE) and `embeddings.npy` (dtype `<f4`, shape (N, 512)). Names are NOT plain UTF-8 — NpzParser handles the UTF-32 decoding.
- **BGR channel order** for both models, not RGB. This is InsightFace convention.
- **`GRADLE_USER_HOME=.gradle`** set in `gradle.properties` — Gradle cache is project-local, not in user home.
- **`build_tool.py`** is a convenience wrapper (gitignored) that runs Gradle with colored output and logs to `build_logs/`. Use it for cleaner build feedback.
- **No CI pipeline** — no GitHub Actions, no pre-commit hooks configured.
- **Release build has `isMinifyEnabled = false`** — no ProGuard shrinking by default.

## Code Style

Per CLAUDE.md requirements:
- **Line-by-line Chinese comments** on all logical Kotlin and XML code
- Annotate variable usage, function purpose, parameter meaning, return values, exception boundaries
- Highlight risks for exception handling, memory leaks, thread safety
- Follow Android Jetpack conventions with full ViewModel/Activity/Fragment annotation
