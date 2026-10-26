#!/usr/bin/env python3
"""scripts/set_github_secret.py — set a GitHub Actions secret via the API.

Uses pynacl to encrypt the secret value with the repo's public key, then
PUTs it to /repos/{owner}/{repo}/actions/secrets/{name}.

Usage:
    GH_TOKEN=ghp_xxx python3 scripts/set_github_secret.py \
        --repo SecretArrow/noiddroid \
        --name GH_PAT \
        --value "$GH_TOKEN"
"""
from __future__ import annotations
import argparse, base64, json, os, sys, urllib.request
from pathlib import Path

# Force user site-packages onto the path before importing nacl
sys.path.insert(0, "/home/z/.local/lib/python3.13/site-packages")
import nacl.encoding  # noqa: E402
import nacl.public  # noqa: E402


def api(method: str, path: str, token: str, body: dict | None = None) -> dict:
    url = f"https://api.github.com{path}"
    data = json.dumps(body).encode() if body else None
    req = urllib.request.Request(
        url, method=method, data=data,
        headers={
            "Authorization": f"Bearer {token}",
            "Accept": "application/vnd.github+json",
            "Content-Type": "application/json",
            "X-GitHub-Api-Version": "2022-11-28",
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            body_text = resp.read()
            return json.loads(body_text) if body_text else {}
    except urllib.error.HTTPError as e:
        return {"error": f"{e.code} {e.reason}", "body": e.read().decode(errors="replace")}


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--repo", required=True, help="owner/repo")
    p.add_argument("--name", required=True, help="secret name (e.g. GH_PAT)")
    p.add_argument("--value", required=True, help="secret value")
    args = p.parse_args()

    token = os.environ.get("GH_TOKEN") or os.environ.get("GITHUB_TOKEN")
    if not token:
        print("ERROR: GH_TOKEN env var not set", file=sys.stderr)
        return 1

    print(f"==> Fetching public key for {args.repo}")
    pk = api("GET", f"/repos/{args.repo}/actions/secrets/public-key", token)
    if "error" in pk:
        print(f"ERROR: {pk}", file=sys.stderr)
        return 1
    print(f"    key_id={pk['key_id']}, key_len={len(pk['key'])} chars")

    print(f"==> Encrypting secret value ({len(args.value)} bytes) with libsodium")
    public_key = nacl.public.PublicKey(pk["key"].encode(), nacl.encoding.Base64Encoder())
    box = nacl.public.SealedBox(public_key)
    encrypted = box.encrypt(args.value.encode())
    encrypted_b64 = base64.b64encode(encrypted).decode()
    print(f"    encrypted_len={len(encrypted_b64)} chars")

    print(f"==> PUT /repos/{args.repo}/actions/secrets/{args.name}")
    result = api("PUT", f"/repos/{args.repo}/actions/secrets/{args.name}", token, {
        "encrypted_value": encrypted_b64,
        "key_id": pk["key_id"],
    })
    if "error" in result:
        print(f"ERROR: {result}", file=sys.stderr)
        return 1
    print(f"    OK — secret '{args.name}' set")
    return 0


if __name__ == "__main__":
    sys.exit(main())
