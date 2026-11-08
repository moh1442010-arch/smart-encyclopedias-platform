package com.mohammedmustafa.almizan.bakery;

import org.json.JSONObject;

/**
 * Validates completeness of the current portable-backup envelope.
 * Legacy backups without a manifest remain importable for backward compatibility.
 */
final class BackupManifestValidator {
    private static final String APP_ID = "com.mohammedmustafa.almizan.bakery";
    private static final int CURRENT_SCHEMA = 1;

    private BackupManifestValidator() {}

    static void validate(JSONObject manifest, JSONObject partySnapshot, JSONObject factorySnapshot) {
        if (manifest == null) return; // Legacy backup format; retain the existing compatibility path.

        if (!APP_ID.equals(manifest.optString("app_id"))
                || manifest.optInt("schema_version", -1) != CURRENT_SCHEMA) {
            throw new IllegalArgumentException("Unsupported backup app identity or schema version");
        }
        if (partySnapshot == null) {
            throw new IllegalArgumentException("Incomplete backup: customer and supplier directory section is missing");
        }
        if (factorySnapshot == null) {
            throw new IllegalArgumentException("Incomplete backup: factory data section is missing");
        }
    }
}
