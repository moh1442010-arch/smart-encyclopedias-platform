package com.mohammedmustafa.almizan.bakery;

/**
 * Validates completeness of the current portable-backup envelope.
 * Legacy backups without a manifest remain importable for backward compatibility.
 */
final class BackupManifestValidator {
    private static final String APP_ID = "com.mohammedmustafa.almizan.bakery";
    private static final int CURRENT_SCHEMA = 1;

    private BackupManifestValidator() {}

    static void validate(boolean hasManifest,String appId,int schemaVersion,boolean hasParty,boolean hasFactory) {
        if (!hasManifest) return;
        if (!APP_ID.equals(appId) || schemaVersion != CURRENT_SCHEMA)
            throw new IllegalArgumentException("Unsupported backup app identity or schema version");
        if (!hasParty)
            throw new IllegalArgumentException("Incomplete backup: customer and supplier directory section is missing");
        if (!hasFactory)
            throw new IllegalArgumentException("Incomplete backup: factory data section is missing");
    }
}
