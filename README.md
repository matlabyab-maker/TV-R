# TV R

Android live TV player with HLS playback and direct clear-HLS recording.

## Features
- Play a stream from a direct URL, including HLS/M3U8.
- Playback controls provided by AndroidX Media3.
- Start and stop direct HLS recording through a foreground service.
- Recording supports clear HLS MPEG-TS streams; DRM/encrypted streams are not bypassed.

## Build
- JDK 17
- Android SDK 35
- Gradle 8.9
- Android Gradle Plugin 8.7.3
- Kotlin 2.0.21

Push to any branch to run GitHub Actions, or run the workflow manually.
The debug APK is uploaded as the `TV R-debug` artifact.
