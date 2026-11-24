# Gulf Job Works Android

A mobile app for the live Gulf Job Works website. The app adds native navigation and Android file handling around the existing website. Job listings, forms, and content still come from https://gulfjobworks.com/ and require an internet connection.

## Build in Android Studio

1. Install the current Android Studio with Android SDK Platform 35, Android SDK Build Tools, JDK 17, and Gradle 8.9 or newer compatible with Android Gradle Plugin 8.7.3.
2. Open this folder as an Android Studio project. Set Gradle to your local Gradle 8.9 installation if Android Studio asks, and allow sync to download the Android Gradle Plugin.
3. Select **Build → Build APK(s)**, or run `gradlew assembleDebug` from the project folder. The debug APK appears at `app/build/outputs/apk/debug/app-debug.apk`.
4. For a distributable release, use **Build → Generate Signed Bundle / APK**, create and securely retain your own signing key, and build a signed release APK. Keep that key for future updates.

Package ID: `com.gulfjobworks.mobile`. Minimum Android version: 7.0 (API 24).

## App behavior

- Home, Jobs, Upload CV, and Contact tabs; more destinations in the top menu.
- Website links stay inside the app. External links, email, phone, and WhatsApp open in the appropriate installed app.
- Android document picker supports CV uploads; system Download Manager saves website documents.
- Loading progress, retry screen, share/open-in-browser actions, and Android Back navigation.
- Mobile CSS adjusts form controls and media width without replacing the website's layout.

## Limits

The website itself owns its forms and job data; a WebView wrapper cannot repair a broken or unresponsive website page. Test registration, login, CV upload, downloads, and every job application on a real Android device before release. This project does not include a generated APK because the current build environment has no Android SDK, Gradle, or Android build tools.
