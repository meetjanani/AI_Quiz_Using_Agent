# 🎤 AI Code Review Agent — 20-Min Team Presentation Guide

> **Audience:** Technical + Non-Technical  
> **Time:** 20 minutes  
> **Presenter:** Meet Janani  
> **Goal:** Explain how an autonomous AI agent reviews Android PRs — no human reviewer needed

---

## 🗂️ SLIDE-BY-SLIDE BREAKDOWN

---

### 📌 SLIDE 1 — Title Slide (1 min)

**Title:** _"Meet My AI Code Review Agent"_  
**Subtitle:** _Automating Android PR Reviews with Gemini AI + GitHub Actions_

> 💬 **Say:** "Today I'll show you how I built an AI agent that reviews every Pull Request automatically — catching bugs, bad code, and missing best practices before any human even looks at it."

---

### 📌 SLIDE 2 — The Problem (2 min)

**Title:** _"Code Reviews Today — What's Wrong?"_

**For Non-Tech:** Show a funny image of a developer waiting for review feedback for 3 days.

**Points:**
- ⏳ Manual reviews take time — reviewer availability, timezone, workload
- 🤷 Reviews are inconsistent — depends who reviews
- 🔁 Same mistakes repeated across PRs — hardcoded strings, magic numbers, missing tests
- 🧠 Reviewer knowledge is siloed — only 1-2 people know certain areas

> 💬 **Say:** "Every team faces this. Reviews are slow, inconsistent, and miss the same things again and again."

---

### 📌 SLIDE 3 — The Solution (1 min)

**Title:** _"What If Your Agent Reviewed Every PR in 2 Minutes?"_

🤖 **An AI Agent that:**
- Runs automatically on every PR
- Reviews code like a Staff Engineer
- Comments on the **exact line** of the problem
- Uses your team's own past review comments as style guide
- Never skips, never gets tired, never misses a magic number

> 💬 **Say:** "I built exactly this. Let me show you how."

---

### 📌 SLIDE 4 — Architecture Overview (2 min)

**Title:** _"How It Works — Bird's Eye View"_

```
Developer opens PR
        │
        ▼
GitHub Actions triggers
        │
        ▼
┌───────────────────────────────────────┐
│         Job 1: fetch-review-history   │
│  Fetches last 90 days of team PR      │
│  comments (humans only, no bots)      │
│  Saves → recent_pr_comments.txt       │
└───────────────────┬───────────────────┘
                    │
                    ▼
┌───────────────────────────────────────┐
│         Job 2: ai-code-review         │
│  1. Generates PR diff                 │
│  2. Runs Android Lint                 │
│  3. Runs Unit Tests + Coverage        │
│  4. Sends everything to Gemini AI     │
│  5. Posts review comments on PR       │
└───────────────────────────────────────┘
```

> 💬 **Say:** "Two jobs run in sequence. First job learns from your team's history. Second job does the actual review and comments on GitHub."

---

### 📌 SLIDE 5 — Files Involved (3 min)

**Title:** _"The 6 Files That Power This Agent"_

| # | File | Role | Used? |
|---|------|------|-------|
| 1 | `.github/workflows/ai-pr-reviewer.yml` | 🚀 **Trigger & orchestrator** — runs on every PR | ✅ **CORE** |
| 2 | `.github/scripts/run_agent.py` | 🧠 **Brain** — fetches history, builds prompt, calls Gemini, posts comments | ✅ **CORE** |
| 3 | `.github/scripts/master-reviewer-agent.md` | 📋 **System prompt** — tells Gemini to act as Lead Staff Engineer | ✅ **CORE** |
| 4 | `.github/scripts/arch-logic-agent.md` | 🏗️ Sub-agent prompt — Architecture / ViewModel / Repository rules | ✅ Active |
| 5 | `.github/scripts/compose-ui-agent.md` | 🎨 Sub-agent prompt — Compose UI rules | ✅ Active |
| 6 | `.github/scripts/security-config-agent.md` | 🔒 Sub-agent prompt — Security / API keys / Manifest rules | ✅ Active |
| 7 | `.github/scripts/test-qa-agent.md` | 🧪 Sub-agent prompt — Test coverage / QA rules | ✅ Active |
| 8 | `.github/scripts/check_coverage.py` | 📊 Reads JaCoCo XML and prints per-file coverage table | ✅ Active |

**❌ Files NOT used by the agent pipeline:**
| File | Why Not Used |
|------|-------------|
| `.github/scripts/test_report.py` | Generates HTML test report locally — not called by the workflow |
| `app/custom_agents/` | Empty folder — not used |
| `app/code-review-agent.md` | Old/draft version — replaced by `master-reviewer-agent.md` |
| `app/Quiz_generation_Agent_Created_By_Meet.md` | Documentation only — not part of the pipeline |

> 💬 **Say:** "You only need 8 files. Everything else is either documentation or unused drafts."

---

### 📌 SLIDE 6 — The Master Agent (2 min)

**Title:** _"master-reviewer-agent.md — The Brain's Instructions"_

Think of it like a **job description for the AI**:

```
Role: Lead Android Staff Engineer
Operating Mode: Review Only (no code edits)

Delegates to:
  → arch-logic-agent.md    (ViewModel, Repository, Coroutines)
  → compose-ui-agent.md    (Compose UI, accessibility, performance)
  → security-config-agent.md (API keys, Manifest, BuildConfig)
  → test-qa-agent.md       (Test coverage, edge cases)

Checks for:
  ✓ Magic numbers
  ✓ Hardcoded strings/colors
  ✓ Kotlin lint violations
  ✓ Android lint rules
  ✓ Code formatting
  ✓ Architecture violations
```

> 💬 **Non-tech explanation:** "Imagine I hired a very strict senior developer. I gave them a detailed rulebook. That rulebook is this file. The AI reads it and follows it exactly."

---

### 📌 SLIDE 7 — Historical Context Feature (2 min)

**Title:** _"It Learns From Your Team's Own Reviews"_

**How it works:**
1. Fetches **last 90 days** of PR comments from GitHub API
2. **Excludes bot comments** (only human reviewers)
3. Collects 3 types:
   - PR conversation comments
   - Review summary (Approve / Request Changes)
   - Inline file comments
4. Passes up to **120 comments** as context to Gemini

**Why this matters:**
> "If your team always says 'use Dependency Injection here' — the agent will start saying the same thing on your PRs."

💡 _It personalizes to your team's coding culture._

---

### 📌 SLIDE 8 — LIVE DEMO (4 min)

**Title:** _"Live Demo — Watch It Catch Bugs"_

**Show on screen:**
1. Open `MainActivity.kt` — point out the 4 intentional bugs:
   - Hardcoded API key: `"API_Key hard coded value"` (line 22)
   - Hardcoded log tag: `"MainActivityDebug"` (line 35)
   - Magic number: `5000` (line 38)
   - Hardcoded string: `"Welcome to the Quiz App!"` (line 41)

2. Open a PR on GitHub

3. Watch the GitHub Actions workflow run:
   - ✅ `fetch-review-history` job
   - ✅ `ai-code-review` job

4. Show the review comments on the PR — **pinned to exact lines**

> 💬 **Say:** "No human reviewed this. The agent found all 4 issues and commented on the exact line — like a human would."

---

### 📌 SLIDE 9 — Comment Format on GitHub (1 min)

**Title:** _"What the Comments Look Like"_

Each finding is posted like this:

```
🟠 [High] Code Review Finding

📍 Location: MainActivity.kt:35 — onCreate()

🐛 Problem: Hardcoded log tag "MainActivityDebug" used directly in Log.d()

💥 Impact: Inconsistent log filtering; log tag not refactorable; 
           violates Android logging conventions

🔧 Suggested Fix:
companion object {
    private const val TAG = "MainActivity"
}
Log.d(TAG, "onCreate called")
```

- **Inline comments** → pinned to exact line in the diff
- **Fallback comments** → posted as general PR comments if line not in diff

---

### 📌 SLIDE 10 — Coverage Alerts (1 min)

**Title:** _"It Also Tracks Test Coverage"_

For business logic files (Repository, UseCase, Domain):

```
⚠️ Coverage Alert — Below 75% Threshold
  File: app/src/.../QuizRepository.kt
  Current Coverage: 42.00% (21/50 lines)
  Missing to reach 75%: 33.00%
  Related test files:
    - app/src/test/.../QuizRepositoryTest.kt
  Suggested action: Add tests for uncovered branches
```

> 💬 **Say:** "It doesn't just review code quality — it also tells you where your test coverage is weak."

---

### 📌 SLIDE 11 — Execution Flow Diagram (1 min)

**Title:** _"End-to-End Flow"_

```
Developer pushes code
         │
         ▼
  PR opened on GitHub
         │
         ▼
  GitHub Actions starts
         │
    ┌────┴────────────────────────────┐
    │  Job 1: fetch-review-history    │
    │  • gh API → fetch last 90 days  │
    │  • Filter: exclude bots         │
    │  • Save: recent_pr_comments.txt │
    │  • Upload as GitHub artifact    │
    └────────────────┬────────────────┘
                     │ artifact passed
                     ▼
    ┌────────────────────────────────────┐
    │  Job 2: ai-code-review             │
    │  • git diff (pr_diff.patch)        │
    │  • Android lint (lint_output.txt)  │
    │  • JaCoCo coverage XML             │
    │  • Download history artifact       │
    │  • Build Gemini prompt             │
    │  • Call Gemini 2.5 Flash API       │
    │  • Parse XML response              │
    │  • Post inline + fallback comments │
    └────────────────────────────────────┘
```

---

### 📌 SLIDE 12 — Key Benefits (1 min)

**Title:** _"Why This Matters"_

| Without Agent | With Agent |
|---------------|------------|
| Wait 1-3 days for review | Review in ~2 minutes |
| Inconsistent quality | Same bar every time |
| Same mistakes repeated | Caught automatically |
| Reviewer knowledge siloed | Rules encoded for everyone |
| 0% coverage blind spots | Coverage alerts per file |
| No team style learning | Learns from past reviews |

---

### 📌 SLIDE 13 — How To Use In Any Project (1 min)

**Title:** _"How To Add This To Your Project"_

**Only 4 steps:**

```bash
# Step 1: Copy these 3 files to your project
.github/workflows/ai-pr-reviewer.yml
.github/scripts/run_agent.py
.github/scripts/master-reviewer-agent.md

# Step 2: Add your API key to GitHub Secrets
Settings → Secrets → LLM_API_KEY = your_gemini_key

# Step 3: Customize the agent prompts (optional)
Edit master-reviewer-agent.md for your project rules

# Step 4: Open a PR and watch it work
```

---

### 📌 SLIDE 14 — Q&A (1 min)

**Title:** _"Questions?"_

**Common questions to prepare for:**

❓ _Is this free?_  
→ Gemini API has a generous free tier. GitHub Actions is free for public repos.

❓ _Can it auto-fix the code?_  
→ Currently review-only. Auto-fix mode is disabled intentionally for safety.

❓ _What if the AI is wrong?_  
→ Developers still decide what to fix. The agent is a suggestion tool, not a gate.

❓ _Can it work for other languages (iOS, web)?_  
→ Yes — just update the agent `.md` prompt files with language-specific rules.

❓ _Is our code safe? Does it go to Google?_  
→ Only the changed diff is sent, not the full codebase. Same as using Copilot.

---

## ⏱️ TIME BUDGET

| Slide | Topic | Time |
|-------|-------|------|
| 1 | Title | 1 min |
| 2 | Problem | 2 min |
| 3 | Solution | 1 min |
| 4 | Architecture | 2 min |
| 5 | Files Involved | 3 min |
| 6 | Master Agent | 2 min |
| 7 | Historical Context | 2 min |
| 8 | **LIVE DEMO** | 4 min |
| 9 | Comment Format | 1 min |
| 10-11 | Coverage + Flow | 1 min |
| 12-13 | Benefits + Setup | 1 min |
| 14 | Q&A | 1 min |
| **Total** | | **21 min** |

---

## 🧩 FILES REFERENCE CARD (Print This)

### ✅ ACTIVE FILES (The Agent Pipeline)

```
.github/
  workflows/
    ai-pr-reviewer.yml          ← Workflow: runs on every PR
  scripts/
    run_agent.py                ← Brain: orchestrates everything
    master-reviewer-agent.md    ← System prompt for Gemini
    arch-logic-agent.md         ← Sub-agent: Architecture rules
    compose-ui-agent.md         ← Sub-agent: Compose UI rules
    security-config-agent.md    ← Sub-agent: Security rules
    test-qa-agent.md            ← Sub-agent: Testing rules
    check_coverage.py           ← Coverage table printer
```

### ❌ NOT USED IN PIPELINE (Safe to Ignore/Delete)

```
.github/scripts/
    test_report.py              ← Local tool only, not called in workflow

app/
    custom_agents/              ← Empty folder
    code-review-agent.md        ← Old draft, superseded
    Quiz_generation_Agent_Created_By_Meet.md  ← Documentation only
```

---

## 💡 PRESENTER TIPS

- **Open GitHub Actions** in browser before presenting — so you can show it live
- **Have the PR already queued** — start it right before Slide 8 so it finishes during your demo
- **Show the collapsed `::group::` logs** in GitHub Actions — audience loves seeing the full prompt
- **Non-tech audience:** Focus on Slides 2, 3, 8, 12 — skip code details
- **Tech audience:** Spend more time on Slides 4, 5, 6, 7 — they'll ask good questions

---

*Guide prepared: April 15, 2026*

