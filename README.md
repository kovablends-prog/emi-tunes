# Emi Tunes for Android

Same Emi Tunes, as an Android app (Android 8.0 or newer). It reads the music on your phone, keeps playing in the
background with notification controls, and has Wrapped, Daily Mixes, the 5-star equalizer, lyrics and the rest.

## Get the APK (easiest: GitHub builds it for you, free)
1. Make a free account at github.com and click **New repository** (name it `emi-tunes`, Public or Private).
2. On the repository page click **uploading an existing file** and drag in EVERYTHING from this folder
   (`package.json`, `capacitor.config.json`, `www`, `native`, `.gitignore`, `README.md`).
   Then click **Add file > Create new file**, type `.github/workflows/build-apk.yml` as the name,
   paste in the contents of `build-apk.yml`, and click **Commit changes**.
3. Open the **Actions** tab. A run called "Build Android APK" starts by itself (about 5-8 minutes).
   If it does not, click it, then **Run workflow**.
4. When it shows a green tick, open the run and download **emi-tunes-apk** at the bottom. Unzip it to get `app-debug.apk`.
5. Send the APK to your phone, open it, and allow "Install unknown apps" when asked.
6. Open Emi Tunes and tap **Allow** for music access (and notifications, so the player controls show).

If the run fails (red cross), open it, copy the error text from the failed step and send it to Claude to fix.

## Build it yourself on Linux Mint instead
    sudo apt install openjdk-17-jdk nodejs npm unzip
    # install the Android SDK command-line tools, then:
    export ANDROID_HOME=$HOME/Android/Sdk
    npm install && npx cap add android && python3 native/apply.py && npx cap sync android
    cd android && ./gradlew assembleDebug
    # APK: android/app/build/outputs/apk/debug/app-debug.apk

## Notes
- This is a "debug" APK: fine for your own phone, not for the Play Store (that needs a signed release build).
- Online search and Song of the Month need internet. Everything else works offline.
