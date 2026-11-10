#!/usr/bin/env python3
"""Al-Mizan 90-defect release gate. Safe by design: it never edits app source or closes defects."""
import argparse, json, re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MANIFEST = ROOT / "bakery-app/docs/ALMIZAN_90_REPAIR_REGISTER.json"
SHA = re.compile(r"^[0-9a-f]{7,40}$", re.I)

def load():
    data = json.loads(MANIFEST.read_text(encoding="utf-8"))
    defects = data.get("defects", [])
    ids = [x.get("id") for x in defects]
    expected = [f"D{i:02d}" for i in range(1, 91)]
    if ids != expected:
        raise ValueError("Register must contain D01-D90 exactly once and in order.")
    for item in defects:
        if item.get("phase") not in {"P1", "P2", "P3"}:
            raise ValueError(f"{item['id']}: invalid phase")
        if item.get("status") not in {"OPEN", "IN_PROGRESS", "CLOSED"}:
            raise ValueError(f"{item['id']}: invalid status")
        if not str(item.get("title", "")).strip():
            raise ValueError(f"{item['id']}: missing defect title")
        if not str(item.get("scope", "")).strip():
            raise ValueError(f"{item['id']}: missing implementation scope")
        criteria = item.get("acceptance_criteria")
        tests = item.get("tests_required")
        if not isinstance(criteria, list) or len(criteria) < 2 or not all(str(x).strip() for x in criteria):
            raise ValueError(f"{item['id']}: at least two explicit acceptance criteria are required")
        if not isinstance(tests, list) or not tests or not all(str(x).strip() for x in tests):
            raise ValueError(f"{item['id']}: reproducible test requirements are missing")
    return data

def valid_evidence(item):
    ev = item.get("evidence") or {}
    return (bool(SHA.fullmatch(str(ev.get("commit", ""))) and
                ev.get("test_command") and ev.get("result") == "PASS" and
                ev.get("evidence_ref")))

def report(data):
    print("AL-MIZAN 90-DEFECT REPAIR STATUS")
    print("REGISTER QUALITY: all 90 items have a title, implementation scope, acceptance criteria and test requirements")
    for phase, description in data["phases"].items():
        rows = [x for x in data["defects"] if x["phase"] == phase]
        closed = sum(x["status"] == "CLOSED" and valid_evidence(x) for x in rows)
        active = sum(x["status"] == "IN_PROGRESS" for x in rows)
        opened = len(rows) - closed - active
        print(f"{phase}: closed-with-evidence={closed}/{len(rows)}, in-progress={active}, open={opened}")
        print(f"  {description}")
    invalid = [x["id"] for x in data["defects"] if x["status"] == "CLOSED" and not valid_evidence(x)]
    if invalid:
        print("INVALID CLOSED RECORDS (missing commit/test/PASS/evidence): " + ", ".join(invalid))
    total = sum(x["status"] == "CLOSED" and valid_evidence(x) for x in data["defects"])
    print(f"TOTAL: {total}/90 defects closed with evidence")

def gate(data):
    report(data)
    blockers = [x["id"] for x in data["defects"] if x["status"] != "CLOSED" or not valid_evidence(x)]
    if blockers:
        print("FINAL GATE: BLOCKED")
        print("Unclosed or unsupported defects: " + ", ".join(blockers))
        return 1
    print("FINAL GATE: DEFECT REGISTER PASSED")
    print("Note: physical-device, upgrade, signing, backup-restore and owner acceptance gates must also be recorded separately.")
    return 0

def main():
    parser = argparse.ArgumentParser(description="Al-Mizan 90-defect evidence gate")
    parser.add_argument("command", choices=["report", "gate"])
    args = parser.parse_args()
    try:
        data = load()
        return gate(data) if args.command == "gate" else (report(data) or 0)
    except Exception as exc:
        print(f"REGISTER ERROR: {exc}", file=sys.stderr)
        return 2

if __name__ == "__main__":
    raise SystemExit(main())
