# Vedic Panchangam (Android)

A minimal, from-scratch Panchangam app showing exactly 10 elements, with
no Jataka/horoscope features:

1. Samvatsaram
2. Ayanam
3. Rithu
4. Masam
5. Paksham
6. Thithi
7. Vasaram
8. Nakshatram
9. Yogam
10. Karanam

## How it works

- **`AstroUtils.kt`** — real astronomical calculations: Julian Day
  conversion, Sun's apparent tropical longitude, Moon's apparent tropical
  longitude (truncated Meeus/ELP2000 series), Lahiri ayanamsa
  (tropical → sidereal), and an approximate local sunrise time.
- **`PanchangamCalculator.kt`** — converts Sun/Moon sidereal longitudes at
  sunrise into the 10 Panchangam values, with lookup tables for names
  (nakshatras, yogas, karanas, tithis, masas, ritus, weekdays, and the
  60-year samvatsara cycle).
- **`MainActivity.kt`** — UI: shows today's Panchangam by default, lets
  you step to the previous/next day, and optionally uses your device's
  GPS location (falls back to Chennai, India if permission is denied).

## Accuracy notes

- Tithi, Nakshatra, Yoga, Karana, Vasara, and Ayana are computed directly
  from real Sun/Moon positions and are accurate to within a few minutes.
- Masa (lunar month name) and Samvatsara (60-year year name) use standard
  approximations described in code comments in `PanchangamCalculator.kt`.
  Regional traditions differ (Amanta vs Purnimanta calendars, adhika/
  kshaya masa, exact Ugadi cutover), so double-check these two against a
  known-good Panchangam for your region if you need calendar-exact
  precision, and adjust the offset constants noted in the comments.

## Building

1. Open this folder in Android Studio (Giraffe or later).
2. Let Gradle sync (it will download the Android Gradle Plugin, Kotlin
   plugin, and the AndroidX / Play Services Location dependencies listed
   in `app/build.gradle.kts`).
3. Run on a device or emulator (minSdk 24 / Android 7.0+).

If you don't want the Google Play Services location dependency, delete
it from `app/build.gradle.kts` and remove the `fetchDeviceLocation()`
body in `MainActivity.kt` — the app will just use the default location.

## Project layout

```
app/src/main/java/com/panchangam/app/
  AstroUtils.kt            - Sun/Moon/ayanamsa/sunrise math
  PanchangamCalculator.kt  - derives the 10 elements + name tables
  MainActivity.kt          - UI wiring
app/src/main/res/layout/
  activity_main.xml        - main screen
  row_element.xml           - reusable label/value row
app/src/main/AndroidManifest.xml
```
