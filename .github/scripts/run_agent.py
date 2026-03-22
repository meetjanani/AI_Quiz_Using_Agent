import os
import requests
import re
import subprocess
import xml.etree.ElementTree as ET
import glob

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

def _strip_code_fences(text):
    trimmed = text.strip()
    fenced_match = re.match(r"^```(?:xml)?\s*(.*?)\s*```$", trimmed, re.DOTALL | re.IGNORECASE)
    return fenced_match.group(1).strip() if fenced_match else trimmed

def _normalized_severity(value):
    normalized = (value or "").strip().lower()
    allowed = {"critical": "Critical", "high": "High", "medium": "Medium", "low": "Low"}
    return allowed.get(normalized, "Unspecified")

def parse_review_xml(review_text):
    """
    Returns:
      {
        "ok": bool,
        "findings": list[dict],
        "no_findings": bool,
        "error": str
      }
    """
    xml_text = _strip_code_fences(review_text)
    if not xml_text:
        return {"ok": False, "findings": [], "no_findings": False, "error": "Empty reviewer response"}

    try:
        root = ET.fromstring(xml_text)
    except ET.ParseError as exc:
        return {"ok": False, "findings": [], "no_findings": False, "error": f"Invalid XML: {exc}"}

    if root.tag != "review_result":
        return {
            "ok": False,
            "findings": [],
            "no_findings": False,
            "error": f"Unexpected root tag '{root.tag}', expected 'review_result'",
        }

    no_findings_node = root.find("no_findings")
    no_findings = bool(no_findings_node is not None and (no_findings_node.text or "").strip().lower() == "true")

    finding_nodes = root.findall("finding")
    findings = []
    for node in finding_nodes:
        findings.append({
            "severity": _normalized_severity(node.findtext("severity", default="")),
            "location": (node.findtext("location", default="") or "").strip() or "Unknown location",
            "problem": (node.findtext("problem", default="") or "").strip() or "No problem text provided",
            "impact": (node.findtext("impact", default="") or "").strip() or "No impact provided",
            "probable_fix": (node.findtext("probable_fix", default="") or "").strip() or "No fix suggestion provided",
        })

    if no_findings and findings:
        return {
            "ok": False,
            "findings": [],
            "no_findings": False,
            "error": "Ambiguous XML: contains both <no_findings>true</no_findings> and <finding> blocks",
        }

    # Accept either exact no-findings sentinel or findings list.
    if not no_findings and not findings:
        return {"ok": False, "findings": [], "no_findings": False, "error": "No <finding> blocks and no no_findings sentinel"}

    return {"ok": True, "findings": findings, "no_findings": no_findings, "error": ""}

def _severity_rank(severity):
    order = {"Critical": 0, "High": 1, "Medium": 2, "Low": 3, "Unspecified": 4}
    return order.get(severity, 4)

def extract_changed_files_from_diff(diff_text):
    files = []
    for line in diff_text.splitlines():
        if line.startswith("+++ b/"):
            path = line[len("+++ b/"):].strip()
            if path and path != "/dev/null":
                files.append(path)
    return sorted(set(files))

def is_business_logic_file(path):
    lower = path.lower()
    if not (lower.endswith(".kt") or lower.endswith(".java")):
        return False
    return "/repository/" in lower or "/usecase/" in lower or "/domain/" in lower

def _collect_line_stats_from_sourcefile(sourcefile_node):
    covered = 0
    missed = 0
    for line in sourcefile_node.findall("line"):
        try:
            ci = int(line.attrib.get("ci", "0"))
            mi = int(line.attrib.get("mi", "0"))
        except ValueError:
            continue
        # Count a line as covered if any instruction on the line was covered.
        if ci > 0:
            covered += 1
        elif mi > 0:
            missed += 1
    total = covered + missed
    return covered, missed, total

def parse_jacoco_coverage_report(xml_path):
    try:
        root = ET.parse(xml_path).getroot()
    except (FileNotFoundError, ET.ParseError):
        return {}

    coverage_by_suffix = {}
    for package_node in root.findall("package"):
        package_name = package_node.attrib.get("name", "").strip()
        for sourcefile in package_node.findall("sourcefile"):
            source_name = sourcefile.attrib.get("name", "").strip()
            if not source_name:
                continue
            covered, missed, total = _collect_line_stats_from_sourcefile(sourcefile)
            if total <= 0:
                continue
            rel_path = f"{package_name}/{source_name}" if package_name else source_name
            rel_path = rel_path.replace("//", "/")
            coverage_by_suffix[rel_path] = {
                "covered": covered,
                "missed": missed,
                "total": total,
                "percentage": (covered * 100.0) / total,
            }
    return coverage_by_suffix

def _find_related_test_files(source_file):
    file_name = os.path.basename(source_file)
    base_name = os.path.splitext(file_name)[0]
    test_roots = [
        "app/src/test",
        "app/src/androidTest",
    ]
    matches = []
    for root in test_roots:
        if not os.path.isdir(root):
            continue
        for candidate in glob.glob(f"{root}/**/*{base_name}*Test.kt", recursive=True):
            matches.append(candidate)
        for candidate in glob.glob(f"{root}/**/{base_name}Test.java", recursive=True):
            matches.append(candidate)
    # Deduplicate while keeping stable order.
    seen = set()
    deduped = []
    for path in matches:
        if path not in seen:
            seen.add(path)
            deduped.append(path)
    return deduped

def _find_coverage_xml_files():
    patterns = [
        "app/build/reports/jacoco/**/*.xml",
        "**/build/reports/jacoco/**/*.xml",
        "**/build/reports/kover/**/*.xml",
    ]
    files = []
    for pattern in patterns:
        files.extend(glob.glob(pattern, recursive=True))
    # Keep likely report files only.
    return [f for f in sorted(set(files)) if f.lower().endswith(".xml")]

def maybe_collect_coverage_report():
    existing = _find_coverage_xml_files()
    if existing:
        return existing

    should_run = os.environ.get("RUN_TEST_COVERAGE", "false").lower() == "true"
    if not should_run:
        return []

    print("RUN_TEST_COVERAGE=true detected. Attempting to collect unit test coverage context...")
    commands = [
        ["./gradlew", ":app:testDebugUnitTest", "--console=plain"],
        ["./gradlew", ":app:jacocoTestReport", "--console=plain"],
        ["./gradlew", ":app:koverXmlReport", "--console=plain"],
    ]
    for cmd in commands:
        try:
            subprocess.run(cmd, capture_output=True, text=True, timeout=1800, check=False)
        except Exception as exc:
            print(f"Coverage command failed ({' '.join(cmd)}): {exc}")

    return _find_coverage_xml_files()

def build_coverage_alerts(pr_diff):
    changed_files = extract_changed_files_from_diff(pr_diff)
    business_files = [path for path in changed_files if is_business_logic_file(path)]
    if not business_files:
        return []

    coverage_reports = maybe_collect_coverage_report()
    if not coverage_reports:
        return []

    coverage_map = {}
    for report in coverage_reports:
        parsed = parse_jacoco_coverage_report(report)
        # Merge reports; keep the highest coverage view when the same file appears multiple times.
        for key, value in parsed.items():
            existing = coverage_map.get(key)
            if existing is None or value["percentage"] > existing["percentage"]:
                coverage_map[key] = value

    alerts = []
    for source_file in business_files:
        normalized = source_file.replace("\\", "/")
        best = None
        best_key = None
        for suffix, coverage in coverage_map.items():
            normalized_suffix = suffix.replace("\\", "/")
            if normalized.endswith(normalized_suffix):
                best = coverage
                best_key = normalized_suffix
                break
            # Fallback by filename only when package matching fails.
            if os.path.basename(normalized) == os.path.basename(normalized_suffix):
                if best is None or coverage["percentage"] > best["percentage"]:
                    best = coverage
                    best_key = normalized_suffix

        if best is None:
            continue

        if best["percentage"] < 75.0:
            missing_pct = max(0.0, 75.0 - best["percentage"])
            related_tests = _find_related_test_files(source_file)
            alerts.append({
                "source_file": source_file,
                "coverage_pct": best["percentage"],
                "missing_pct": missing_pct,
                "covered_lines": best["covered"],
                "total_lines": best["total"],
                "matched_report_path": best_key or "unknown",
                "related_tests": related_tests,
            })
    return alerts

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

def post_findings_to_github(findings, fallback_comment, coverage_alerts=None):
    coverage_alerts = coverage_alerts or []
    if not (GH_TOKEN and PR_NUMBER and REPO_NAME):
        print("GitHub env vars not fully set. Skipping PR comment posting.")
        return

    gh_headers = {
        "Authorization": f"token {GH_TOKEN}",
        "Accept": "application/vnd.github.v3+json"
    }
    gh_url = f"https://api.github.com/repos/{REPO_NAME}/issues/{PR_NUMBER}/comments"

    if not findings and not coverage_alerts:
        body = "No findings. Reviewed for Android/Kotlin lint, static literals, magic numbers, formatting, and general code quality."
        if fallback_comment.strip():
            body = f"{body}\n\nRaw reviewer output:\n{fallback_comment[:5000]}"
        response = requests.post(gh_url, headers=gh_headers, json={"body": body}, timeout=30)
        if response.status_code >= 300:
            print(f"Failed posting no-findings comment: {response.status_code} {response.text}")

    for index, alert in enumerate(coverage_alerts, start=1):
        related_tests = alert["related_tests"]
        tests_text = "\n".join([f"  - `{path}`" for path in related_tests]) if related_tests else "  - No matching test file found by naming convention."
        body = (
            f"### Special Coverage Alert {index}\n"
            f"- Scope: `{alert['source_file']}`\n"
            f"- Current Coverage: **{alert['coverage_pct']:.2f}%** ({alert['covered_lines']}/{alert['total_lines']} lines)\n"
            f"- Missing to reach 75%: **{alert['missing_pct']:.2f}%**\n"
            f"- Coverage Source: `{alert['matched_report_path']}`\n"
            f"- Related test files:\n{tests_text}\n"
            f"- Suggested action: Add/extend tests for uncovered branches and failure paths until this file reaches at least 75% coverage."
        )
        response = requests.post(gh_url, headers=gh_headers, json={"body": body}, timeout=30)
        if response.status_code >= 300:
            print(f"Failed posting coverage alert {index}: {response.status_code} {response.text}")

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

    try:
        response = requests.post(LLM_API_URL, headers=headers, json=payload, timeout=90)
    except requests.RequestException as exc:
        print(f"Failed to call LLM API: {exc}")
        return

    if response.status_code >= 300:
        print(f"LLM API HTTP error: {response.status_code} {response.text[:1000]}")
        return

    try:
        response_data = response.json()
    except ValueError:
        print("Failed to parse LLM API JSON response.")
        print(response.text[:1000])
        return

    if "error" in response_data:
        print(f"API Error: {response_data['error']['message']}")
        return

    try:
        review_comment = response_data["candidates"][0]["content"]["parts"][0]["text"]
    except KeyError:
        print("Error parsing Gemini response.")
        return

    parsed = parse_review_xml(review_comment)
    findings = parsed["findings"]
    no_findings = parsed["no_findings"]
    coverage_alerts = build_coverage_alerts(pr_diff)

    # Keep comment order stable and severity-first even if the model returns mixed ordering.
    findings.sort(key=lambda finding: _severity_rank(finding.get("severity", "Unspecified")))

    if not parsed["ok"]:
        print("Reviewer response was not in the expected XML schema.")
        print(f"Schema error: {parsed['error']}")
        print("Raw output follows:")
        print(review_comment)
        fallback = f"Schema error: {parsed['error']}\n\nRaw reviewer output:\n{review_comment[:5000]}"
        post_findings_to_github([], fallback, coverage_alerts)
    elif findings:
        print(f"Found {len(findings)} issue(s):")
        for index, finding in enumerate(findings, start=1):
            print(f"{index}. [{finding['severity']}] {finding['location']} -> {finding['problem']}")
            print(f"   Probable fix: {finding['probable_fix']}")
        if coverage_alerts:
            print(f"Detected {len(coverage_alerts)} coverage alert(s) below 75%.")
    elif no_findings:
        print("No findings returned by reviewer.")
        if coverage_alerts:
            print(f"Detected {len(coverage_alerts)} coverage alert(s) below 75%.")
    post_findings_to_github(findings, "" if parsed["ok"] and no_findings else review_comment, coverage_alerts)

if __name__ == "__main__":
    main()