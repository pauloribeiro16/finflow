import sys
import xml.etree.ElementTree as ET

REPORT = sys.argv[1] if len(sys.argv) > 1 else "app/build/reports/coverage/test/prod/debug/report.xml"
MIN_LINE_COVERAGE = 95.0
PACKAGES = ("com/financas/app/domain/model", "com/financas/app/domain/usecase")

root = ET.parse(REPORT).getroot()
failed = False
for package in root.findall("package"):
    name = package.get("name")
    if name not in PACKAGES:
        continue
    line = next(c for c in package.findall("counter") if c.get("type") == "LINE")
    covered, missed = int(line.get("covered")), int(line.get("missed"))
    pct = 100.0 * covered / max(1, covered + missed)
    status = "OK" if pct >= MIN_LINE_COVERAGE else "FALHA"
    print(f"{status} {name}: {pct:.1f}% linhas (mínimo {MIN_LINE_COVERAGE}%)")
    failed |= pct < MIN_LINE_COVERAGE
sys.exit(1 if failed else 0)
