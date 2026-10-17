# BeatPad — Android Launchpad & Beat-Maker

**BeatPad** is a complete, production-grade, offline-first Android beat-making and launchpad application built exclusively for Android phones. Inspired by the launchpad music creation genre, BeatPad features 100% original branding, procedurally synthesized royalty-free audio, and a responsive neon DJ interface.

> **Legal Guardrail Notice**: BeatPad is an original work replicating only functional and UX concepts of launchpads. Branding is controlled via a single constant (`AppConfig.APP_NAME = "BeatPad"`). All 98 audio samples are procedurally synthesized via DSP algorithms under the Creative Commons Zero (CC0 1.0 Universal) Public Domain license. No proprietary assets or trademarks from third parties are used. See [`docs/LICENSES.md`](docs/LICENSES.md).

---

## Architecture & Tech Stack

```
com.beatpad.app
 ├── audio          # AudioEngine, SoundPoolAudioEngine, MasterClock, Quantizer, AudioRenderer, StepTimingUtils
 ├── data
 │    ├── local     # BeatPadDatabase, TrackDao, TrackEntity
 │    ├── model     # SoundPack, PadInfo, InstrumentFamily, PadMode, PackSide, RecordingEvent, Lesson, LoopPattern
 │    └── repository# PackRepository, TrackRepository, LessonRepository, CustomPackManager, BackupRestoreManager, UserPreferencesRepository
 ├── ui
 │    ├── navigation# NavRoute, BottomNavBar
 │    ├── pads      # PadScreen, PadViewModel, PadGrid, PadItem, ControlRow, SaveRecordingDialog
 │    ├── library   # LibraryScreen, LibraryViewModel, PackCard
 │    ├── loop      # LoopEditorScreen, LoopEditorViewModel, LoopStepGrid, LoopControlBar
 │    ├── learn     # LearnScreen, LearnViewModel, LessonArenaScreen, LessonArenaViewModel, LessonScoreDialog
 │    ├── sampler   # SamplerDialog, WaveformTrimView
 │    ├── settings  # SettingsScreen, SettingsViewModel, SettingsComponents, AboutLicensesDialog
 │    ├── tracks    # TracksScreen, TracksViewModel, TrackCard, TrackActionDialogs, WaveformThumbnail
 │    └── theme     # Color, Theme, Type
 └── util           # AppConfig, AudioRecordSampler, WaveformUtil, WavUtils
```

- **Kotlin & Jetpack Compose (Material 3)**: Pure declarative UI with neon glow effects, dynamic ripple animations, and haptic feedback.
- **MVVM + StateFlow**: Unidirectional data flow and reactive UI state with Coroutines.
- **Room Database (KSP) & DataStore**: Persistent local storage for recordings, lesson high scores, and audio settings.
- **Low-Latency Audio Engine (< 25ms)**: Native `SoundPool` with preloaded uncompressed PCM buffers in memory; zero garbage collection latency during pad strikes.
- **Multi-Touch Support**: Pointer input with `awaitPointerEventScope` allowing concurrent multi-finger tapping.
- **100% Offline-First**: All sound packs, interactive lessons, sequencer, and tools work completely offline with no network requirements.
- **Localization**: English and Vietnamese (`values-vi`).
- **Clean Architecture Rule**: All source files strictly under 300 lines of code.

---

## Feature Tour

### 1. Sound Pack Library
- 8 bundled genre sound packs: **Neon Trap**, **Cyber Dubstep**, **Cyber EDM**, **Deep House**, **Neuro DnB**, **Lo-Fi Chill**, **Retro Electro**, and **Future Bass**.
- Audio preview with 8-second demo loops.
- Genre filtering and "NEW" pack badges.
- Remembers last-used pack in DataStore.

### 2. Main Launchpad (Pad Screen)
- **3x4 Pad Grid (12 Pads)**: Color-coded by instrument family (Drums = Amber/Red, Bass = Purple, Synth = Green, Chords = Blue, FX/Vocal = Yellow).
- **Tempo & Quantize**: Live BPM slider (±20 BPM), master clock quantize synchronization for loops, metronome toggle.
- **Side A / B Switch**: Instant variation bank switching.
- **Amber "Tutorial" Button**: Quick-jump to pack-specific interactive lessons.

### 3. 16-Step Sequencer (Loop Editor)
- 16 steps × 12 pad rows with scrolling grid and family color coding.
- Live sweeping playhead cursor locked to the master clock.
- Swing groove control (0% to 50%) for bouncy offbeats.
- **Save as Track**: Renders 4 bars (64 steps) to stereo 16-bit 44.1kHz WAV via `AudioRenderer` and saves to My Tracks.

### 4. Learn Mode (Interactive Lessons)
- 6 structured lessons across Neon Trap and Deep House.
- **Listen Tab**: Auto-plays with neon pads illuminating in sequence.
- **Play Tab**: Real-time rhythm training with next-pad pulsing ring indicator, dimmed unused pads, and upcoming white dots.
- **Millisecond Timing Scoring**: **PERFECT!** (±60ms), **GOOD** (±120ms), and **MISS**.
- Completion dialog with 1–3 star ratings, accuracy %, and persistent progress.

### 5. Performance Recorder & My Tracks
- Live timeline event recording (pad index, press time, release time, side, BPM).
- Background offline WAV mixing with clipping protection and peak normalization.
- In-app playback, renaming, deletion, and sharing via Android Share Sheet (`FileProvider`).

### 6. Beatbox Sampler
- Records up to 3 seconds from microphone (`AudioRecord`) with permission rationale dialog.
- Live waveform display with interactive draggable start/end trim handles.
- Audio preview and assignment to any of the 12 pad slots in the custom "My Pads" pack.

### 7. Settings & Backup / Restore
- Audio latency calibration slider (-100ms to +100ms) with interactive test click.
- Haptics and quantize default toggles.
- Language switcher (System / English / Tiếng Việt).
- Full ZIP backup and restore of all recordings and custom pads via Storage Access Framework.
- About dialog with CC0 1.0 Universal public domain license text.

---

## Build and Run Instructions

### Prerequisites
- JDK 17
- Android SDK 35 (Build-Tools 35.0.0)

### Building the Debug APK
```powershell
.\gradlew.bat assembleDebug
```
The APK is generated at `app/build/outputs/apk/debug/app-debug.apk` and copied to `BeatPad.apk`.

### Building the ProGuard/R8 Release APK
```powershell
.\gradlew.bat assembleRelease
```
The release APK is generated at `app/build/outputs/apk/release/app-release.apk`.

### Running Automated Unit Tests
```powershell
.\gradlew.bat testDebugUnitTest
```
Tests cover:
- Beat quantization math (`QuantizerTest`)
- Sound pack JSON manifest parsing (`SoundPackManifestParserTest`)
- WAV header serialization and PCM byte alignment (`WavHeaderTest`)
- Timeline event mixing and downsampling (`RecorderAndTracksTest`)
- Timing window scoring (`TimingWindowScoringTest`)
- Step-to-time conversion & swing math (`StepToTimeConversionTest`)
- Sampler audio trimming & normalization (`SamplerAudioTrimTest`)
- Backup/restore ZIP archive packaging (`BackupRestoreTest`)

---

## Regenerating Audio Samples

All sound samples can be procedurally regenerated using the DSP synthesis generator:
```powershell
javac tools/AudioSynthesizer.java
java -cp tools AudioSynthesizer
```

See [`docs/KNOWN_LIMITATIONS.md`](docs/KNOWN_LIMITATIONS.md) for architectural discussion on Oboe/AAudio and time-stretching.
