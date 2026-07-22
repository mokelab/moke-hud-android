# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

MokeHudAndroid provides a debug-time HUD: a library that overlays Analytics (and similar)
events on top of a running app's screen. Two modules:

- **`:hud`** — the library itself (`com.android.library`, namespace `com.mokelab.hud.android`).
  Currently only holds `HudEvent`, the model for one overlaid event. The overlay rendering,
  the public install API, and Analytics ingestion are not written yet.
- **`:demo`** — the consumer/sample app (`com.android.application`). It depends on `:hud` via
  `implementation(project(":hud"))`. `MainActivity` references `HudEvent` purely to prove the
  wiring compiles; replace that with a real HUD call once the overlay exists.

## Commands

Substitute `:hud` for `:demo` to run the same task against the library.

```bash
./gradlew :demo:assembleDebug            # build the demo APK
./gradlew :demo:installDebug             # build + install on a connected device/emulator
./gradlew :demo:testDebugUnitTest        # host-side (JVM) unit tests
./gradlew :demo:connectedDebugAndroidTest # instrumented tests (needs a device/emulator)
./gradlew :demo:lintDebug                # Android Lint; `lintFix` applies safe fixes
./gradlew :demo:check                    # lint + unit tests
./gradlew :hud:assembleDebug             # build the library AAR
```

Run a single unit test class or method with the standard Gradle test filter:

```bash
./gradlew :demo:testDebugUnitTest --tests 'com.mokelab.hud.android.demo.ExampleUnitTest'
./gradlew :hud:testDebugUnitTest --tests '*.HudEventTest'
```

Single instrumented test:

```bash
./gradlew :demo:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.mokelab.hud.android.demo.ExampleInstrumentedTest
```

## Toolchain

The versions here are deliberately near the bleeding edge; be careful before assuming that
advice written for older AGP/Gradle applies.

- Gradle 9.5 via the wrapper. Always invoke `./gradlew`, never a system `gradle`.
- The Gradle daemon runs on **JDK 25**, pinned by `gradle/gradle-daemon-jvm.properties` and
  auto-provisioned through the foojay resolver — no local JDK 25 install is required.
- AGP 9.3.0, Kotlin 2.2.10, Compose BOM 2026.02.01, `compileSdk`/`targetSdk` 37, `minSdk` 24.
- Java source/target compatibility is 11 (this is separate from the daemon JVM above).
- **Configuration cache is on** (`org.gradle.configuration-cache=true` in `gradle.properties`).
  Build-logic changes that read at configuration time (e.g. `System.getenv`, `project` access
  inside task actions) will fail the build rather than silently degrade.

### AGP 9 DSL differences

The build scripts use AGP 9 syntax that differs from most online examples:

- **No `org.jetbrains.kotlin.android` plugin anywhere.** Kotlin compiles through AGP 9's
  built-in Kotlin support, so a module's `plugins` block only needs `com.android.application`
  or `com.android.library` (plus `kotlin-compose` if that module uses Compose). Do not add the
  Kotlin Android plugin "to make Kotlin work" — it is not missing.
- `compileSdk { version = release(37) }`, not `compileSdk = 37`.
- Release shrinking is configured via `optimization { enable = false }`, not `isMinifyEnabled`.
- R8 keep rules live in `demo/src/main/keepRules/` (AGP merges every file in that directory).
  There is no `proguard-rules.pro`.
- `:hud` declares its `namespace` in `hud/build.gradle.kts` and has **no `AndroidManifest.xml`**;
  add one only when the library needs a permission or a manifest component.

All dependency and plugin coordinates go through the version catalog at
`gradle/libs.versions.toml` and are referenced as `libs.*` — add new dependencies there rather
than hardcoding coordinates in a build script.

## Code layout

- Namespace and Kotlin package match in both modules: `:hud` is `com.mokelab.hud.android`,
  `:demo` is `com.mokelab.hud.android.demo` (which is also its `applicationId`). Keep them
  aligned — putting `:demo` code back into `com.mokelab.hud.android` would split that package
  across two modules and make an unqualified `R` in `:demo` resolve to the library's.
- Source directories are inconsistent for historical reasons; put new files where that source
  set already keeps them:
  - `:hud` — `hud/src/main/kotlin/`, `hud/src/test/kotlin/`
  - `:demo` — `demo/src/main/kotlin/`, but `demo/src/test/java/` and `demo/src/androidTest/java/`
- **`:hud` does not use Compose.** The overlay is meant to attach to arbitrary host apps, so the
  library stays on plain `View`/`Canvas` and depends only on `androidx.core-ktx`. Do not pull
  Compose into `hud/build.gradle.kts`.
- **`:demo` is Jetpack Compose only** — no XML layouts, no view binding. `MainActivity` calls
  `enableEdgeToEdge()` and wraps content in `MokeHudAndroidTheme` (`ui/theme/Theme.kt`), which
  prefers Android 12+ dynamic color and falls back to the Purple/Pink palette in
  `ui/theme/Color.kt`.

## Not built yet

Deliberately deferred; do not assume these exist:

- The overlay itself (attaching a `View` to each Activity's decor view, e.g. via
  `Application.ActivityLifecycleCallbacks`) and the public install API.
- Ingestion from Analytics SDKs (Firebase Analytics et al).
- A `:hud-no-op` sibling module to strip the HUD from release builds — the plan is to add it
  once the public API has settled, at which point `:demo` switches to
  `debugImplementation`/`releaseImplementation`.
- Maven publishing coordinates.
