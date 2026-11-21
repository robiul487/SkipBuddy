# SkipBuddy

SkipBuddy is an Android app that taps YouTube's **Skip Ad** button automatically, for viewers who cannot tap it themselves. It was built for a father who watches YouTube but cannot press the small Skip button in time.

## What it does

- Watches the YouTube app through an Accessibility Service and taps **Skip Ad / Skip Ads** the moment it becomes available (portrait, landscape and fullscreen).
- **Mutes unskippable ads** and restores the exact previous volume when the ad ends.
- Plays a short **voice announcement** ("Advertisement is playing, please wait a moment") with an on-screen message while an unskippable ad plays.
- A small **floating button** shows the status inside YouTube only. It can be hidden and locked, so accidental taps do nothing.
- **Home dashboard**: ads detected, skipped, unskippable and failed today and in total, success rate, last 7 days chart and recent activity.
- Works in **English, Bangla (বাংলা) and Hindi (हिन्दी)**. English is the default; the language can be changed in Settings.
- Detection uses view IDs first, then text matching, with a language priority cascade. Ad detection and Skip detection are separate rule sets.

## Privacy

- 100% offline. No internet permission, no accounts, no analytics, no data collection of any kind.
- The Accessibility Service is active only inside the YouTube app (`com.google.android.youtube`).

## Setup notes

1. Install the APK and open SkipBuddy.
2. Follow the Setup Wizard: enable the Accessibility Service, allow display over other apps, and exempt the app from battery optimization.
3. On some phones (Android 13 and newer), Android may show **"Restricted setting"** when you try to enable the Accessibility Service for a sideloaded app. Fix: **Settings > Apps > SkipBuddy > ⋮ (top right) > Allow restricted settings**, confirm with your PIN, then go back and turn the Accessibility Service ON. This is an Android security rule for apps installed outside the Play Store; it is a one-time step per install.

## Tech

- Kotlin + Jetpack Compose, MVVM, DataStore.
- Permissions are limited to: Accessibility binding, display over other apps, notifications, battery-optimization exemption, boot completed, vibrate. Nothing else.

## Status

Personal project, built with Google AI Studio. Debug builds are produced with the standard Gradle `assembleDebug` process.
