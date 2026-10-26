# AGENTS.md

Guidance for AI agents working on the noiddroid codebase. Mirrors the
conventions used by the [android/skills](https://github.com/android/skills)
project (Apache-2.0) and applies their best-practice patterns.

## Quick reference

- **Project type**: Android arm64 app, Jetpack Compose UI, Hilt DI, Rust NDK core (Poseidon2b nonce search), Vulkan compute shader for GPU path
- **Build**: `./gradlew assembleDebug` / `assembleRelease` (CI handles all builds via `.github/workflows/ci.yml`)
- **Test commands**: see [docs/testing.md](docs/testing.md) for the full matrix
- **Lint**: `./gradlew ktlintCheck detekt` (both `ignoreFailures = true` — autofix workflow commits real fixes back to PR/push)
- **Native core**: cross-compile Rust via `bash scripts/build-prepare.sh` before any gradle assemble step

## Architecture at a glance

```
Kotlin UI (Compose)  →  Hilt graph  →  Repositories  →  Retrofit  →  parano1d node
                        ↓
                    MiningStateBus (object singleton)
                        ↑
MiningService (foreground)  →  MinerNative (JNI)  →  libnoiddroid.so
                                                   →  Rust Poseidon2b + nonce search
                                                   →  Vulkan compute shim (GPU path, falls back to CPU)
```

## Skill-driven guidance applied

| Skill (android/skills) | Where applied |
|---|---|
| `security/android-permissions-security` | `NotificationPermissionController.kt` (3-state POST_NOTIFICATIONS flow), `AndroidManifest.xml` (exported=false for all private components, no `signatureOrSystem`, no `pathPrefix="/"`) |
| `testing/testing-setup` | This file + `docs/testing.md`; JUnit4 + mockk + Robolectric + Compose UI tests; Hilt testing graph |
| `performance/anr-audit-and-fix` | `MiningService` runs work on `Dispatchers.Default`; `BearerInterceptor` uses `runBlocking` only on OkHttp IO dispatcher (never main thread) |
| `performance/r8-analyzer` | `app/proguard-rules.pro` keeps only Hilt-generated + JNI symbols; `android.enableR8.fullMode=false` is NOT in `gradle.properties` |
| `jetpack-compose/jetpack-compose-performance` | `DashboardScreen` uses `remember { derivedStateOf { ... } }` for hash-rate formatting; `collectAsStateWithLifecycle` for ViewModel state; no `@Stable`/`@Immutable` on collection models |

## Commit message conventions

- `feat:` — new feature
- `fix:` — bug fix
- `style:` — ktlintFormat + detekt --auto-correct output
- `ci:` — workflow changes
- `test:` — test additions
- `docs:` — README/AGENTS/docs/ changes
- `refactor:` — code restructure without behavior change

## Auto-fix behavior

The `.github/workflows/autofix.yml` workflow runs on every push to `main`
and every PR. It applies `ktlintFormat` + `detekt --auto-correct` and
commits the result back to the branch as `style: auto-fix (ktlint + detekt
+ spotless)`. If you rebase against `origin/main` before pushing, you
avoid most conflicts.

## Next skill-driven improvements (queued)

- [ ] Add Jacoco coverage config per testing-setup Step 14
- [ ] Add Compose screenshot tests per testing-setup Step 8
- [ ] Add Macrobenchmark + Baseline Profiles per jetpack-compose-performance "Foundational prerequisites"
- [ ] Add `Profiler` config per `profilers/android-profiler` skill
