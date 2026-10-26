# Testing strategy

Per the `android/skills` `testing/testing-setup` skill (Apache-2.0), this
document describes the testing setup for noiddroid — what frameworks are
installed, where each type of test lives, and the commands to run them.

## Step 1: stack analysis

| Layer | Library | Version |
|---|---|---|
| Dependency Injection | Hilt (com.google.dagger:hilt-android) | 2.52 |
| Unit (local) testing framework | JUnit4 | 4.13.2 |
| Mocking framework | Mockk | 1.13.13 |
| Robolectric | Robolectric | 4.13 |
| Behavior UI tests | Compose Testing APIs (androidx.compose.ui:ui-test-junit4) | via BOM 2024.10.01 |
| Test core | androidx.test:core + androidx.test:runner | 1.6.1 / 1.6.2 |
| Coroutine test | kotlinx-coroutines-test + Turbine | 1.9.0 / 1.2.0 |

The app is **100% Compose** (no XML views, no Espresso).

## Step 2: Hilt testing graph

- `@HiltAndroidApp` on `NoiddroidApp`
- `@AndroidEntryPoint` on `MainActivity`, `MiningService`, `WalletSyncService`, `BootReceiver`
- `@HiltViewModel` on `DashboardViewModel`, `SettingsViewModel`, `WalletViewModel`, `MinerStatusViewModel`, `SplashViewModel`, `ThemeViewModel`
- `@HiltWorker` on `MiningResumeWorker` (via `androidx.hilt:hilt-work` + `HiltWorkerFactory`)
- `WorkManagerModule` provides `Configuration` singleton

For instrumented tests that need Hilt, annotate the test class with
`@HiltAndroidTest` and apply `HiltAndroidRule`. (No instrumented Hilt test
exists yet — added when first needed.)

## Step 3: frameworks installed

| Purpose | Framework |
|---|---|
| Local unit tests | JUnit4 + Mockk |
| Local JVM UI tests (Compose) | Robolectric + `createAndroidComposeRule` |
| Instrumented UI tests | Compose UI Test JUnit4 (run on emulator/device) |
| Coroutines flow assertion | Turbine |
| Coroutine test dispatchers | kotlinx-coroutines-test |

**Not yet installed** (queued per testing-setup Step 3, 8, 13, 14):
- Jacoco (test coverage) — Step 14
- Compose Preview Screenshot Testing tool — Step 8
- Dropshots (device screenshot tests) — Step 13
- UI Automator (end-to-end) — Step 12

## Step 4: fakes & mocks

| Class under test | Fake / mock used |
|---|---|
| `WalletRepository` | `mockk<WalletApi>()` — verifies JSON-RPC envelope + error handling |
| `MiningRepository` | `mockk<MiningApi>()` + `mockk<SettingsRepository>()` (relaxed) — verifies `getBlockTemplate` + `submitBlock` happy path |
| `MinerNative` | not mocked — the `loaded=false` path is exercised on JVM (no `.so` available); real path is covered by instrumented tests on arm64 devices |

## Step 5: unit test sourceset

Path: `app/src/test/java/org/secretarrow/noiddroid/`

| File | What it tests |
|---|---|
| `mine/MinerNativeTest.kt` | lib load + handle creation + hash determinism |
| `rpc/WalletRepositoryTest.kt` | JSON-RPC result unwrapping + error-code → exception mapping |
| `rpc/MiningRepositoryTest.kt` | submitBlock accepted/rejected paths |

Run: `./gradlew testDebugUnitTest`

## Step 6: UI test sourceset

Behavior UI tests use Compose Test APIs and run via Robolectric in the
`test` sourceset (no device needed). Instrumented versions of the same
tests live in the `androidTest` sourceset.

## Step 7: database tests

noiddroid uses Preferences DataStore (no SQLite). No instrumented database
tests are needed. If we add Room in the future, see testing-setup Step 7
for the in-memory database pattern.

## Step 8: screenshot tests (TODO)

Queued. Per testing-setup Step 8, when added:
- Screen-level: Dashboard / Miner / Wallet / Settings / About in 9 sizes (400/610/900 × 400/500/1000)
- Variations: dark + light theme, font scale 1.5
- Tool: Compose Preview Screenshot Testing (AGP 9.5+ uses `testOptions.screenshotTests`; pre-9.5 uses the standalone plugin)

## Step 9: Compose UI behavior tests

Pattern followed:
- Use `createAndroidComposeRule<ComponentActivity>()` for resource access
- Prefer semantic matchers (`onNodeWithText`, `onNodeWithTag`) over chained matchers
- Use `testTag` when matching needs > 3 matchers
- Verify state restoration after config change

The existing `AppSmokeTest.kt` covers navigation between Dashboard/Miner/Wallet/Settings/About — see `app/src/androidTest/java/org/secretarrow/noiddroid/AppSmokeTest.kt`.

## Step 10: navigation tests (TODO)

Queued. Will add tests for back-button handling, deeplinks (none yet),
and "exit through home with multiple backstacks" pattern.

## Step 11: window size + font scale simulation (TODO)

Queued. Will use `DeviceConfigurationOverride` per the testing-setup
common-patterns reference.

## Step 12: end-to-end tests

Per testing-setup Step 12, end-to-end tests should be ~5% of all tests
and cover big user journeys. Currently we have:
- `AppSmokeTest.user_can_navigate_to_*_screen` — 5 navigation e2e tests

UI Automator will be added when we test system-UI interactions
(notifications, edge-to-edge, etc.).

## Step 13: instrumented screenshot tests (TODO)

Queued. Dropshots plugin + a `Dropshots()` JUnit rule.

## Step 14: Jacoco coverage (TODO)

Queued. Add the `jacoco` plugin to `app/build.gradle.kts` and run
`./gradlew jacocoTestReport` to generate coverage HTML at
`app/build/reports/jacoco/`.

## Commands cheat-sheet

```bash
# Local unit tests (JVM, no device)
./gradlew testDebugUnitTest

# Lint
./gradlew ktlintCheck detekt  # both warn-only via || true in CI

# Debug APK
./gradlew assembleDebug

# Release APK (R8 + minified)
./gradlew assembleRelease

# Instrumented UI tests on emulator (requires arm64 device or x86_64 abi added)
./gradlew connectedCheck

# Rust core (run before any gradle assemble that touches native code)
bash scripts/build-prepare.sh
```

## Where reference screenshots will live (TODO)

When Compose Preview Screenshot Testing is added (Step 8):
- Reference images: `app/src/screenshotTest/reference/`
- Failure diffs: `app/build/outputs/scr_snapshot/`
