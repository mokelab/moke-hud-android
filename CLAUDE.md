# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

MokeHudAndroid is at its very first commit: a freshly scaffolded Android Studio project. The
only module is `:demo`, an `com.android.application` module holding the default Compose
"Hello Android" template. The HUD library module that the repository name implies does not
exist yet — when adding it, create a new `com.android.library` module and `include(":<name>")`
it from `settings.gradle.kts`, keeping `:demo` as the consumer/sample app.

## Commands

```bash
./gradlew :demo:assembleDebug            # build the demo APK
./gradlew :demo:installDebug             # build + install on a connected device/emulator
./gradlew :demo:testDebugUnitTest        # host-side (JVM) unit tests
./gradlew :demo:connectedDebugAndroidTest # instrumented tests (needs a device/emulator)
./gradlew :demo:lintDebug                # Android Lint; `lintFix` applies safe fixes
./gradlew :demo:check                    # lint + unit tests
```

Run a single unit test class or method with the standard Gradle test filter:

```bash
./gradlew :demo:testDebugUnitTest --tests 'com.mokelab.hud.android.ExampleUnitTest'
./gradlew :demo:testDebugUnitTest --tests '*.ExampleUnitTest.addition_isCorrect'
```

Single instrumented test:

```bash
./gradlew :demo:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.mokelab.hud.android.ExampleInstrumentedTest
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

`demo/build.gradle.kts` uses AGP 9 syntax that differs from most online examples:

- `compileSdk { version = release(37) }`, not `compileSdk = 37`.
- Release shrinking is configured via `optimization { enable = false }`, not `isMinifyEnabled`.
- R8 keep rules live in `demo/src/main/keepRules/` (AGP merges every file in that directory).
  There is no `proguard-rules.pro`.

All dependency and plugin coordinates go through the version catalog at
`gradle/libs.versions.toml` and are referenced as `libs.*` — add new dependencies there rather
than hardcoding coordinates in a build script.

## Code layout

- Production Kotlin lives under `demo/src/main/kotlin/`, but tests are under
  `demo/src/test/java/` and `demo/src/androidTest/java/` (the template's default). Keep new
  files in whichever directory already exists for that source set.
- The module `namespace`/`applicationId` is `com.mokelab.hud.android.demo`, while the Kotlin
  package is `com.mokelab.hud.android`. They intentionally differ, so the generated `R` class
  and `BuildConfig` are in `com.mokelab.hud.android.demo`.
- UI is Jetpack Compose only — no XML layouts, no view binding. `MainActivity` calls
  `enableEdgeToEdge()` and wraps content in `MokeHudAndroidTheme`
  (`ui/theme/Theme.kt`), which prefers Android 12+ dynamic color and falls back to the
  Purple/Pink palette in `ui/theme/Color.kt`.

## Known template leftovers

`ExampleInstrumentedTest.useAppContext` asserts the package name is `com.mokelab.hud.android`,
but the actual `applicationId` is `com.mokelab.hud.android.demo`, so that test fails on device.
Fix or delete it rather than working around it.
