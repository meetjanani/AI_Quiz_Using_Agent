# Speaker Notes — Agentic AI Code Review Demo

## Slide 1 — Title
"Today I want to show an agentic AI use case I built around Android pull request reviews. The goal is to reduce review effort, catch issues earlier, and return line-level suggestions directly in GitHub."

## Slide 2 — Why this matters
"Code reviews are high value but also repetitive. Reviewers repeatedly check for lint issues, hardcoded values, magic numbers, architecture drift, security gaps, and weak test coverage. That is a perfect place for an AI agent to help before a human spends time."

## Slide 3 — Why this is agentic and not just prompt-based
"This solution does more than answer a question. It reacts to a PR event, collects diff context, runs lint and coverage, routes work through specialized reviewer personas, structures the output, and pushes the result back into GitHub. That makes it agentic: context-aware, goal-driven, and workflow-integrated."

## Slide 4 — End-to-end flow
"When a PR opens or gets updated, GitHub Actions checks out the code, builds a diff, runs tests and coverage, collects Android lint output, then calls the master reviewer. The master reviewer behaves like a lead engineer and delegates mentally across architecture, Compose UI, security, and test quality domains. Findings are returned in XML, mapped to changed lines, and posted as GitHub review comments."

## Slide 5 — Architecture components
"There are 3 layers here: orchestration, intelligence, and output. Orchestration is the GitHub Action and Python runner. Intelligence is the master reviewer prompt plus specialist prompt files. Output is structured findings and GitHub comments that developers can act on immediately."

## Slide 6 — What the agent checks
"The agent is tuned for Android realities: lint rules, Kotlin style, hardcoded strings and values, BuildConfig vs resources, coroutine/threading issues, architecture boundaries, security configuration, and business-logic test coverage below 75 percent."

## Slide 7 — Live demo flow
"For the demo, I will show a pull request with intentionally inserted issues. Then I’ll trigger the workflow and show how comments appear on the exact changed lines. I’ll also show that coverage alerts are surfaced separately for repository or use-case files."

## Slide 8 — Business impact
"This improves consistency, accelerates feedback, and gives teams a reusable review baseline. Engineers spend less time on repetitive checks and more time on design and product logic. Managers get better quality signals earlier in the SDLC."

## Slide 9 — Rollout plan
"The easiest rollout is to start with review-only mode on one Android repo, monitor false positives for 1–2 sprints, tune the prompts, then scale the same pattern to other projects."

## Slide 10 — Ask / close
"My ask is to pilot this as a standard PR quality gate for Android work. It is lightweight to adopt because it plugs into GitHub Actions and uses prompt files that can be versioned with the codebase."

## Backup Q&A talking points
- Why not replace human reviewers? → This is meant to amplify them, not replace them.
- Why multiple agents? → Specialization improves review quality and makes findings easier to tune.
- Why structured XML? → It keeps the output machine-readable and comment-ready.
- Why line-level comments? → Developers can act immediately without reading a long generic summary.
- Why this matters to managers? → Faster cycle time, better consistency, less escaped quality risk.

