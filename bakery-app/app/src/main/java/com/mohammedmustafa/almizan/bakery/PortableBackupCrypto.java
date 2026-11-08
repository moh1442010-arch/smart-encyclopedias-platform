package com.mohammedmustafa.almizan.bakery;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/** Portable password-encrypted backup envelope; the password is never stored by the app. */
final class PortableBackupCrypto {
    private static final byte[] MAGIC = "MIZANBK1".getBytes(StandardCharsets.US_ASCII);
    private static final int VERSION = 1;
    private static final int SALT_BYTES = 16;
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final int ITERATIONS = 210000;
    private PortableBackupCrypto() {}

    static boolean isEncrypted(byte[] data) {
        if (data == null || data.length < MAGIC.length) return false;
        for (int i=0;i<MAGIC.length;i++) if (data[i] != MAGIC[i]) return false;
        return true;
    }

    static byte[] encrypt(byte[] plaintext, char[] password) throws Exception {
        if (plaintext == null || password == null || password.length < 10) {
            throw new IllegalArgumentException("Backup password must contain at least 10 characters");
        }
        byte[] salt = new byte[SALT_BYTES], iv = new byte[IV_BYTES];
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt); random.nextBytes(iv);
        byte[] header = new byte[MAGIC.length + 1 + SALT_BYTES + IV_BYTES];
        System.arraycopy(MAGIC,0,header,0,MAGIC.length);
        header[MAGIC.length] = (byte)VERSION;
        System.arraycopy(salt,0,header,MAGIC.length+1,SALT_BYTES);
        System.arraycopy(iv,0,header,MAGIC.length+1+SALT_BYTES,IV_BYTES);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password,salt), new GCMParameterSpec(TAG_BITS,iv));
        cipher.updateAAD(header);
        byte[] ciphertext = cipher.doFinal(plaintext);
        ByteArrayOutputStream out = new ByteArrayOutputStream(header.length+ciphertext.length);
        out.write(header); out.write(ciphertext);
        Arrays.fill(salt,(byte)0); Arrays.fill(iv,(byte)0);
        return out.toByteArray();
    }

    static byte[] decrypt(byte[] envelope, char[] password) throws Exception {
        int headerLength = MAGIC.length + 1 + SALT_BYTES + IV_BYTES;
        if (!isEncrypted(envelope) || envelope.length < headerLength + 16 || password == null || password.length < 10) {
            throw new IllegalArgumentException("Invalid encrypted backup or password");
        }
        if ((envelope[MAGIC.length] & 0xff) != VERSION) throw new IllegalArgumentException("Unsupported backup encryption version");
        byte[] header = Arrays.copyOfRange(envelope,0,headerLength);
        byte[] salt = Arrays.copyOfRange(envelope,MAGIC.length+1,MAGIC.length+1+SALT_BYTES);
        byte[] iv = Arrays.copyOfRange(envelope,MAGIC.length+1+SALT_BYTES,headerLength);
        byte[] ciphertext = Arrays.copyOfRange(envelope,headerLength,envelope.length);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,deriveKey(password,salt),new GCMParameterSpec(TAG_BITS,iv));
            cipher.updateAAD(header);
            return cipher.doFinal(ciphertext);
        } catch (AEADBadTagException badPasswordOrTamper) {
            throw new IllegalArgumentException("Backup password is incorrect or file integrity check failed");
        } finally {
            Arrays.fill(salt,(byte)0); Arrays.fill(iv,(byte)0);
        }
    }

    private static SecretKeySpec deriveKey(char[] password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password,salt,ITERATIONS,256);
        try {
            byte[] raw = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).getEncoded();
            try { return new SecretKeySpec(raw,"AES"); }
            finally { Arrays.fill(raw,(byte)0); }
        } finally { spec.clearPassword(); }
    }
}
