# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

FaceFound is an offline face recognition Android app built on InsightFace (buffalo_l) models and ONNX Runtime Mobile. All inference runs on-device with no network dependency. Package: `com.Enco.facefound`.

## Build & Run

```bash
# Debug build
.\gradlew.bat assembleDebug

# Release build
.\gradlew.bat assembleRelease

# Install on connected device
.\gradlew.bat installDebug

# Output: app/build/outputs/apk/debug/app-debug.apk
```

**Prerequisites**: JDK 17, Android SDK (compileSdk 34), ONNX model files in `app/src/main/assets/` (det_10g.onnx ~16MB, w600k_r50.onnx ~166MB). Models are not in the repo — obtain from InsightFace buffalo_l package.

## Architecture

Single-module Android app using Kotlin + Jetpack Compose + Material 3.

### Core Pipeline (ml/OnnxFaceRecognition.kt)

1. **Detection** — SCRFD model (det_10g.onnx): 640x640 RGB input → bounding boxes + 5-point landmarks. Preprocessing: `(pixel - 127.5) / 128.0`, BGR order. Post-processing: confidence threshold → decode bbox/kps → NMS.
2. **Alignment** — 5-point least-squares similarity transform to ArcFace 112x112 template coordinates.
3. **Recognition** — ArcFace model (w600k_r50.onnx): 112x112 RGB input → 512-dim embedding, L2-normalized.
4. **Matching** — Cosine similarity (dot product since vectors are L2-normed). Default threshold 0.45.

### Key Modules

- `ml/OnnxFaceRecognition.kt` — ONNX model loading, detection, alignment, embedding extraction, recognition matching
- `video/VideoProcessor.kt` — Video frame extraction, batch face recognition, result annotation, H.264 encoding output
- `ui/viewmodel/FaceRecognitionViewModel.kt` — UI state management, orchestrates recognition/video flows, template management
- `util/NpzParser.kt` — NPZ/ZIP parsing, NPY header parsing, UTF-32 name decoding, embedding normalization
- `util/TemplateRepository.kt` — Template binary serialization, atomic write, index management
- `ui/screens/MainScreen.kt` — Main Compose UI with navigation drawer and settings
- `ui/screens/VideoScreen.kt` — Video recognition UI
- `ui/screens/AboutScreen.kt` — About page: app info, developers, dependencies, license, links

### Data Flow

Images/video → OnnxFaceRecognition (detect → align → embed) → match against templates (Map<String, FloatArray>) → results displayed in Compose UI. Templates stored as serialized binary files via TemplateRepository, imported from .npz files parsed by NpzParser.

## Key Technical Details

- Template .npz format: ZIP containing `names.npy` (dtype `<U9`, UTF-32LE) and `embeddings.npy` (dtype `<f4`, shape (N, 512))
- Models use BGR channel order with `(x - 127.5) / 128.0` normalization
- NDK ABI filters: arm64-v8a, armeabi-v7a, x86_64
- Requires `largeHeap=true` in manifest (models need ~300MB at runtime)
- Gradle repositories configured with Alibaba Cloud mirrors for China network

# Android Kotlin Project AI Code Specification
1. Add detailed Chinese comments to EVERY LINE of logical Kotlin & XML layout code
2. Clearly annotate variable usage, function purpose, parameter meaning, return value and exception boundaries
3. Add line-by-line comments for Activity lifecycle, page navigation, network requests, UI adaptation and permission logic
4. Avoid redundant meaningless comments, accurately explain code function, business logic and implementation reasons
5. Update and supplement line-by-line comments synchronously during diff code review
6. Follow Android Jetpack specifications, fully annotate ViewModel, Activity and Fragment processes line by line
7. Highlight risks and causes for exception capture, memory leakage and thread safety logic

