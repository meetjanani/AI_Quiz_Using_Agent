# Demo Runbook — 7 Minute Manager Presentation

## 0. Before the meeting
Keep these ready:
- `docs/presentation/Agentic_AI_Code_Review_Demo.pptx`
- GitHub PR showing intentional review issues
- GitHub Actions run page
- PR conversation / Files changed tab with agent comments

## 1. Opening (30 seconds)
"I built an agentic AI reviewer for Android pull requests. It doesn't just generate text — it participates in the engineering workflow and adds actionable review comments directly in GitHub."

## 2. Problem statement (45 seconds)
- Manual PR review is valuable but repetitive.
- Common issues: lint, hardcoded strings, magic numbers, threading, test gaps.
- Senior engineers spend time on repeatable checks instead of higher-order design review.

## 3. Agentic framing (60 seconds)
Use this exact line:
"This is agentic because it has a goal, gathers context, reasons across specialist roles, and performs an action back in the workflow."

## 4. Show the flow diagram (60–90 seconds)
Walk left to right:
1. PR opened
2. GitHub Action triggered
3. Diff + lint + coverage collected
4. Master reviewer orchestrates specialist reviewers
5. Findings returned in structured XML
6. Comments posted to exact file lines in the PR

## 5. Live GitHub demo (2–3 minutes)
Recommended order:
1. Open the PR
2. Open `Files changed`
3. Point to intentional issue in Kotlin/Compose file
4. Show inline AI comment on the exact changed line
5. Show another comment for a different finding
6. Show coverage alert comment for repository/use-case file if present
7. Open Actions tab and show the workflow run

## 6. Business impact (45 seconds)
- Faster feedback cycle
- More consistent review quality
- Reusable prompt-based governance
- Better scale across repos

## 7. Closing ask (20 seconds)
"I’d like to pilot this on one Android repo/team and measure review turnaround time, usefulness of findings, and reduction in escaped issues."

## Backup answers
### If asked: Why not just use lint?
"Lint is necessary but limited. This agent combines lint, coverage, architecture reasoning, security checks, and reviewer-style guidance in one workflow."

### If asked: Why not replace reviewers?
"This is an assistant, not a replacement. It removes repetitive review work so humans can focus on design, trade-offs, and product correctness."

### If asked: Can this scale?
"Yes. The workflow is reusable and the agent prompts are version-controlled, so the same pattern can be applied to other Android repositories."

