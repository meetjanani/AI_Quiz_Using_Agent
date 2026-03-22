#!/usr/bin/env python3
"""Generate a comprehensive test execution and coverage report."""
import xml.etree.ElementTree as ET
import re

test_results_file = "app/build/reports/tests/testDebugUnitTest/index.html"
coverage_path = "app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml"

# Read test report summary
try:
    with open(test_results_file, 'r') as f:
        content = f.read()
        tests = re.search(r'<div class="counter">(\d+)</div>\s*<p>tests</p>', content)
        failures = re.search(r'<div class="counter">(\d+)</div>\s*<p>failures</p>', content)
        ignored = re.search(r'<div class="counter">(\d+)</div>\s*<p>ignored</p>', content)
        duration = re.search(r'<div class="counter">([\d.]+s)</div>\s*<p>duration</p>', content)

        if tests:
            print("\n" + "="*70)
            print("        UNIT TEST EXECUTION REPORT - PROJECT SUMMARY")
            print("="*70)
            print(f"\n📊 Test Results:")
            total_tests = int(tests.group(1))
            failed_tests = int(failures.group(1))
            passed_tests = total_tests - failed_tests
            print(f"   ✅ Total Tests:      {total_tests}")
            print(f"   ✅ Passed:           {passed_tests}")
            print(f"   ❌ Failed:           {failed_tests}")
            print(f"   ⏭️  Ignored:          {ignored.group(1)}")
            print(f"   ⏱️  Duration:         {duration.group(1)}")
            success_rate = (passed_tests / total_tests * 100) if total_tests > 0 else 0
            print(f"   🎯 Success Rate:     {success_rate:.1f}%")
except Exception as e:
    print(f"Error reading test results: {e}")

# Parse coverage report
try:
    root = ET.parse(coverage_path).getroot()
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
                rows.append((f"{pkg}/{name}", covered, total, pct))

    total_covered = sum(r[1] for r in rows)
    total_lines = sum(r[2] for r in rows)
    overall_pct = total_covered * 100.0 / total_lines if total_lines else 0

    print(f"\n📈 Code Coverage Analysis:")
    print(f"   📊 Overall Coverage: {overall_pct:.1f}%")
    print(f"   📍 Lines Covered:    {total_covered}/{total_lines}")
    print(f"   📁 Files Analyzed:   {len(rows)}")

    print(f"\n📋 Per-File Coverage Breakdown:")
    print(f"   {'File':<55} {'Coverage':>10} {'Status':>10}")
    print(f"   {'-'*55} {'-'*10} {'-'*10}")
    for (path, covered, total, pct) in sorted(rows, key=lambda x: -x[3]):
        status = "✅ PASS" if pct >= 75 else "⚠️  LOW"
        short_path = path if len(path) <= 55 else "..." + path[-52:]
        print(f"   {short_path:<55} {pct:>9.1f}% {status:>10}")

    print(f"\n" + "="*70)
    above_75 = len([r for r in rows if r[3] >= 75])
    below_75 = len([r for r in rows if r[3] < 75])
    print(f"Summary: {above_75} files ✅ PASS | {below_75} files ⚠️ BELOW 75%")
    print("="*70 + "\n")

except Exception as e:
    print(f"\nℹ️ Coverage report not yet generated: {e}")

