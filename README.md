# Dummy Quiz Using Agent

Jetpack Compose app with two AI-powered flows:

- Quiz Agent: generates technology-specific quizzes by experience level.
- Smart Shopping Decision Agent: summarizes product reviews, flags fake-review risk, suggests alternatives, and gives preference-aware recommendations.

## Setup

1. Add your Gemini key to `local.properties`:

```properties
GEMINI_API_KEY=your_api_key_here
```

2. Build the app:

```bash
./gradlew build
```

## App flow

- Home: pick Quiz Agent or Smart Shopping Agent.
- Quiz Setup: choose technology, experience level, and question count.
- Loading: Gemini quiz generation runs.
- In Progress: answer generated questions.
- Results: score and incorrect answer review with hints.
- Error: retry, back, or fallback sample quiz.
- Shopping Input: paste Amazon/Flipkart link and choose preference.
- Shopping Result: get review summary, fake-review risk, alternatives, and personalized suggestion.

## Local PR context artifacts for the review agent

Generate the PR diff file:

```bash
git fetch origin
git diff --binary origin/main...HEAD > pr_diff.patch
```

Generate last 90 days of human PR review comments (excluding `github-actions[bot]`):

```bash
REPO="meetjanani/AI_Quiz_Using_Agent"
SINCE="$(date -u -v-90d +"%Y-%m-%dT%H:%M:%SZ")"
gh api --paginate "repos/${REPO}/pulls/comments?per_page=100&sort=created&direction=desc&since=${SINCE}" \
  | jq -r '.[]
	  | select(.user.login != "github-actions[bot]")
	  | "[PR #\(.pull_request_url | split("/")[-1])] \(.user.login) @ \(.path // "unknown-file"):\(.line // .original_line // "?") - \(.body | gsub("\\s+"; " "))"' \
  > recent_pr_comments.txt
```

Then run the agent locally (it reads both files automatically when present):

```bash
python .github/scripts/run_agent.py
```

