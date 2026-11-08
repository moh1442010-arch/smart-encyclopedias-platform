package com.mohammedmustafa.almizan.bakery;

import org.json.JSONObject;
import org.junit.Test;

public class BackupManifestValidatorTest {
    private JSONObject manifest() throws Exception {
        return new JSONObject("{\"_type\":\"almizan_backup_manifest\",\"schema_version\":1,\"app_id\":\"com.mohammedmustafa.almizan.bakery\"}");
    }

    @Test public void acceptsCompleteCurrentBackup() throws Exception {
        BackupManifestValidator.validate(manifest(),
                new JSONObject("{\"_type\":\"almizan_party_directory_backup\"}"),
                new JSONObject("{\"_type\":\"almizan_factory_backup\"}"));
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsCurrentBackupMissingPartyDirectory() throws Exception {
        BackupManifestValidator.validate(manifest(), null,
                new JSONObject("{\"_type\":\"almizan_factory_backup\"}"));
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsCurrentBackupMissingFactory() throws Exception {
        BackupManifestValidator.validate(manifest(),
                new JSONObject("{\"_type\":\"almizan_party_directory_backup\"}"), null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsWrongApplicationIdentity() throws Exception {
        BackupManifestValidator.validate(new JSONObject("{\"schema_version\":1,\"app_id\":\"other.app\"}"),
                new JSONObject(), new JSONObject());
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsUnsupportedManifestSchema() throws Exception {
        BackupManifestValidator.validate(new JSONObject("{\"schema_version\":2,\"app_id\":\"com.mohammedmustafa.almizan.bakery\"}"),
                new JSONObject(), new JSONObject());
    }

    @Test public void preservesLegacyBackupCompatibility() {
        BackupManifestValidator.validate(null, null, null);
    }
}
