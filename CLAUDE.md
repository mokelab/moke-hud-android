# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

MokeHudAndroid provides a debug-time HUD: a library that overlays Analytics (and similar)
events on top of a running app's screen, plus a multi-module demo that shows how a real app
feeds it through a swappable DI seam.

### The HUD library

- **`:hud`** — the library itself (`com.android.library`, namespace `com.mokelab.hud.android`).
  The automatic attach machinery: a manifest-declared `ContentProvider`
  (`internal/HudInitProvider.kt`) installs `Application.ActivityLifecycleCallbacks`
  (`internal/HudActivityWatcher.kt`) from its `onCreate()`, so a host app gets the HUD just by
  adding `:hud` to `dependencies` — **no `Application` subclass required.**
- The watcher attaches an `internal/HudOverlayView.kt` per Activity as an **independent
  `WindowManager` sub-window** (`TYPE_APPLICATION_PANEL`, keyed by the Activity's
  `windowToken`), **not** a child of `decorView`. This gives the overlay its own
  ViewRootImpl/Surface so a bare `invalidate()` reliably recomposes its own frame without
  riding on the host's (Compose etc.) draw pass — the decorView-child approach failed to
  repaint while the host was static. Attach happens on `onActivityResumed` (posted one loop
  later so the `windowToken` is valid), detach on `onActivityDestroyed`. The window is
  `FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCHABLE`, so it never intercepts input.
- `HudOverlayView` renders POSTed messages as stacked bands at the top of the screen, each with
  an auto-expiry timer; `MAX_MESSAGES = 10` caps the stack and evicts oldest-first. It draws on
  plain `Canvas` and pads for system-bar insets.
- Public surface is `Hud` (`Hud.kt`): `post(message: String, durationMillis)`,
  `post(event: HudEvent, durationMillis)`, `setEnabled(...)`, `isEnabled`. All are thread-safe
  (they hop to the main thread internally); `install(...)` is `internal` (called only from the
  ContentProvider). When disabled, `post` drops messages rather than queueing them.
- `HudEvent` (name/params/timestamp) is the structured event type and **is** live: `post(event)`
  carries it through unflattened, and `HudOverlayView` renders `name` as the band title with
  formatted `params` as a smaller detail line beneath it. The `post(String)` overload remains as
  a freeform-message primitive (title only, no detail); `timestampMillis` is carried on the model
  but not yet drawn.

### The demo and its analytics seam

`:demo` (`com.android.application`) is a small Compose + nav3 app (a mokera list⇄detail) that
emits Analytics events through an interface, then binds that interface to either a plain or a
HUD-decorating implementation via Hilt. The seam is what the library is meant to plug into:

- **`:core:analytics:api`** — the `AnalyticsLogger` interface (`screenView`, `logEvent`). Pure
  abstraction; no Hilt, no HUD. Feature code depends only on this.
- **`:core:analytics:impl`** — `AnalyticsLoggerImpl`, the "real" logger. Currently a placeholder
  that only writes to logcat (stands in for a Firebase Analytics call).
- **`:core:analytics:prod`** — Hilt module binding `AnalyticsLogger` directly to
  `AnalyticsLoggerImpl`. No `:hud` dependency.
- **`:core:analytics:debug`** — Hilt module that binds `AnalyticsLogger` to
  `HudAnalyticsLogger`, which calls `Hud.post(...)` for each event and then forwards to the real
  `AnalyticsLoggerImpl` (injected under the `@DelegateLogger` qualifier). This module depends on
  `:hud`; it is the only place the demo's app code touches the HUD.
- **`:feature:mokera:{api,impl}`** — the sample feature. `:api` holds the `@Serializable` nav3
  `NavKey`s (`MokeraList`, `MokeraDetail(id)`); `:impl` holds the Compose screens + Hilt
  ViewModels (detail uses assisted injection for its `id`) and the `mokeraEntries` entry
  provider. Composables pull the singleton `AnalyticsLogger` via a Hilt `EntryPoint`
  (`rememberAnalyticsLogger`), so `:impl` depends only on `:core:analytics:api` — the concrete
  binding is chosen by the app.
- **Which logger the demo uses is a hand-edited line** in `demo/build.gradle.kts`: it currently
  depends on `:core:analytics:prod` (logcat only, HUD not shown), with the
  `:core:analytics:debug` swap commented out right below it. So a fresh clone does **not** show
  the HUD until you flip that line. `DemoApp` is `@HiltAndroidApp` and `MainActivity` is
  `@AndroidEntryPoint` — that `Application` exists for Hilt, not for the HUD (the HUD still
  needs no Application changes). `HudOverlayAttachTest` (instrumented) verifies the overlay
  attaches as its own window without touching `:demo`'s `Application`.

## Commands

The build is multi-module: `:hud`, `:demo`, `:core:analytics:{api,impl,prod,debug}`, and
`:feature:mokera:{api,impl}` (see `settings.gradle.kts`). The examples below use `:demo` and
`:hud`; substitute any module path to run the same task against it.

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
  or `com.android.library`, plus feature plugins as needed: `kotlin-compose` for Compose
  (`:demo`, `:feature:mokera:impl`), `hilt` + `ksp` for Hilt DI (`:demo`, both
  `:core:analytics:{prod,debug}`, `:feature:mokera:impl`), and `kotlin-serialization` for the
  nav3 `@Serializable` keys (`:feature:mokera:api`). Do not add the Kotlin Android plugin "to
  make Kotlin work" — it is not missing. All are referenced as `libs.plugins.*`.
- `compileSdk { version = release(37) }`, not `compileSdk = 37`.
- Release shrinking is configured via `optimization { enable = false }`, not `isMinifyEnabled`.
- R8 keep rules live in `demo/src/main/keepRules/` (AGP merges every file in that directory).
  There is no `proguard-rules.pro`.
- `:hud` declares its `namespace` in `hud/build.gradle.kts`. It **does** have an
  `AndroidManifest.xml` (`hud/src/main/AndroidManifest.xml`), added specifically to declare the
  auto-init `ContentProvider`; it carries no `package` attribute (namespace comes from the
  build script).

All dependency and plugin coordinates go through the version catalog at
`gradle/libs.versions.toml` and are referenced as `libs.*` — add new dependencies there rather
than hardcoding coordinates in a build script.

## Code layout

- Namespace and Kotlin package match in every module, and each module's namespace mirrors its
  Gradle path: `:hud` is `com.mokelab.hud.android`, `:demo` is `com.mokelab.hud.android.demo`
  (also its `applicationId`), `:core:analytics:api` is
  `com.mokelab.hud.android.core.analytics.api`, `:feature:mokera:impl` is
  `com.mokelab.hud.android.feature.mokera.impl`, and so on. Keep them aligned — e.g. putting
  `:demo` code back into `com.mokelab.hud.android` would split that package across two modules
  and make an unqualified `R` in `:demo` resolve to the library's.
- **Dependency direction runs api ← impl/prod/debug ← app, never the reverse.** Feature and
  app-side code depend on `:core:analytics:api` only; the concrete `AnalyticsLogger` binding is
  supplied by whichever `:core:analytics:{prod,debug}` module the app pulls in. Only
  `:core:analytics:debug` (and `:demo`, transitively) may depend on `:hud`. Don't make
  `:feature:*` or `:core:analytics:api`/`impl` reach into `:hud`.
- Source directories are inconsistent for historical reasons; put new files where that source
  set already keeps them:
  - `:hud` — `hud/src/main/kotlin/`, `hud/src/test/kotlin/`
  - `:demo` — `demo/src/main/kotlin/`, but `demo/src/test/java/` and `demo/src/androidTest/java/`
  - `:core:*` and `:feature:*` — `src/main/kotlin/`
- **`:hud` does not use Compose.** The overlay is meant to attach to arbitrary host apps, so the
  library stays on plain `View`/`Canvas` and depends only on `androidx.core-ktx`. Do not pull
  Compose into `hud/build.gradle.kts`.
- **`:demo` is Jetpack Compose only** — no XML layouts, no view binding. `MainActivity` calls
  `enableEdgeToEdge()` and wraps content in `MokeHudAndroidTheme` (`ui/theme/Theme.kt`), which
  prefers Android 12+ dynamic color and falls back to the Purple/Pink palette in
  `ui/theme/Color.kt`.

## Already built (don't rewrite these as if missing)

CLAUDE.md previously listed these as deferred; they exist now:

- Overlay event rendering (stacked message bands with per-message expiry, `MAX_MESSAGES` cap)
  and the `Hud.post(...)` API that feeds it.
- The app→HUD ingestion path: `HudAnalyticsLogger` in `:core:analytics:debug` turns every
  `AnalyticsLogger` call into a structured `Hud.post(HudEvent(...))` (`screenView` becomes a
  `screen_view` event with `screen_name`/`screen_class` params).
- Structured event rendering: `Hud.post(HudEvent)` draws `name` + formatted `params` distinctly
  (see the HUD library notes above).
- Maven publishing for `:hud` — `hud/build.gradle.kts` applies `maven-publish` (group
  `com.mokelab.hud`, `artifactId` `hud-android`, current `version` `0.1.1`) and publishes to a
  local `.gh-pages` Maven repo.

## Not built yet

Deliberately deferred; do not assume these exist:

- A real Analytics SDK behind `AnalyticsLoggerImpl` — it only logs to logcat today; Firebase
  Analytics (et al) is not wired.
- Rendering of `HudEvent.timestampMillis` — the model carries it but the overlay doesn't draw it.
- A `:hud-no-op` sibling module to strip the HUD from release builds — the plan is to add it
  once the public API has settled, at which point consumers switch to
  `debugImplementation`/`releaseImplementation`. (The demo's prod/debug analytics swap is a
  hand-edited dependency line today, not a build-variant split.)
