package com.mohammedmustafa.almizan.bakery;

import org.junit.Test;
import static org.junit.Assert.*;

public class PortableBackupCryptoTest {
    @Test public void encryptsAndDecryptsPortableBackup() throws Exception {
        byte[] plain="{\"bakery\":[],\"factory\":[]}".getBytes("UTF-8");
        byte[] encrypted=PortableBackupCrypto.encrypt(plain,"StrongPass2026".toCharArray());
        assertTrue(PortableBackupCrypto.isEncrypted(encrypted));
        assertArrayEquals(plain,PortableBackupCrypto.decrypt(encrypted,"StrongPass2026".toCharArray()));
        assertFalse(new String(encrypted,"ISO-8859-1").contains("bakery"));
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsWrongPassword() throws Exception {
        byte[] encrypted=PortableBackupCrypto.encrypt("ledger".getBytes("UTF-8"),"StrongPass2026".toCharArray());
        PortableBackupCrypto.decrypt(encrypted,"WrongPass2026".toCharArray());
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsTamperedCiphertext() throws Exception {
        byte[] encrypted=PortableBackupCrypto.encrypt("ledger".getBytes("UTF-8"),"StrongPass2026".toCharArray());
        encrypted[encrypted.length-1]^=1;PortableBackupCrypto.decrypt(encrypted,"StrongPass2026".toCharArray());
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsShortPassword() throws Exception {
        PortableBackupCrypto.encrypt(new byte[]{1,2,3},"short".toCharArray());
    }
    @Test public void recognizesLegacyPlaintextBackupForCompatibility() {
        assertFalse(PortableBackupCrypto.isEncrypted("[]".getBytes()));
    }
}
