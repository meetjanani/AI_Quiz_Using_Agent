#!/usr/bin/env python3
"""
Fetch a single Jira story by key and prepare it as a ready-to-use Copilot agent context.

Usage:
    python3 tools/jira_context_sync/implement_story.py --story KAN-1

What it does:
  1. Reads credentials from local.properties or env vars (same as fetch_jira_stories.py)
  2. Fetches the single issue from Jira REST API
  3. Writes docs/jira/current_story.md   <- rich Copilot context
  4. Writes docs/jira/current_story.json <- raw data
  5. Prints the exact prompt to paste into Copilot Agent chat
"""

from __future__ import annotations

import argparse
import base64
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, List, Optional


# ─────────────────────────────────────────────────────────────────────────────
# Credential loading — same logic as fetch_jira_stories.py
# ─────────────────────────────────────────────────────────────────────────────

def _env(name: str) -> str:
    return os.environ.get(name, "").strip()


def _read_local_properties() -> Dict[str, str]:
    candidates = [
        Path.cwd() / "local.properties",
        Path(__file__).resolve().parents[2] / "local.properties",
    ]
    props: Dict[str, str] = {}
    for path in candidates:
        if not path.exists():
            continue
        for raw_line in path.read_text(encoding="utf-8").splitlines():
            line = raw_line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            k, v = line.split("=", 1)
            props[k.strip()] = v.strip()
        if props:
            break
    return props


def _pick(env_names: List[str], props: Dict[str, str], prop_names: List[str]) -> str:
    for n in env_names:
        v = _env(n)
        if v:
            return v
    for n in prop_names:
        v = props.get(n, "").strip()
        if v:
            return v
    return ""


def load_credentials() -> Dict[str, str]:
    props = _read_local_properties()
    domain = _pick(["JIRA_DOMAIN"], props, ["jira.domain"])
    email  = _pick(["JIRA_EMAIL", "JIRA_USERNAME"], props, ["jira.email", "jira.username"])
    token  = _pick(["JIRA_API_TOKEN"], props, ["jira.api.token"])

    missing = [n for n, v in [("JIRA_DOMAIN", domain), ("JIRA_EMAIL", email), ("JIRA_API_TOKEN", token)] if not v]
    if missing:
        raise ValueError(
            f"Missing credentials: {', '.join(missing)}\n"
            "Add to local.properties:\n"
            "  jira.domain=your-company.atlassian.net\n"
            "  jira.email=you@company.com\n"
            "  jira.api.token=your_token"
        )

    domain = domain.replace("https://", "").rstrip("/")
    return {"domain": domain, "email": email, "token": token}


# ─────────────────────────────────────────────────────────────────────────────
# Jira fetch — single issue
# ─────────────────────────────────────────────────────────────────────────────

def fetch_issue(domain: str, email: str, token: str, issue_key: str) -> Dict:
    auth = base64.b64encode(f"{email}:{token}".encode()).decode()
    fields = "summary,description,issuetype,status,priority,assignee,labels,components,parent,created,updated"
    url = f"https://{domain}/rest/api/3/issue/{urllib.parse.quote(issue_key, safe='')}?fields={fields}"

    req = urllib.request.Request(
        url,
        method="GET",
        headers={"Authorization": f"Basic {auth}", "Accept": "application/json"},
    )
    try:
        with urllib.request.urlopen(req, timeout=20) as resp:
            return json.loads(resp.read().decode())
    except urllib.error.HTTPError as exc:
        detail = exc.read().decode(errors="replace")
        raise RuntimeError(f"Jira API {exc.code}: {detail}") from exc
    except urllib.error.URLError as exc:
        raise RuntimeError(f"Network error: {exc}") from exc


# ─────────────────────────────────────────────────────────────────────────────
# Parse ADF description → plain text
# ─────────────────────────────────────────────────────────────────────────────

def _extract_text(node: Optional[Dict]) -> str:
    if not node or not isinstance(node, dict):
        return ""
    parts: List[str] = []

    def walk(n: Dict):
        node_type = n.get("type", "")
        # Add blank line before headings and list items for readability
        if node_type in ("heading", "bulletList", "orderedList", "listItem", "paragraph"):
            if parts and parts[-1] != "":
                parts.append("")
        for child in n.get("content") or []:
            if isinstance(child, dict):
                walk(child)
        text = n.get("text")
        if isinstance(text, str) and text.strip():
            parts.append(text.strip())

    walk(node)
    return "\n".join(p for p in parts).strip()


def parse_issue(raw: Dict) -> Dict:
    f = raw.get("fields", {})
    return {
        "key":        raw.get("key", ""),
        "type":       (f.get("issuetype") or {}).get("name", ""),
        "summary":    f.get("summary") or "",
        "status":     (f.get("status") or {}).get("name", ""),
        "priority":   (f.get("priority") or {}).get("name", ""),
        "assignee":   (f.get("assignee") or {}).get("displayName", "Unassigned"),
        "labels":     f.get("labels") or [],
        "components": [c.get("name", "") for c in (f.get("components") or []) if c.get("name")],
        "parent":     (f.get("parent") or {}).get("key", ""),
        "created":    f.get("created") or "",
        "updated":    f.get("updated") or "",
        "description": _extract_text(f.get("description")),
    }


# ─────────────────────────────────────────────────────────────────────────────
# Write output files
# ─────────────────────────────────────────────────────────────────────────────

def write_outputs(output_dir: Path, issue: Dict) -> Path:
    output_dir.mkdir(parents=True, exist_ok=True)

    # JSON snapshot
    json_path = output_dir / "current_story.json"
    json_path.write_text(
        json.dumps({"generatedAt": datetime.now(timezone.utc).isoformat(), "issue": issue}, indent=2),
        encoding="utf-8",
    )

    # Markdown — this is what Copilot reads
    md_path = output_dir / "current_story.md"
    lines = [
        f"# Jira {issue['type']}: {issue['key']} — {issue['summary']}",
        "",
        "## Metadata",
        f"- **Key:** {issue['key']}",
        f"- **Type:** {issue['type']}",
        f"- **Status:** {issue['status']}",
        f"- **Priority:** {issue['priority']}",
        f"- **Assignee:** {issue['assignee']}",
        f"- **Labels:** {', '.join(issue['labels']) if issue['labels'] else 'None'}",
        f"- **Components:** {', '.join(issue['components']) if issue['components'] else 'None'}",
        f"- **Parent:** {issue['parent'] or 'None'}",
        f"- **Updated:** {issue['updated']}",
        "",
        "## Full Description & Acceptance Criteria",
        "",
        issue["description"] or "_No description provided._",
        "",
        "---",
        f"_Fetched from Jira at {datetime.now(timezone.utc).isoformat()}_",
    ]
    md_path.write_text("\n".join(lines), encoding="utf-8")

    return md_path


# ─────────────────────────────────────────────────────────────────────────────
# Print Copilot agent prompt
# ─────────────────────────────────────────────────────────────────────────────

def print_copilot_prompt(issue: Dict, md_path: Path):
    separator = "═" * 66
    print(f"\n╔{separator}╗")
    print(f"║  PASTE THIS INTO COPILOT AGENT CHAT (Agent mode)           ║")
    print(f"╚{separator}╝\n")
    print(
        f"@workspace Implement Jira {issue['type']} `{issue['key']}` — {issue['summary']}\n\n"
        f"Full requirements are in `{md_path}`.\n"
        f"Architecture rules are in `.github/scripts/story_agent_prompt.md`.\n\n"
        f"Please implement all required files end-to-end following "
        f"the existing MVVM + Compose + StateFlow architecture."
    )
    print(f"\n{'─' * 68}\n")


# ─────────────────────────────────────────────────────────────────────────────
# Entry point
# ─────────────────────────────────────────────────────────────────────────────

def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Fetch a single Jira story and prepare it for Copilot agent implementation."
    )
    parser.add_argument("--story", required=True, help="Jira issue key, e.g. KAN-1")
    parser.add_argument("--output-dir", default="docs/jira", help="Output directory")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    issue_key = args.story.strip().upper()

    print(f"\n🔍 Fetching Jira story: {issue_key} ...")

    try:
        creds = load_credentials()
    except ValueError as exc:
        print(f"\n❌ {exc}")
        return 2

    try:
        raw = fetch_issue(
            domain=creds["domain"],
            email=creds["email"],
            token=creds["token"],
            issue_key=issue_key,
        )
    except RuntimeError as exc:
        print(f"\n❌ {exc}")
        return 1

    issue = parse_issue(raw)
    print(f"✅ [{issue['key']}] {issue['summary']}")
    print(f"   Type: {issue['type']} | Status: {issue['status']} | Priority: {issue['priority']}")

    md_path = write_outputs(Path(args.output_dir), issue)
    print(f"\n📄 Story written to: {md_path}")
    print(f"📄 JSON written to:  {Path(args.output_dir) / 'current_story.json'}")

    print_copilot_prompt(issue, md_path)
    return 0


if __name__ == "__main__":
    sys.exit(main())

