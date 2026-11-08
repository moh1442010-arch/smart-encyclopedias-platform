package com.mohammedmustafa.almizan.bakery;

/**
 * Validates a portable backup envelope. New backups identify exactly one
 * accounting system. Older combined archives require an explicit import choice.
 */
final class BackupManifestValidator {
    private static final String APP_ID = "com.mohammedmustafa.almizan.bakery";
    private static final int CURRENT_SCHEMA = 1;

    private BackupManifestValidator() {}

    static void validate(boolean hasManifest,String appId,int schemaVersion,boolean hasParty,boolean hasFactory) {
        validate(hasManifest,appId,schemaVersion,"",hasParty,hasFactory);
    }

    static void validate(boolean hasManifest,String appId,int schemaVersion,String system,boolean hasParty,boolean hasFactory) {
        if (!hasManifest) return;
        if (!APP_ID.equals(appId) || schemaVersion != CURRENT_SCHEMA)
            throw new IllegalArgumentException("Unsupported backup app identity or schema version");
        String selected=system==null?"":system.trim();
        if ("bakery".equals(selected)) {
            if (!hasParty || hasFactory)
                throw new IllegalArgumentException("Bakery backup must contain only bakery records and its own party directory");
        } else if ("factory".equals(selected)) {
            if (!hasFactory || hasParty)
                throw new IllegalArgumentException("Factory backup must contain only factory records and its own party directory");
        } else if (selected.isEmpty()) {
            if (!hasParty || !hasFactory)
                throw new IllegalArgumentException("Legacy combined backup is incomplete");
        } else {
            throw new IllegalArgumentException("Unknown backup accounting system");
        }
    }
}
