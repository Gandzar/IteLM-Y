# YMusic Lite — All Downloader & 8D Spatial Audio (Android)

Native Android (Kotlin + Jetpack Compose + Material 3) rewrite of **YMusic Lite**, combining universal media link detection/download management, YouTube search & in-app playback, and the **Kruzen 8D Spatial Audio** real-time DSP engine.

## Features

- **Download**: Paste or share URLs from YouTube, TikTok, Instagram, X/Twitter, Facebook, Spotify, SoundCloud, Pinterest, Reddit, Bilibili, Threads, or direct audio/video links. Includes YouTube oEmbed metadata extraction, MediaStore download queue (`Documents/<folder>`), library saving, and playback.
- **Search**: Debounced YouTube music search (uses optional YouTube Data API v3 key from Settings, with curated demo tracks fallback).
- **8D Spatial Audio (`dev.bagja.spatialaudio`)**:
  - **8D Spatial**: Interactive Orbit Visualizer Canvas (with custom path finger-drawing), 24-band live Spectrum Analyzer, 10 built-in + Lissajous orbit paths, speed/radius/depth/crossfeed/stereo width controls.
  - **Efek**: 10-band EQ + 9 EQ presets, preamp/bass boost/sub-bass punch/vocal exciter/balance/DSP gain, vocal/bass isolator + HP/LP filters, 6 room presets + reverb/echo + custom `.wav` Impulse Response picker, tempo/pitch/drive/compressor/soft limiter.
  - **Preset**: 8 built-in 8D spatial presets, EQ preset shortcuts, saved custom presets CRUD, and JSON import/export.
  - **Melayang**: Floating control bubble & synced lyrics overlay (`FloatingBubbleService`), Universal 8D system audio capture (`Universal8DService` via `MediaProjection`), System Global EQ (`DynamicsProcessing`), and Quick Settings tile (`Quick8DTileService`).
- **Library & Riwayat**: Persistent saved track library with playback/download actions, and up to 50 recent scrape history entries.
- **Settings**: GitHub Releases app update check (opens release page in browser), `@coflyn/scrapr` version metadata check & custom engine config metadata, download folder selection, optional YouTube Data API v3 key, and app info.

## Building

Open in Android Studio or run:

```bash
gradle assembleDebug
```
