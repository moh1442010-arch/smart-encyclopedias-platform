package com.mohammedmustafa.smartcompany;

import org.junit.Test;
import static org.junit.Assert.*;

public class BackupSchemaVersionTest {
    @Test
    public void acceptsCurrentDatabaseSchemaVersion() {
        assertTrue(MainActivity.isSupportedBackupSchemaVersion(11));
    }

    @Test
    public void acceptsOlderVersionsForValidatedUpgradePath() {
        assertTrue(MainActivity.isSupportedBackupSchemaVersion(0));
        assertTrue(MainActivity.isSupportedBackupSchemaVersion(10));
    }

    @Test
    public void rejectsFutureAndNegativeSchemaVersions() {
        assertFalse(MainActivity.isSupportedBackupSchemaVersion(12));
        assertFalse(MainActivity.isSupportedBackupSchemaVersion(-1));
    }
}
