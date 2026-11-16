# BOX – typewriter notes (Android)

A WebView app that wraps the BOX notes app (all notes stay on the phone).

## Easiest: build the APK online with GitHub (no Android Studio)
1. Create a free account at github.com and make a **new repository** (private is fine).
2. Click **Add file → Upload files**, drag in *everything inside this folder*
   (including the hidden `.github` folder) and commit.
3. Open the **Actions** tab → **Build BOX APK** → wait about 3–5 minutes.
4. Open the finished run → **Artifacts → BOX-apk** → download, unzip → `app-debug.apk`.
5. Copy the APK to your phone and open it. Allow "Install unknown apps" when asked.

## Or with Android Studio
Open this folder in Android Studio → let it sync → **Build → Build APK(s)**.
The file appears in `app/build/outputs/apk/debug/app-debug.apk`.

The app UI lives in `app/src/main/assets/index.html`. Edit it and rebuild to change the app.
