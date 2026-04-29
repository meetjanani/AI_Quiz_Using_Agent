#!/usr/bin/env python3
"""Sync Jira user stories into local project files for Copilot @workspace context.

This script is intentionally local-only:
- Reads credentials from environment variables (not from GitHub Actions).
- Writes Jira data to docs/jira/ so Copilot chat can read it from the workspace.
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
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, Iterable, List, Optional


DEFAULT_ISSUE_TYPES = ["Story", "Task", "Bug", "Epic"]


@dataclass
class JiraConfig:
    domain: str
    email: str
    api_token: str
    project_key: str


def _env(name: str) -> str:
    return os.environ.get(name, "").strip()


def _read_local_properties() -> Dict[str, str]:
    """Read project local.properties as optional fallback for local-only usage."""
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
            key, value = line.split("=", 1)
            props[key.strip()] = value.strip()
        if props:
            break
    return props


def _pick_value(env_names: List[str], local_props: Dict[str, str], local_names: List[str]) -> str:
    for env_name in env_names:
        value = _env(env_name)
        if value:
            return value
    for local_name in local_names:
        value = local_props.get(local_name, "").strip()
        if value:
            return value
    return ""


def load_config(project_key_override: Optional[str]) -> JiraConfig:
    local_props = _read_local_properties()

    domain = _pick_value(
        env_names=["JIRA_DOMAIN"],
        local_props=local_props,
        local_names=["jira.domain", "JIRA_DOMAIN"],
    )
    email = _pick_value(
        env_names=["JIRA_EMAIL", "JIRA_USERNAME"],
        local_props=local_props,
        local_names=["jira.email", "jira.username", "JIRA_EMAIL", "JIRA_USERNAME"],
    )
    api_token = _pick_value(
        env_names=["JIRA_API_TOKEN"],
        local_props=local_props,
        local_names=["jira.api.token", "JIRA_API_TOKEN"],
    )
    project_key = (project_key_override or _pick_value(
        env_names=["JIRA_PROJECT_KEY"],
        local_props=local_props,
        local_names=["jira.project.key", "JIRA_PROJECT_KEY"],
    )).strip()


    missing = [
        name
        for name, value in [
            ("JIRA_DOMAIN", domain),
            ("JIRA_EMAIL (or JIRA_USERNAME)", email),
            ("JIRA_API_TOKEN", api_token),
            ("JIRA_PROJECT_KEY", project_key),
        ]
        if not value
    ]
    if missing:
        raise ValueError("Missing required environment variables: " + ", ".join(missing))

    if domain.startswith("https://"):
        domain = domain.replace("https://", "", 1)
    if domain.endswith("/"):
        domain = domain[:-1]

    return JiraConfig(domain=domain, email=email, api_token=api_token, project_key=project_key)


def build_auth_header(email: str, api_token: str) -> str:
    token = base64.b64encode(f"{email}:{api_token}".encode("utf-8")).decode("ascii")
    return f"Basic {token}"


def jira_search(
    config: JiraConfig,
    jql: str,
    max_results: int,
    fields: Iterable[str],
) -> List[Dict]:
    auth_header = build_auth_header(config.email, config.api_token)
    base_url = f"https://{config.domain}/rest/api/3/search/jql"

    issues: List[Dict] = []
    start_at = 0
    page_size = 100

    while True:
        query_params = urllib.parse.urlencode(
            {
                "jql": jql,
                "startAt": start_at,
                "maxResults": page_size,
                "fields": ",".join(fields),
            }
        )
        request = urllib.request.Request(
            f"{base_url}?{query_params}",
            method="GET",
            headers={
                "Authorization": auth_header,
                "Accept": "application/json",
            },
        )

        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                data = json.loads(response.read().decode("utf-8"))
        except urllib.error.HTTPError as exc:
            detail = exc.read().decode("utf-8", errors="replace")
            raise RuntimeError(f"Jira API error {exc.code}: {detail}") from exc
        except urllib.error.URLError as exc:
            raise RuntimeError(f"Network error while contacting Jira: {exc}") from exc

        page_issues = data.get("issues", [])
        issues.extend(page_issues)

        total = int(data.get("total", 0))
        fetched = len(issues)
        print(f"Fetched {fetched}/{total} issues...")

        if not page_issues or fetched >= total or fetched >= max_results:
            break

        start_at += len(page_issues)

    if len(issues) > max_results:
        return issues[:max_results]
    return issues


def issue_to_compact(issue: Dict) -> Dict:
    fields = issue.get("fields", {})
    parent = fields.get("parent") or {}
    assignee = fields.get("assignee") or {}
    status = fields.get("status") or {}
    priority = fields.get("priority") or {}

    return {
        "key": issue.get("key", ""),
        "type": ((fields.get("issuetype") or {}).get("name") or ""),
        "summary": fields.get("summary") or "",
        "status": status.get("name") or "",
        "priority": priority.get("name") or "",
        "assignee": assignee.get("displayName") or "Unassigned",
        "labels": fields.get("labels") or [],
        "components": [c.get("name", "") for c in (fields.get("components") or []) if c.get("name")],
        "parent": parent.get("key") or "",
        "created": fields.get("created") or "",
        "updated": fields.get("updated") or "",
        "description": extract_plain_description(fields.get("description")),
    }


def extract_plain_description(description_field: Optional[Dict]) -> str:
    if not description_field or not isinstance(description_field, dict):
        return ""

    parts: List[str] = []

    def walk(node: Dict):
        content = node.get("content")
        if isinstance(content, list):
            for child in content:
                if isinstance(child, dict):
                    walk(child)
        text = node.get("text")
        if isinstance(text, str):
            parts.append(text)

    walk(description_field)
    return " ".join(part.strip() for part in parts if part.strip())


def write_outputs(output_dir: Path, compact_issues: List[Dict], query_meta: Dict):
    output_dir.mkdir(parents=True, exist_ok=True)

    json_path = output_dir / "user_stories.json"
    md_path = output_dir / "user_stories.md"
    meta_path = output_dir / "user_stories_meta.json"

    payload = {
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "count": len(compact_issues),
        "issues": compact_issues,
    }

    json_path.write_text(json.dumps(payload, indent=2), encoding="utf-8")
    meta_path.write_text(json.dumps(query_meta, indent=2), encoding="utf-8")

    lines = [
        "# Jira User Stories Context",
        "",
        f"- Generated: {payload['generatedAt']}",
        f"- Total issues: {payload['count']}",
        f"- Project: {query_meta.get('projectKey', '')}",
        f"- JQL: `{query_meta.get('jql', '')}`",
        "",
        "## Issues",
        "",
    ]

    for issue in compact_issues:
        lines.extend(
            [
                f"### {issue['key']} - {issue['summary']}",
                f"- Type: {issue['type']}",
                f"- Status: {issue['status']}",
                f"- Priority: {issue['priority']}",
                f"- Assignee: {issue['assignee']}",
                f"- Labels: {', '.join(issue['labels']) if issue['labels'] else 'None'}",
                f"- Components: {', '.join(issue['components']) if issue['components'] else 'None'}",
                f"- Parent: {issue['parent'] or 'None'}",
                f"- Updated: {issue['updated'] or 'N/A'}",
                f"- Description: {issue['description'][:500] if issue['description'] else 'N/A'}",
                "",
            ]
        )

    md_path.write_text("\n".join(lines), encoding="utf-8")

    print(f"Wrote: {json_path}")
    print(f"Wrote: {md_path}")
    print(f"Wrote: {meta_path}")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Fetch Jira stories into local workspace files.")
    parser.add_argument("--project-key", default="", help="Override JIRA_PROJECT_KEY")
    parser.add_argument("--max-results", type=int, default=500, help="Maximum issues to fetch")
    parser.add_argument("--days", type=int, default=0, help="Only include issues updated in last N days")
    parser.add_argument(
        "--types",
        default=",".join(DEFAULT_ISSUE_TYPES),
        help="Comma-separated Jira issue types to include",
    )
    parser.add_argument(
        "--output-dir",
        default="docs/jira",
        help="Directory to write output files",
    )
    return parser.parse_args()


def build_jql(project_key: str, types: List[str], days: int) -> str:
    quoted_types = ",".join(f'"{t.strip()}"' for t in types if t.strip())
    jql = f"project = {project_key} AND issuetype in ({quoted_types})"
    if days > 0:
        jql += f" AND updated >= -{days}d"
    jql += " ORDER BY updated DESC"
    return jql


def main() -> int:
    args = parse_args()

    try:
        config = load_config(args.project_key)
    except ValueError as exc:
        print(str(exc))
        print("\nSet env vars locally (zsh example):")
        print("export JIRA_DOMAIN=your-company.atlassian.net")
        print("export JIRA_EMAIL=you@company.com")
        print("export JIRA_API_TOKEN=your_api_token")
        print("export JIRA_PROJECT_KEY=ABC")
        print("\nOr put these keys in local.properties:")
        print("jira.domain=your-company.atlassian.net")
        print("jira.email=you@company.com")
        print("jira.api.token=your_api_token")
        print("jira.project.key=ABC")
        return 2

    types = [t.strip() for t in args.types.split(",") if t.strip()]
    jql = build_jql(config.project_key, types, args.days)

    fields = [
        "summary",
        "description",
        "issuetype",
        "status",
        "priority",
        "assignee",
        "labels",
        "components",
        "parent",
        "created",
        "updated",
    ]

    print(f"Connecting to Jira domain: {config.domain}")
    print(f"Project: {config.project_key}")
    print(f"JQL: {jql}")

    try:
        raw_issues = jira_search(config, jql=jql, max_results=args.max_results, fields=fields)
    except RuntimeError as exc:
        print(f"Failed to fetch Jira issues: {exc}")
        return 1

    compact_issues = [issue_to_compact(issue) for issue in raw_issues]
    write_outputs(
        output_dir=Path(args.output_dir),
        compact_issues=compact_issues,
        query_meta={
            "projectKey": config.project_key,
            "jql": jql,
            "maxResults": args.max_results,
            "days": args.days,
            "types": types,
        },
    )

    print("Done. You can now use these files in Copilot chat @workspace context.")
    return 0


if __name__ == "__main__":
    sys.exit(main())

