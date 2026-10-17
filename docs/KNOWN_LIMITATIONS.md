# BeatPad — Known Limitations & Future Architecture Roadmap

This document outlines the known technical boundaries, design trade-offs, and future architectural enhancements for the BeatPad Android launchpad application.

---

## 1. Audio Engine Backend: SoundPool vs C++ Oboe / AAudio

### Current Implementation:
- BeatPad uses Android's native `SoundPool` framework behind a clean `AudioEngine` abstraction.
- Decoded 16-bit 44.1kHz PCM samples are preloaded into uncompressed native audio memory buffers during pack initialization.
- Achieves sub-25ms touch-to-sound trigger latency on modern Android devices.

### Limitation:
- `SoundPool` is subject to Android OS audio mixer variations across different OEM devices. On certain budget or legacy Android devices lacking AAudio fast-track hardware routing, mixer buffer latency may range from 30ms to 50ms.

### Roadmap Solution:
- The `AudioEngine` interface is completely decoupled from UI code. A C++ NDK engine leveraging Google's **Oboe library** (which selects AAudio on Android 8.0+ and OpenSL ES fallback) can be dropped in seamlessly to achieve sub-10ms ultra-low latency on specialized low-latency hardware.

---

## 2. Tempo Modification & Pitch Preservation (Time-Stretching)

### Current Implementation:
- BeatPad allows tempo adjustments within ±20 BPM of each pack's base tempo.
- Playback rate adjustments are performed via `SoundPool.setRate(streamId, rate)` clamped to `[0.5f, 2.0f]`.

### Limitation:
- Because native `SoundPool` adjusts sample rate without phase vocoding or granular resynthesis, tempo adjustments will slightly shift sample pitch (raising tempo raises pitch; lowering tempo lowers pitch).

### Roadmap Solution:
- Integrating a lightweight real-time time-stretching DSP library (such as SoundTouch, Rubber Band, or DIRAC) into a C++ audio processing pipeline will allow independent tempo adjustment while preserving the musical root key and pitch profile.

---

## 3. Storage & Downloadable Sound Pack Architecture

### Current Implementation:
- 8 genre sound packs and 6 interactive lessons are bundled locally in the APK assets directory (`assets/packs/` and `assets/lessons/`), ensuring 100% offline functionality with zero external network dependencies.
- Custom mic recordings are stored locally in application internal storage (`filesDir/custom_samples/`).

### Limitation:
- Cloud-based sound pack streaming and dynamic in-app pack downloads are intentionally excluded to keep the application 100% offline-first, private, and free of third-party network SDKs.

### Roadmap Solution:
- The JSON pack manifest parser (`PackManifest.kt`) and `PackRepository` already support reading packs from external file paths. A cloud repository module (e.g. Amazon S3, Cloudflare R2, or Firebase Storage) can ingest ZIP archives and extract them directly into internal storage without altering pad trigger logic.

---

## 4. Microphone Beatbox Sampler Hardware Variations

### Current Implementation:
- Records mono 16-bit 44.1kHz PCM via `AudioRecord`, applies silence trimming, peak normalization, and allows interactive start/end slice trimming.

### Limitation:
- Microphone hardware frequency response and background noise cancellation algorithms vary substantially across different Android smartphone manufacturers.

### Roadmap Solution:
- An optional software noise gate and parametric equalizer (low-cut filter to eliminate room rumble) can be integrated into the post-recording DSP pipeline.
