#!/usr/bin/env python3
"""
Local coverage checker — reads the JaCoCo XML report and prints a per-file
table with a clear PASS / BELOW-75% flag.
Run from the repo root:
    python .github/scripts/check_coverage.py
"""
import sys
import glob
import os
import xml.etree.ElementTree as ET


THRESHOLD = 75.0

BUSINESS_LOGIC_MARKERS = ["/repository/", "/usecase/", "/domain/", "/viewmodel/", "/data/"]


def find_report():
    patterns = [
        "app/build/reports/jacoco/**/*.xml",
        "**/build/reports/jacoco/**/*.xml",
        "**/build/reports/kover/**/*.xml",
    ]
    for pattern in patterns:
        matches = glob.glob(pattern, recursive=True)
        for m in matches:
            if m.lower().endswith(".xml"):
                return m
    return None


def parse_coverage(xml_path):
    root = ET.parse(xml_path).getroot()
    rows = []
    for package_node in root.findall("package"):
        pkg = package_node.attrib.get("name", "")
        for sf in package_node.findall("sourcefile"):
            name = sf.attrib.get("name", "")
            covered = missed = 0
            for line in sf.findall("line"):
                ci = int(line.attrib.get("ci", "0"))
                mi = int(line.attrib.get("mi", "0"))
                if ci > 0:
                    covered += 1
                elif mi > 0:
                    missed += 1
            total = covered + missed
            if total > 0:
                pct = covered * 100.0 / total
                rows.append({
                    "path": f"{pkg}/{name}",
                    "covered": covered,
                    "total": total,
                    "pct": pct,
                })
    return rows


def is_business_file(path):
    lower = path.lower()
    return any(marker in lower for marker in BUSINESS_LOGIC_MARKERS)


def main():
    report = find_report()
    if not report:
        print("No JaCoCo/Kover XML report found.")
        print("Run:  ./gradlew :app:testDebugUnitTest :app:jacocoTestReport")
        sys.exit(1)

    print(f"Coverage report: {report}\n")
    rows = parse_coverage(report)
    rows.sort(key=lambda r: r["pct"])

    col_w = 65
    print(f"{'File':<{col_w}} {'Covered':>8} {'Total':>7} {'Coverage':>10}  Status")
    print("-" * (col_w + 40))

    below_threshold = []
    for row in rows:
        flag = "✅  OK" if row["pct"] >= THRESHOLD else "⚠️  BELOW 75%"
        short = row["path"]
        if len(short) > col_w:
            short = "..." + short[-(col_w - 3):]
        print(f"{short:<{col_w}} {row['covered']:>8} {row['total']:>7} {row['pct']:>9.1f}%  {flag}")
        if row["pct"] < THRESHOLD:
            below_threshold.append(row)

    total_covered = sum(r["covered"] for r in rows)
    total_lines = sum(r["total"] for r in rows)
    overall_pct = total_covered * 100.0 / total_lines if total_lines else 0
    print("-" * (col_w + 40))
    overall_flag = "✅  OK" if overall_pct >= THRESHOLD else "⚠️  BELOW 75%"
    print(f"{'OVERALL':<{col_w}} {total_covered:>8} {total_lines:>7} {overall_pct:>9.1f}%  {overall_flag}")

    if below_threshold:
        print(f"\n{'='*60}")
        print(f"FILES BELOW {THRESHOLD:.0f}% COVERAGE THRESHOLD")
        print(f"{'='*60}")
        for row in below_threshold:
            missing_lines = row["total"] - row["covered"]
            missing_pct = THRESHOLD - row["pct"]
            biz = "  [business-logic]" if is_business_file(row["path"]) else ""
            print(f"\n  File    : {row['path']}{biz}")
            print(f"  Current : {row['pct']:.1f}%  ({row['covered']}/{row['total']} lines covered)")
            print(f"  Missing : ~{missing_lines} more lines to reach {THRESHOLD:.0f}%  (+{missing_pct:.1f}%)")
        sys.exit(1)
    else:
        print(f"\nAll files meet the {THRESHOLD:.0f}% coverage threshold. ✅")
        sys.exit(0)


if __name__ == "__main__":
    main()

