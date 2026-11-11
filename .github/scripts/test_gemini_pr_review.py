import base64
import io
import urllib.error
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



class ReviewScopeTests(unittest.TestCase):
    def test_reviewer_prompt_covers_three_stages_and_five_axes(self):
        source = SCRIPT.read_text(encoding="utf-8")
        required_sections = [
            "PROGRAM FRAMEWORK — THREE PHASES",
            "FIVE REVIEW AXES",
            "Stage 1 closes the accounting foundation",
            "Stage 2 integrates accounts and operational data",
            "Stage 3 completes quality, security, usability",
            "Accounting integrity",
            "Bakery/factory operations",
            "Enterprise finance",
            "Security and data continuity",
            "Product quality and market readiness",
            "D01-D90",
        ]
        for marker in required_sections:
            with self.subTest(marker=marker):
                self.assertIn(marker, source)

    def test_register_context_limit_is_above_measured_full_register_size(self):
        self.assertEqual(MODULE.MAX_REGISTER_CONTEXT_CHARS, 55_000)

    def test_register_loader_preserves_all_90_records_including_d90(self):
        register = {
            "baseline": "2.8.0",
            "candidate": "2.8.3",
            "phases": {"P1": "Accounting", "P2": "Operations", "P3": "Quality"},
            "defects": [
                {
                    "id": f"D{i:02}", "phase": "P3", "status": "OPEN",
                    "title": f"Defect {i}", "scope": "Scope detail " * 45,
                    "acceptance_criteria": ["criterion " * 8],
                    "tests_required": ["test " * 8],
                }
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
        self.assertEqual(result["defects"][-1]["id"], "D90")

    def test_register_loader_fails_closed_when_full_context_exceeds_limit(self):
        register = {
            "baseline": "2.8.0",
            "candidate": "2.8.3",
            "phases": {},
            "defects": [
                {"id": f"D{i:02}", "phase": "P3", "status": "OPEN",
                 "title": "x" * 900, "scope": "y" * 900}
                for i in range(1, 91)
            ],
        }
        encoded = base64.b64encode(json.dumps(register).encode()).decode()
        with patch.object(MODULE, "request_json", return_value={
            "encoding": "base64", "content": encoded
        }):
            with self.assertRaisesRegex(RuntimeError, "silently truncated"):
                MODULE.load_repair_register(
                    "owner/repo", "abc123", {"Authorization": "Bearer test"}
                )


class RequestRetryTests(unittest.TestCase):
    def test_request_retries_transient_503_then_succeeds(self):
        temporary = urllib.error.HTTPError(
            "https://example.invalid", 503, "Unavailable",
            {"Retry-After": "0"}, io.BytesIO(b"")
        )
        with patch.object(MODULE.urllib.request, "urlopen",
                          side_effect=[temporary, io.BytesIO(b'{"ok": true}')]) as urlopen, \
             patch.object(MODULE.time, "sleep") as sleep:
            result = MODULE.request_json("https://example.invalid", {})
        self.assertEqual(result, {"ok": True})
        self.assertEqual(urlopen.call_count, 2)
        sleep.assert_called_once_with(0.0)


    def test_retry_after_header_allows_longer_bounded_wait(self):
        temporary = urllib.error.HTTPError(
            "https://example.invalid", 503, "Unavailable",
            {"Retry-After": "20"}, io.BytesIO(b"")
        )
        with patch.object(MODULE.urllib.request, "urlopen",
                          side_effect=[temporary, io.BytesIO(b'{"ok": true}')]) as urlopen:
            with patch.object(MODULE.time, "sleep") as sleep:
                result = MODULE.request_json("https://example.invalid", {})
        self.assertEqual(result, {"ok": True})
        self.assertEqual(urlopen.call_count, 2)
        sleep.assert_called_once_with(20.0)

    def test_retry_after_is_capped_at_thirty_seconds(self):
        temporary = urllib.error.HTTPError(
            "https://example.invalid", 503, "Unavailable",
            {"Retry-After": "120"}, io.BytesIO(b"")
        )
        with patch.object(MODULE.urllib.request, "urlopen",
                          side_effect=[temporary, io.BytesIO(b'{"ok": true}')]):
            with patch.object(MODULE.time, "sleep") as sleep:
                MODULE.request_json("https://example.invalid", {})
        sleep.assert_called_once_with(30.0)

    def test_timed_out_post_is_not_replayed(self):
        timeout = urllib.error.URLError("The read operation timed out")
        with patch.object(MODULE.urllib.request, "urlopen", side_effect=timeout) as urlopen, \
             patch.object(MODULE.time, "sleep") as sleep:
            with self.assertRaisesRegex(RuntimeError, "POSTs are not retried"):
                MODULE.request_json("https://example.invalid", {}, data=b'{"review": true}')
        self.assertEqual(urlopen.call_count, 1)
        sleep.assert_not_called()

    def test_socket_read_timeout_on_post_is_not_replayed(self):
        with patch.object(MODULE.urllib.request, "urlopen", side_effect=TimeoutError("The read operation timed out")) as urlopen, patch.object(MODULE.time, "sleep") as sleep:
            with self.assertRaisesRegex(RuntimeError, "POSTs are not retried"):
                MODULE.request_json("https://example.invalid", {}, data=b'{"review": true}')
        self.assertEqual(urlopen.call_count, 1)
        sleep.assert_not_called()

    def test_review_request_uses_bounded_output_budget(self):
        source = SCRIPT.read_text(encoding="utf-8")
        self.assertIn('"generation_config": {"max_output_tokens": 3072}', source)
        self.assertEqual(MODULE.MAX_DIFF_CHARS, 30_000)
        self.assertEqual(MODULE.MAX_REGISTER_CONTEXT_CHARS, 55_000)

    def test_request_does_not_retry_permanent_400(self):
        permanent = urllib.error.HTTPError(
            "https://example.invalid", 400, "Bad Request", {}, io.BytesIO(b"")
        )
        with patch.object(MODULE.urllib.request, "urlopen", side_effect=permanent) as urlopen, \
             patch.object(MODULE.time, "sleep") as sleep:
            with self.assertRaisesRegex(RuntimeError, "HTTP 400"):
                MODULE.request_json("https://example.invalid", {})
        self.assertEqual(urlopen.call_count, 1)
        sleep.assert_not_called()

    def test_request_stops_after_five_transient_failures(self):
        failures = [
            urllib.error.HTTPError(
                "https://example.invalid", 503, "Unavailable", {}, io.BytesIO(b"")
            )
            for _ in range(5)
        ]
        with patch.object(MODULE.urllib.request, "urlopen", side_effect=failures) as urlopen, \
             patch.object(MODULE.time, "sleep") as sleep:
            with self.assertRaisesRegex(RuntimeError, "HTTP 503"):
                MODULE.request_json("https://example.invalid", {})
        self.assertEqual(urlopen.call_count, 5)
        self.assertEqual(sleep.call_count, 4)


if __name__ == "__main__":
    unittest.main()
