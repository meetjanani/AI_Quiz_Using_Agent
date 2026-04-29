# Jira Context Sync (Local Copilot Agent Mode)

This utility pulls Jira user stories into local project files so Copilot chat can use them via `@workspace`.

It is intentionally local-only (not GitHub Actions).

## What it generates

- `docs/jira/user_stories.json` - machine-friendly full context
- `docs/jira/user_stories.md` - human-friendly summary
- `docs/jira/user_stories_meta.json` - query metadata

## 1) Set environment variables (zsh)

```bash
export JIRA_DOMAIN="your-company.atlassian.net"
export JIRA_EMAIL="you@company.com"
export JIRA_API_TOKEN="your_api_token"
export JIRA_PROJECT_KEY="ABC"
```

## 2) Run sync

```bash
python3 tools/jira_context_sync/fetch_jira_stories.py
```

## 3) Use in Copilot chat (agent mode)

Examples:

- `@workspace summarize docs/jira/user_stories.md`
- `@workspace map docs/jira/user_stories.json to app modules`
- `@workspace propose sprint plan from docs/jira/user_stories.json`

## Common options

```bash
# Only updated in last 90 days
python3 tools/jira_context_sync/fetch_jira_stories.py --days 90

# Limit to specific issue types
python3 tools/jira_context_sync/fetch_jira_stories.py --types "Story,Task"

# Cap the number of fetched issues
python3 tools/jira_context_sync/fetch_jira_stories.py --max-results 300
```

## Optional alias

```bash
alias jira-sync='cd /Users/2000113075/AndroidStudioProjects/AI_Agent_Projects_Android/Dummy_Quiz_Using_Agent && python3 tools/jira_context_sync/fetch_jira_stories.py --days 180'
```

Then run:

```bash
jira-sync
```

## Security notes

- Do not commit your API token.
- Keep credentials in shell env or local secret manager.
- Output files may include issue descriptions; review before sharing externally.

