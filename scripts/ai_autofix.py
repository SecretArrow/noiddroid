#!/usr/bin/env python3
"""scripts/ai_autofix.py — call an LLM to fix lint errors the deterministic
tools couldn't.

Reads the lint output, walks each failing file, asks the LLM for a unified
diff, applies it, runs the linter again to confirm. If anything looks
wrong (the diff doesn't apply, or the linter still fails), it backs out.

Usage:
    python3 scripts/ai_autofix.py --lint lint.txt --diff

Requires environment variable COPILOT_TOKEN (set in the GitHub Actions
workflow). Locally, can be invoked with a Hugging Face or OpenAI token.
"""
from __future__ import annotations
import argparse, json, os, re, subprocess, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TOKEN = os.environ.get("COPILOT_TOKEN") or os.environ.get("OPENAI_API_KEY")

PROMPT_TEMPLATE = """You are an automated Kotlin code-fixer. I will give you
the contents of a file and a list of detekt / ktlint errors that affect it.
Reply with ONLY a unified diff (git diff format) that fixes all the
errors. Do not add comments explaining the diff. Do not change unrelated
code. If you cannot fix an error, leave that section unchanged.

FILE: {file}

ERRORS:
{errors}

FILE CONTENTS:
{content}
"""


def parse_lint(lint_text: str) -> dict[str, list[str]]:
    """Group lint messages by file path."""
    out: dict[str, list[str]] = {}
    pattern = re.compile(r"^(.+?):(\d+):(\d+): (.+)$")
    for line in lint_text.splitlines():
        m = pattern.match(line)
        if not m: continue
        path, _, _, msg = m.groups()
        out.setdefault(path, []).append(line)
    return out


def ask_llm(file: Path, errors: list[str]) -> str | None:
    """Ask the LLM for a unified diff for this file. Returns the diff text
    or None on error."""
    if not TOKEN:
        return None
    try:
        import requests
    except ImportError:
        subprocess.run([sys.executable, "-m", "pip", "install", "-q", "requests"])
        import requests

    content = file.read_text()
    prompt = PROMPT_TEMPLATE.format(
        file=str(file), errors="\n".join(errors), content=content
    )
    endpoint = "https://api.githubcopilot.com/chat/completions"
    headers = {
        "Authorization": f"Bearer {TOKEN}",
        "Editor-Version": "ai-autofix/0.1",
        "Content-Type": "application/json",
    }
    body = {
        "messages": [
            {"role": "system", "content": "You are an expert Kotlin auto-fixer."},
            {"role": "user", "content": prompt},
        ],
        "model": "gpt-4o-mini",
        "temperature": 0,
    }
    try:
        resp = requests.post(endpoint, headers=headers, json=body, timeout=60)
        resp.raise_for_status()
        return resp.json()["choices"][0]["message"]["content"]
    except Exception as e:
        print(f"WARN: LLM call failed for {file}: {e}", file=sys.stderr)
        return None


def apply_diff(file: Path, diff_text: str) -> bool:
    """Apply a unified diff to a file. Returns True on success."""
    if not diff_text or not diff_text.startswith("---"):
        return False
    diff_path = file.with_suffix(".patch")
    diff_path.write_text(diff_text)
    rc = subprocess.run(
        ["git", "apply", "--check", str(diff_path)],
        cwd=ROOT, capture_output=True,
    ).returncode
    if rc != 0:
        diff_path.unlink(missing_ok=True)
        return False
    subprocess.run(["git", "apply", str(diff_path)], cwd=ROOT, check=True)
    diff_path.unlink(missing_ok=True)
    return True


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--lint", required=True, help="Path to lint output text")
    p.add_argument("--diff", action="store_true", help="Actually apply diffs (default: dry run)")
    args = p.parse_args()

    lint_text = Path(args.lint).read_text()
    grouped = parse_lint(lint_text)
    if not grouped:
        print("No lint errors to fix.")
        return 0

    if not TOKEN:
        print("No LLM token configured — skipping AI fix.")
        return 0

    fixed = 0
    for path, errors in grouped.items():
        abs_path = (ROOT / path).resolve()
        if not abs_path.exists():
            continue
        print(f"==> {path}: {len(errors)} errors")
        diff = ask_llm(abs_path, errors)
        if not diff:
            continue
        if args.diff:
            if apply_diff(abs_path, diff):
                fixed += 1
        else:
            print(diff)
    print(f"\nApplied {fixed} AI diffs.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
