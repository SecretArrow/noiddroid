# Noiddroid

**External Poseidon2b nonce-search worker + wallet RPC client for the [parano1d](https://parano1d.org/) network — Android arm64.**

[![CI](https://github.com/SecretArrow/noiddroid/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/SecretArrow/noiddroid/actions/workflows/ci.yml)
[![Auto-fix](https://github.com/SecretArrow/noiddroid/actions/workflows/autofix.yml/badge.svg)](https://github.com/SecretArrow/noiddroid/actions/workflows/autofix.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-black.svg)](LICENSE)

> Noiddroid is a community project and is not affiliated with the upstream
> parano1d maintainers. The Poseidon2b implementation in this repo is a
> best-effort re-derivation from public papers; it must be validated
> against upstream test vectors before being trusted with real blocks.

## What this is

Noiddroid turns an Android phone into a remote **nonce-search worker**
for a parano1d full node you control. The phone never holds the chain
state, never constructs a block, and never selects transactions — it
just hashes 128-bit nonces against immutable templates the node hands
it over HTTP, and ships back any winner.

```
┌──────────────────────┐     HTTP/JSON-RPC     ┌─────────────────────────┐
│  Your parano1d node  │  ←────────────────→  │   Noiddroid (this app)  │
│  --mode extminer      │   getBlockTemplate   │   Poseidon2b in Rust    │
│  port 9601           │   submitBlock        │   + Vulkan GPU path    │
│  --mining-key-file   │                       │   + Wallet RPC client  │
└──────────────────────┘                       └─────────────────────────┘
```

This design follows the parano1d mining spec: the worker "does not receive
the block body, State witness or HistoryStep witness. It cannot replace
transactions, alter the State root or modify a template after proof
construction." — [docs.parano1d.org/mining](https://docs.parano1d.org/mining)

## Architecture

| Layer | Tech | Notes |
|-------|------|-------|
| UI | Jetpack Compose, Material 3 | Brutalist minimal design system |
| DI | Hilt | `@HiltAndroidApp` + `@HiltViewModel` + `@HiltWorker` |
| Networking | Retrofit + OkHttp + kotlinx.serialization | JSON-RPC 2.0 envelope for wallet |
| Persistence | Preferences DataStore | No room — settings only |
| Background | Foreground service + WorkManager | Battery + thermal + wake-lock guards |
| Native (CPU) | Rust → `libnoiddroid_core.a` via NDK | Poseidon2b + multi-threaded search |
| Native (GPU) | Vulkan Compute shader via JNI | Optional, falls back to CPU |
| Crash reporting | Sentry | Opt-out in About screen |

## Getting started

### Build locally (developer workflow)

```bash
# 1. Install Android NDK r26+ and set ANDROID_NDK_HOME
export ANDROID_NDK_HOME=/path/to/ndk

# 2. Add the Rust Android target
rustup target add aarch64-linux-android

# 3. Cross-compile the Rust core
bash scripts/build-prepare.sh

# 4. (optional) Recompile the Vulkan shader
python3 scripts/compile_shaders.py

# 5. Assemble the debug APK
./gradlew assembleDebug
```

### CI/CD (production workflow)

All production builds happen in GitHub Actions — no local Android SDK
required. See `.github/workflows/`:

- **`ci.yml`** — lint (ktlint + detekt) → unit tests → build (Rust + APK)
  → e2e instrumented tests on a managed Android emulator.
- **`autofix.yml`** — runs `ktlintFormat`, `detekt --auto-correct`,
  `spotlessApply` and pushes the changes back to the PR branch. If the
  `COPILOT_TOKEN` secret is set, a second AI pass attempts to fix any
  remaining lint errors.
- **`release.yml`** — triggered by a `v*` tag, builds a signed release
  APK and attaches it to a GitHub Release.

### Required repo secrets (Settings → Secrets → Actions)

| Name | Purpose |
|------|---------|
| `GH_PAT` | GitHub PAT used by `autofix.yml` to push back to PR branches. **Read the security note below.** |
| `COPILOT_TOKEN` | (optional) GitHub Copilot token for AI auto-fix pass. If absent, only deterministic fixes run. |
| `SIGNING_KEYSTORE` | (optional) base64 of release keystore. If absent, release APK is debug-signed. |
| `SIGNING_KEY_ALIAS` | (optional) keystore alias for release signing. |
| `SIGNING_KEY_PASSWORD` | (optional) keystore key password. |
| `SIGNING_STORE_PASSWORD` | (optional) keystore store password. |

## Security

⚠️ **Never commit a GitHub Personal Access Token (PAT) to a repo.** If you
need a token for the `autofix` workflow to push back to PR branches:

1. Go to https://github.com/settings/tokens
2. Generate a fine-grained PAT scoped to **only** `SecretArrow/noiddroid`
   with `Contents: Read and write` and `Pull requests: Read and write`.
3. In the repo: Settings → Secrets and variables → Actions → New
   repository secret → name it `GH_PAT` → paste the token.
4. Reference it in workflows as `${{ secrets.GH_PAT }}`. The token never
   appears in logs, in commit messages, or in any file committed to the
   repo.

If a token is leaked (e.g. committed by accident, pasted into a chat,
shared in a support ticket): **revoke it immediately** at
https://github.com/settings/tokens — do not just delete it. A revoked
token kills any cached sessions.

## Layout

```
.
├── app/
│   ├── src/main/
│   │   ├── cpp/                  # JNI bridge + Vulkan compute shim
│   │   │   ├── CMakeLists.txt
│   │   │   ├── jni_bridge.cpp
│   │   │   ├── vulkan_compute.cpp
│   │   │   └── poseidon2b_spirv.h  # generated by scripts/compile_shaders.py
│   │   ├── java/org/secretarrow/noiddroid/
│   │   │   ├── mine/            # JNI wrapper (MinerNative.kt)
│   │   │   ├── rpc/             # Retrofit interfaces + DTOs
│   │   │   ├── service/         # Foreground service + WorkManager + boot receiver
│   │   │   ├── store/           # DataStore-backed settings
│   │   │   └── ui/              # Compose screens + theme + components
│   │   └── res/                 # strings, themes, drawables
│   └── build.gradle.kts
├── rust/                         # NDK Rust crate (Poseidon2b + search loop)
│   ├── Cargo.toml
│   └── src/{lib,poseidon2b,search}.rs
├── vulkan/shaders/poseidon2b.comp  # GLSL compute shader source
├── scripts/
│   ├── build-prepare.sh         # Cross-compile Rust → libnoiddroid_core.a
│   ├── compile_shaders.py       # GLSL → SPIR-V → C header
│   └── ai_autofix.py            # LLM-assisted lint fixer (used by CI)
├── .github/workflows/           # ci.yml, autofix.yml, release.yml
└── config/detekt.yml
```

## Roadmap

- [x] v0.1 — Architecture, CI/CD, brutalist UI, foreground service, Rust NDK scaffold, Vulkan dispatch skeleton.
- [ ] v0.2 — Poseidon2b validation against upstream test vectors.
- [ ] v0.3 — Full GLSL Poseidon2b shader (replace placeholder SPIR-V).
- [ ] v0.4 — Wallet screens (send/receive flows, receipt viewer).
- [ ] v0.5 — Pool mode (Stratum v2 job-declaration protocol).
- [ ] v0.6 — Adaptive hash-rate governor (thermal-aware throttling).

## License

MIT — see [LICENSE](LICENSE).

## Acknowledgements

- The parano1d maintainers for the protocol spec at [docs.parano1d.org](https://docs.parano1d.org/).
- The Poseidon-2 paper (Grassi, Khovratovich, Rechberger, Roy, 2023) for the hash design.
- The Android NDK + Vulkan samples for the GPU dispatch pattern.
