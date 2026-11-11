import importlib.util
import unittest
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
            "Finding one.\\nFinding two."
        )

    def test_empty_interactions_response_returns_empty_text(self):
        self.assertEqual(MODULE.extract_interaction_text({"steps": []}), "")


if __name__ == "__main__":
    unittest.main()
