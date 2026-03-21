import os
import requests
import re
import subprocess

API_KEY = os.environ.get("LLM_API_KEY")
GH_TOKEN = os.environ.get("GH_TOKEN")
PR_NUMBER = os.environ.get("PR_NUMBER")
REPO_NAME = os.environ.get("REPO_NAME")

LLM_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

def read_file(filepath):
    with open(filepath, 'r') as file:
        return file.read()

def read_optional_file(filepath):
    try:
        return read_file(filepath)
    except FileNotFoundError:
        return ""

def extract_tag_value(block, tag_name):
    tag_match = re.search(rf'<{tag_name}>(.*?)</{tag_name}>', block, re.DOTALL | re.IGNORECASE)
    return tag_match.group(1).strip() if tag_match else ""

def extract_findings(review_text):
    finding_blocks = re.findall(r'<finding>(.*?)</finding>', review_text, re.DOTALL | re.IGNORECASE)
    findings = []
    for block in finding_blocks:
        findings.append({
            "severity": extract_tag_value(block, "severity") or "Unspecified",
            "location": extract_tag_value(block, "location") or "Unknown location",
            "problem": extract_tag_value(block, "problem") or "No problem text provided",
            "impact": extract_tag_value(block, "impact") or "No impact provided",
            "probable_fix": extract_tag_value(block, "probable_fix") or "No fix suggestion provided",
        })
    return findings

def maybe_collect_lint_context():
    lint_text = read_optional_file("lint_output.txt")
    if lint_text.strip():
        return lint_text[-12000:]

    should_run_local_lint = os.environ.get("RUN_ANDROID_LINT", "false").lower() == "true"
    if not should_run_local_lint:
        return ""

    print("RUN_ANDROID_LINT=true detected. Running :app:lintDebug for extra context...")
    try:
        lint_result = subprocess.run(
            ["./gradlew", ":app:lintDebug", "--console=plain"],
            capture_output=True,
            text=True,
            timeout=1200,
            check=False,
        )
        lint_output = f"{lint_result.stdout}\n{lint_result.stderr}".strip()
        return lint_output[-12000:]
    except Exception as exc:
        print(f"Unable to collect local lint context: {exc}")
        return ""

def post_findings_to_github(findings, fallback_comment):
    if not (GH_TOKEN and PR_NUMBER and REPO_NAME):
        print("GitHub env vars not fully set. Skipping PR comment posting.")
        return

    gh_headers = {
        "Authorization": f"token {GH_TOKEN}",
        "Accept": "application/vnd.github.v3+json"
    }
    gh_url = f"https://api.github.com/repos/{REPO_NAME}/issues/{PR_NUMBER}/comments"

    if not findings:
        body = "No findings. Reviewed for Android/Kotlin lint, static literals, magic numbers, formatting, and general code quality."
        if fallback_comment.strip():
            body = f"{body}\n\nRaw reviewer output:\n{fallback_comment[:5000]}"
        response = requests.post(gh_url, headers=gh_headers, json={"body": body}, timeout=30)
        if response.status_code >= 300:
            print(f"Failed posting no-findings comment: {response.status_code} {response.text}")
        return

    for index, finding in enumerate(findings, start=1):
        body = (
            f"### Finding {index}\n"
            f"- Severity: **{finding['severity']}**\n"
            f"- Location: `{finding['location']}`\n"
            f"- Problem: {finding['problem']}\n"
            f"- Impact: {finding['impact']}\n"
            f"- Probable Fix: {finding['probable_fix']}"
        )
        response = requests.post(gh_url, headers=gh_headers, json={"body": body}, timeout=30)
        if response.status_code >= 300:
            print(f"Failed posting finding {index}: {response.status_code} {response.text}")

def main():
    if not API_KEY:
        print("Error: LLM_API_KEY is missing.")
        return

    master_agent_prompt = read_file(".github/scripts/master-reviewer-agent.md")

    try:
        pr_diff = read_file("pr_diff.patch")
    except FileNotFoundError:
        print("No diff found. Skipping review.")
        return

    if not pr_diff.strip():
        print("Diff is empty. Skipping review.")
        return

    lint_context = maybe_collect_lint_context()

    print("Sending code to Gemini Agent for review-only findings...")

    system_instruction = (
        f"{master_agent_prompt}\n\n"
        "You are locked in Review mode. Do NOT generate <file_update> blocks and do NOT propose auto-edits in raw file form.\n"
        "Check specifically for Android lint rules, Kotlin lint/style, magic numbers, hardcoded static values (String/Int/Float/Double), formatting, maintainability, and correctness regressions.\n"
        "If hardcoded values are found, recommend BuildConfig first when semantically appropriate; otherwise recommend the correct resource XML file.\n"
        "Return findings using ONLY this XML schema:\n"
        "<review_result>\n"
        "  <finding>\n"
        "    <severity>Critical|High|Medium|Low</severity>\n"
        "    <location>path + symbol/line</location>\n"
        "    <problem>...</problem>\n"
        "    <impact>...</impact>\n"
        "    <probable_fix>...</probable_fix>\n"
        "  </finding>\n"
        "  ...repeat one finding per <finding>...\n"
        "</review_result>\n"
        "If no issues exist, return exactly: <review_result><no_findings>true</no_findings></review_result>."
    )

    user_prompt = f"Here is the git diff:\n\n```diff\n{pr_diff}\n```"
    if lint_context.strip():
        user_prompt += f"\n\nOptional lint context (may include warnings/errors):\n\n```text\n{lint_context}\n```"

    headers = {
        "Content-Type": "application/json",
        "x-goog-api-key": API_KEY
    }

    payload = {
        "systemInstruction": {"parts": [{"text": system_instruction}]},
        "contents": [{"role": "user", "parts": [{"text": user_prompt}]}],
        "generationConfig": {"temperature": 0.1}
    }

    response = requests.post(LLM_API_URL, headers=headers, json=payload, timeout=90)
    response_data = response.json()

    if "error" in response_data:
        print(f"API Error: {response_data['error']['message']}")
        return

    try:
        review_comment = response_data["candidates"][0]["content"]["parts"][0]["text"]
    except KeyError:
        print("Error parsing Gemini response.")
        return

    findings = extract_findings(review_comment)
    no_findings = bool(re.search(r"<no_findings>\s*true\s*</no_findings>", review_comment, re.IGNORECASE))

    if findings:
        print(f"Found {len(findings)} issue(s):")
        for index, finding in enumerate(findings, start=1):
            print(f"{index}. [{finding['severity']}] {finding['location']} -> {finding['problem']}")
            print(f"   Probable fix: {finding['probable_fix']}")
    elif no_findings:
        print("No findings returned by reviewer.")
    else:
        print("Reviewer response was not in the expected schema. Raw output follows:")
        print(review_comment)

    post_findings_to_github(findings, "" if no_findings else review_comment)

if __name__ == "__main__":
    main()