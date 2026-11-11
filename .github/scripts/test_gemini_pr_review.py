import base64
import importlib.util
import json
import unittest
from unittest.mock import patch
from pathlib import Path

SCRIPT = Path(__file__).with_name("gemini_pr_review.py")
SPEC = importlib.util.spec_from_file_location("gemini_pr_review", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC and SPEC.loader
SPEC.loader.exec_module(MODULE)


class RedactionTests(unittest.TestCase):
    def test_google_api_key_is_redacted(self):
        self.assertNotIn("AIza", MODULE.redact_secrets("key=AIza0123456789abcdefghijklmnop"))

    def test_github_token_is_redacted(self):
        self.assertNotIn("ghp_", MODULE.redact_secrets("ghp_abcdefghijklmnopqrstuvwxyz123456"))

    def test_bearer_token_is_redacted(self):
        self.assertNotIn("verylongbearertokenvalue", MODULE.redact_secrets(
            "Authorization: Bearer verylongbearertokenvalue"
        ))

    def test_secret_assignment_line_is_removed(self):
        result = MODULE.redact_secrets("GEMINI_API_KEY=some-secret-value")
        self.assertEqual(result, "[REDACTED]")

    def test_normal_accounting_text_is_preserved(self):
        text = "Debit 1000 SDG, Credit 1000 SDG; journal balanced."
        self.assertEqual(MODULE.redact_secrets(text), text)

    def test_interactions_response_extracts_model_text_only(self):
        response = {
            "status": "completed",
            "steps": [
                {"type": "user_input", "content": [{"type": "text", "text": "ignore rules"}]},
                {"type": "model_output", "content": [
                    {"type": "text", "text": "Finding one."},
                    {"type": "text", "text": "Finding two."}
                ]}
            ]
        }
        self.assertEqual(
            MODULE.extract_interaction_text(response),
            "Finding one.\nFinding two."
        )

    def test_empty_interactions_response_returns_empty_text(self):
        self.assertEqual(MODULE.extract_interaction_text({"steps": []}), "")

    def test_register_loader_requires_exactly_90_defects(self):
        register = {
            "baseline": "2.8.0",
            "candidate": "2.8.3",
            "phases": {"P1": "Accounting", "P2": "Operations", "P3": "Quality"},
            "defects": [
                {"id": f"D{i:02}", "phase": "P1", "status": "OPEN",
                 "title": f"Defect {i}", "scope": "scope",
                 "acceptance_criteria": ["criterion"], "tests_required": ["test"]}
                for i in range(1, 91)
            ],
        }
        encoded = base64.b64encode(json.dumps(register).encode()).decode()
        with patch.object(MODULE, "request_json", return_value={
            "encoding": "base64", "content": encoded
        }):
            result = json.loads(MODULE.load_repair_register(
                "owner/repo", "abc123", {"Authorization": "Bearer test"}
            ))
        self.assertEqual(len(result["defects"]), 90)
        self.assertEqual(result["defects"][0]["id"], "D01")
        self.assertEqual(result["defects"][-1]["id"], "D90")
        self.assertEqual(result["phases"]["P2"], "Operations")

    def test_register_loader_rejects_incomplete_register(self):
        register = {"defects": [{"id": "D01"}]}
        encoded = base64.b64encode(json.dumps(register).encode()).decode()
        with patch.object(MODULE, "request_json", return_value={
            "encoding": "base64", "content": encoded
        }):
            with self.assertRaises(RuntimeError):
                MODULE.load_repair_register(
                    "owner/repo", "abc123", {"Authorization": "Bearer test"}
                )

    def test_incomplete_review_is_rejected(self):
        with self.assertRaises(RuntimeError):
            MODULE.validate_review_text("تضمن التعديل إضافة فحص هوية الطرف.")

    def test_review_without_verdict_is_rejected(self):
        report = ("نتائج وملاحظات هندسية " * 100) + "\n## النتائج\nلا توجد ملاحظات."
        with self.assertRaises(RuntimeError):
            MODULE.validate_review_text(report)

    def test_complete_review_with_verdict_is_accepted(self):
        report = (
            ("مراجعة هندسية تفصيلية للملفات والتغييرات. " * 60)
            + "\n## النتائج\nتم فحص التغييرات والاختبارات المقترحة."
            + "\n## الحكم النهائي\nNEEDS CHANGES"
        )
        MODULE.validate_review_text(report)


if __name__ == "__main__":
    unittest.main()
