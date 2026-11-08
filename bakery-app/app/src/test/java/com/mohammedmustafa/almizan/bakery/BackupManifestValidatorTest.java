package com.mohammedmustafa.almizan.bakery;

import org.junit.Test;

public class BackupManifestValidatorTest {
    private static final String APP = "com.mohammedmustafa.almizan.bakery";

    @Test public void acceptsCompleteCurrentBackup() {
        BackupManifestValidator.validate(true,APP,1,true,true);
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsCurrentBackupMissingPartyDirectory() {
        BackupManifestValidator.validate(true,APP,1,false,true);
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsCurrentBackupMissingFactory() {
        BackupManifestValidator.validate(true,APP,1,true,false);
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsWrongApplicationIdentity() {
        BackupManifestValidator.validate(true,"other.app",1,true,true);
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsUnsupportedManifestSchema() {
        BackupManifestValidator.validate(true,APP,2,true,true);
    }

    @Test public void preservesLegacyBackupCompatibility() {
        BackupManifestValidator.validate(false,null,-1,false,false);
    }

    @Test public void acceptsBakeryOnlyBackup() {
        BackupManifestValidator.validate(true,APP,1,"bakery",true,false);
    }

    @Test public void acceptsFactoryOnlyBackup() {
        BackupManifestValidator.validate(true,APP,1,"factory",false,true);
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsBakeryBackupContainingFactoryData() {
        BackupManifestValidator.validate(true,APP,1,"bakery",true,true);
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsFactoryBackupContainingBakeryPartyData() {
        BackupManifestValidator.validate(true,APP,1,"factory",true,true);
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsUnknownBackupSystem() {
        BackupManifestValidator.validate(true,APP,1,"both",true,true);
    }
}
