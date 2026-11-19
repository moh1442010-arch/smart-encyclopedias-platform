package com.mohammedmustafa.tirbal;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Holds the SQLCipher passphrase encrypted by a non-exportable Android Keystore AES key. */
final class DbKeyStore {
    private static final String PREFS = "tirbal_db_key";
    private static final String ALIAS = "com.mohammedmustafa.tirbal.database-key.v1";
    private static final String CIPHER = "ciphertext";
    private static final String IV = "iv";

    private DbKeyStore() {}

    static synchronized String getOrCreate(Context context) {
        try {
            SharedPreferences prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
            KeyStore store=KeyStore.getInstance("AndroidKeyStore");
            store.load(null);
            boolean hasStoredCipher=prefs.contains(CIPHER)&&prefs.contains(IV);
            if(!store.containsAlias(ALIAS)) {
                if(hasStoredCipher) throw new IllegalStateException("مفتاح تشفير قاعدة البيانات غير متاح. لا تنشئ مفتاحاً جديداً حتى لا تفقد البيانات.");
                KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
                generator.init(new KeyGenParameterSpec.Builder(ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setRandomizedEncryptionRequired(true)
                        .build());
                generator.generateKey();
                store.load(null);
            }
            SecretKey key=(SecretKey)store.getKey(ALIAS,null);
            byte[] raw;
            if(hasStoredCipher) {
                byte[] iv=Base64.decode(prefs.getString(IV,""),Base64.NO_WRAP);
                byte[] encrypted=Base64.decode(prefs.getString(CIPHER,""),Base64.NO_WRAP);
                Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,iv));
                raw=cipher.doFinal(encrypted);
                if(raw.length!=32) throw new IllegalStateException("طول مفتاح قاعدة البيانات غير صحيح.");
            } else {
                raw=new byte[32];
                new SecureRandom().nextBytes(raw);
                Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.ENCRYPT_MODE,key);
                byte[] encrypted=cipher.doFinal(raw);
                boolean saved=prefs.edit()
                        .putString(IV,Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP))
                        .putString(CIPHER,Base64.encodeToString(encrypted,Base64.NO_WRAP))
                        .commit();
                if(!saved) throw new IllegalStateException("تعذر حفظ مفتاح قاعدة البيانات بأمان.");
            }
            return Base64.encodeToString(raw,Base64.NO_WRAP);
        } catch(IllegalStateException e) {
            throw e;
        } catch(Exception e) {
            throw new IllegalStateException("تعذر فتح مفتاح تشفير قاعدة البيانات المحمي.",e);
        }
    }
}
