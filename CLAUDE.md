# ClawHark — Definitive Reference

> **Last updated:** 2026-03-07
> **Package:** `ai.etti.clawhark` | **Version:** 1.1.0 (versionCode 3)
> **Repo:** `git@github.com:etticat/clawhark.git` | **Branch:** `main`
> **Play Store:** https://play.google.com/store/apps/details?id=ai.etti.clawhark

---

## What Is ClawHark

An open-source Wear OS app that turns any smartwatch into an always-on AI wearable. Like Omi, Limitless, or Bee — but running on hardware you already own, with zero third-party servers.

**The loop:** Watch records → VAD filters silence → 5-min WAV chunks → auto-upload to Google Drive → pull script downloads + deletes from Drive → transcription pipeline (Whisper + AssemblyAI) → speaker-diarized transcripts → OpenClaw scans for action items.

**Etti uses this every day.** It replaced Omi completely as of March 2026. All daily coaching, transcript scanning, and action extraction runs on ClawHark recordings.

---

## Architecture

### Watch App (Kotlin, ~2,300 LOC)

```
app/src/main/java/ai/etti/clawhark/
├── MainActivity.kt              # Single-screen UI — big toggle button
├── RecordingService.kt          # Foreground service, VAD, chunking (928 LOC — the heart)
├── DriveUploader.kt             # Google Drive upload via REST API
├── UploadWorker.kt              # WorkManager-based upload scheduling
├── AuthManager.kt               # OAuth2 with PKCE for Wear OS (379 LOC)
├── BootReceiver.kt              # Resume recording after reboot
├── ComplicationToggleReceiver.kt # Watch face complication toggle
├── RecordingComplicationService.kt # Complication data provider
└── AppLog.kt                    # Structured logging
```

### Key Technical Details

| Aspect | Detail |
|--------|--------|
| **Audio format** | WAV, 16kHz mono |
| **Chunk size** | 5 minutes |
| **VAD** | Energy-based — only writes chunks with speech detected |
| **Upload** | WiFi-only via WorkManager, scoped `drive.file` OAuth2 |
| **Boot persist** | `RECEIVE_BOOT_COMPLETED` → auto-restart recording |
| **Min SDK** | 30 (Wear OS 3+), target 34 |
| **Tested on** | Pixel Watch 3 |
| **Battery** | ~4-6 hours active recording (with VAD savings) |

### Privacy Model
- `drive.file` scope — can only see its OWN files
- No analytics, no telemetry, no crash reporting, no servers
- MIT licensed, fully auditable

---

## Scripts

### `scripts/pull.sh` — Download from Drive
Pulls all files from the `ClawHark/` Drive folder, organizes by date, deletes from Drive after download.

```bash
# Requires credentials at ~/.clawhark/credentials.json
CLAWHARK_OUTPUT=~/.clawhark/recordings ./scripts/pull.sh
```

### `scripts/transcribe.py` — 4-Phase Pipeline
1. **Whisper** — local speech detection, filter silent chunks
2. **Segment** — group chunks into conversations by time gaps
3. **Concat** — merge related chunks into conversation audio
4. **Diarize** — speaker-separated transcription (AssemblyAI or Gemini)

```bash
python3 scripts/transcribe.py 2026-03-07
python3 scripts/transcribe.py 2026-03-07 --provider gemini
```

---

## Build & Deploy

```bash
# Build debug APK
./gradlew assembleDebug

# Build release (needs keystore.properties)
./gradlew assembleRelease

# Install to connected watch via ADB
adb install app/build/outputs/apk/debug/app-debug.apk

# Logs
adb logcat | grep ClawHark
```

**Signing:** `keystore.properties` (not in git) → points to `keystore/clawhark.jks`

**SDK:** `ANDROID_HOME=~/Library/Android/sdk`

---

## OpenClaw Integration

The `openclaw/` folder contains the OpenClaw skill for automating pull + transcribe + action extraction.

The daily pipeline runs via OpenClaw crons (11am + 6pm "Watch Pipeline"):
1. Pull recordings from Drive
2. Transcribe with speaker diarization
3. Scan for action items, todos, mentions of Onyx
4. Store transcripts at `~/.openclaw/workspace/watch-recordings/transcripts/YYYY-MM-DD-diarized.md`

---

## Store Listing

- **Short description:** Turn any Wear OS watch into an AI wearable. Always-on recording + Google Drive.
- **Assets:** `store-listing/` — feature-graphic.png, screenshot-recording.png, LISTING.md

---

## What's Next / Ideas

This is Etti's passion project — a real product with real users. Potential areas:
- **Marketing & growth** — more visibility, community building
- **New features** — companion phone app, multiple cloud backends, real-time streaming
- **Code quality** — tests, CI/CD, automated Play Store deployment
- **RN library** — `saturn-rn-audio-recorder` at `~/code/saturn-rn-audio-recorder/` is a related React Native audio recording library

---

## Rules for This Codebase

1. **Read this file first** before any ClawHark work.
2. **Test on real hardware** — Wear OS emulators are unreliable for audio. Use `adb install` to the Pixel Watch.
3. **Don't break the upload loop** — the Drive upload is the critical path. If recordings don't upload, they're lost when storage fills.
4. **Keep it simple** — this is a ~2,300 LOC app. That's a feature, not a limitation.
5. **Privacy is non-negotiable** — no analytics, no tracking, no external servers. Ever.
