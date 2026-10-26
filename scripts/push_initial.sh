#!/usr/bin/env bash
# scripts/push_initial.sh — pushes the initial commit to SecretArrow/noiddroid.
#
# Run AFTER you have:
#  1. Revoked the previously-leaked PAT at
#     https://github.com/settings/tokens
#  2. Generated a NEW fine-grained PAT scoped to SecretArrow/noiddroid only
#     with Contents: Read+write + Workflows: Read+write
#  3. Added it as repo secret GH_PAT in the repo's
#     Settings -> Secrets and variables -> Actions
#  4. Exported the SAME new token as GH_TOKEN in your shell:
#
#         export GH_TOKEN=ghp_NEW_TOKEN_HERE
#
# Then run:
#         bash scripts/push_initial.sh
#
# After the push succeeds, also rotate the GH_TOKEN env var out of your
# shell history: `unset GH_TOKEN; history -c`

set -euo pipefail

if [[ -z "${GH_TOKEN:-}" ]]; then
  echo "ERROR: GH_TOKEN env var not set. Did you rotate the leaked PAT?" >&2
  echo "  export GH_TOKEN=ghp_NEW_TOKEN" >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_ROOT"

# Initialise git if not already
if [[ ! -d .git ]]; then
  git init -b main
  git config user.name "SecretArrow"
  git config user.email "secretarrow@users.noreply.github.com"
fi

# Configure remote with the token embedded (the token will not be
# committed to a file — it lives only in the local git config of this
# machine, and we strip it out at the end of the script).
git remote remove origin 2>/dev/null || true
git remote add origin "https://x-access-token:${GH_TOKEN}@github.com/SecretArrow/noiddroid.git"

# Stage everything (respecting .gitignore)
git add -A
git status -sb

# Make the commit if there isn't one yet
if ! git log --oneline -1 >/dev/null 2>&1; then
  git commit -m "feat: noiddroid v0.1.0-alpha01 — Android parano1d worker + wallet

External Poseidon2b nonce-search worker + wallet RPC client for the
parano1d network, Android arm64 only.

Layers:
- Kotlin / Jetpack Compose / Material 3 (brutalist minimal theme)
- Hilt DI, Preferences DataStore, WorkManager, Sentry
- Rust NDK core (libnoiddroid_core.a) — Poseidon2b + multi-threaded search
- Vulkan Compute shader path (optional, falls back to CPU)
- Foreground service with battery + thermal + wake-lock guards
- Retrofit + kotlinx.serialization JSON-RPC 2.0

CI/CD (GitHub Actions):
- ci.yml: lint -> unit tests -> Rust cross-compile + APK build -> e2e
- autofix.yml: ktlintFormat + detekt autoCorrect + spotlessApply, with optional AI pass
- release.yml: tag-triggered signed APK release

Security:
- No tokens in the repo. Workflows reference \$\{\{ secrets.GH_PAT \}\}.
- Network security config limits cleartext traffic to LAN node IPs.
- Bearer tokens / payout addresses excluded from cloud backup.

Co-Authored-By: Z.ai <noreply@z.ai>"
fi

# Push to main
git push -u origin main

# Strip the token out of the local git config so it doesn't linger
git remote set-url origin "https://github.com/SecretArrow/noiddroid.git"

echo
echo "==> Push complete. Verify at https://github.com/SecretArrow/noiddroid"
echo "==> Now configure the GH_PAT repo secret (Settings -> Secrets -> Actions)"
echo "==> so the autofix workflow can push back to PR branches."
