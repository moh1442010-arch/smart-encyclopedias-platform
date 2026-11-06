package com.mohammedmustafa.almizan.bakery;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.File;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/** SQLCipher key management and verified migration of existing plaintext SQLite files. */
final class SecureDatabase {
    private static final String PREFS="almizan_secure_database";
    private static final String WRAPPED_KEY="wrapped_sqlcipher_key_v1";
    private static final String KEY_ALIAS="almizan_sqlcipher_wrapping_key_v1";
    private static final Object LOCK=new Object();
    private static volatile boolean loaded;
    private SecureDatabase(){}

    static void initialize(Context context){
        if(!loaded)synchronized(LOCK){if(!loaded){System.loadLibrary("sqlcipher");loaded=true;}}
    }

    static byte[] passphrase(Context context,String name){
        synchronized(LOCK){
            initialize(context);
            try{
                SharedPreferences prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
                String stored=prefs.getString(WRAPPED_KEY,null);
                byte[] key;
                if(stored==null){
                    key=new byte[32];new SecureRandom().nextBytes(key);
                    String blob=Base64.encodeToString(wrap(context,key),Base64.NO_WRAP);
                    if(!prefs.edit().putString(WRAPPED_KEY,blob).commit())throw new IllegalStateException("Cannot persist wrapped SQLCipher key");
                } else key=unwrap(context,Base64.decode(stored,Base64.NO_WRAP));
                File dbFile=context.getDatabasePath(name);
                if(dbFile.exists()&&dbFile.length()>0&&!isEncrypted(dbFile,key))migratePlaintext(dbFile,key);
                return key.clone();
            }catch(Exception e){
                throw new IllegalStateException("تعذر فتح قاعدة البيانات المشفرة أو ترحيل القاعدة القديمة بأمان. لم يتم حذف الأصل.",e);
            }
        }
    }

    static SQLiteDatabase open(Context context,String name){
        byte[] key=passphrase(context,name);
        return SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name),key,null,null);
    }

    private static SecretKey wrappingKey(Context context)throws Exception{
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
        if(ks.containsAlias(KEY_ALIAS))return (SecretKey)ks.getKey(KEY_ALIAS,null);
        KeyGenerator gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        gen.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
            .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true).build());
        return gen.generateKey();
    }

    private static byte[] wrap(Context c,byte[] key)throws Exception{
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,wrappingKey(c));
        byte[] iv=cipher.getIV(),body=cipher.doFinal(key),out=new byte[iv.length+body.length];
        System.arraycopy(iv,0,out,0,iv.length);System.arraycopy(body,0,out,iv.length,body.length);return out;
    }

    private static byte[] unwrap(Context c,byte[] blob)throws Exception{
        if(blob==null||blob.length<29)throw new IllegalStateException("Invalid wrapped database key");
        byte[] iv=new byte[12],body=new byte[blob.length-12];
        System.arraycopy(blob,0,iv,0,12);System.arraycopy(blob,12,body,0,body.length);
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE,wrappingKey(c),new GCMParameterSpec(128,iv));return cipher.doFinal(body);
    }

    private static boolean isEncrypted(File file,byte[] key){
        SQLiteDatabase db=null;
        try{
            db=SQLiteDatabase.openDatabase(file.getAbsolutePath(),key,null,SQLiteDatabase.OPEN_READWRITE,null,null);
            Cursor c=db.rawQuery("PRAGMA schema_version",null);try{return c.moveToFirst();}finally{c.close();}
        }catch(Exception ignored){return false;}finally{if(db!=null)db.close();}
    }

    private static void migratePlaintext(File original,byte[] key)throws Exception{
        android.database.sqlite.SQLiteDatabase source=null;SQLiteDatabase target=null;
        File temp=new File(original.getParentFile(),original.getName()+".cipher-new");
        File legacy=new File(original.getParentFile(),original.getName()+".plaintext-migration");
        if(temp.exists()&&!temp.delete())throw new IllegalStateException("Cannot clear migration temp file");
        if(legacy.exists())throw new IllegalStateException("A prior migration copy needs recovery");
        List<String> objects=new ArrayList<>(),tables=new ArrayList<>();
        try{
            source=android.database.sqlite.SQLiteDatabase.openDatabase(original.getAbsolutePath(),null,android.database.sqlite.SQLiteDatabase.OPEN_READWRITE);
            Cursor checkpoint=source.rawQuery("PRAGMA wal_checkpoint(FULL)",null);checkpoint.close();
            Cursor schema=source.rawQuery("SELECT type,name,sql FROM sqlite_master WHERE sql IS NOT NULL AND name NOT LIKE 'sqlite_%' ORDER BY CASE type WHEN 'table' THEN 0 WHEN 'index' THEN 1 WHEN 'trigger' THEN 2 ELSE 3 END",null);
            try{while(schema.moveToNext()){String type=schema.getString(0),sql=schema.getString(2);if("table".equals(type))tables.add(schema.getString(1));objects.add(type+"\n"+sql);}}finally{schema.close();}
            target=SQLiteDatabase.openOrCreateDatabase(temp,key,null,null);
            target.rawExecSQL("PRAGMA foreign_keys=OFF");target.beginTransaction();
            try{
                for(String o:objects)if(o.startsWith("table\n"))target.execSQL(o.substring(6));
                for(String table:tables)copyTable(source,target,table);
                for(String o:objects)if(!o.startsWith("table\n"))target.execSQL(o.substring(o.indexOf('\n')+1));
                target.setVersion(source.getVersion());target.setTransactionSuccessful();
            }finally{target.endTransaction();}
            for(String table:tables)if(count(source,table)!=count(target,table))throw new IllegalStateException("Row count mismatch: "+table);
            target.close();target=null;source.close();source=null;
            new File(original.getAbsolutePath()+"-wal").delete();
            new File(original.getAbsolutePath()+"-shm").delete();
            if(!original.renameTo(legacy))throw new IllegalStateException("Cannot preserve original database");
            if(!temp.renameTo(original)){legacy.renameTo(original);throw new IllegalStateException("Cannot replace database with encrypted copy");}
            if(!isEncrypted(original,key)){original.delete();legacy.renameTo(original);throw new IllegalStateException("Encrypted copy verification failed; original restored");}
            if(!legacy.delete())throw new IllegalStateException("Encrypted database verified, but plaintext migration copy could not be removed");
            new File(original.getAbsolutePath()+"-wal").delete();new File(original.getAbsolutePath()+"-shm").delete();
        }finally{if(target!=null)target.close();if(source!=null)source.close();if(temp.exists())temp.delete();}
    }

    private static void copyTable(android.database.sqlite.SQLiteDatabase source,SQLiteDatabase target,String table){
        String q="["+table.replace("]","]]")+"]";
        Cursor info=source.rawQuery("PRAGMA table_info("+q+")",null);ArrayList<String> cols=new ArrayList<>();
        try{while(info.moveToNext())cols.add(info.getString(1));}finally{info.close();}
        Cursor rows=source.rawQuery("SELECT * FROM "+q,null);
        try{while(rows.moveToNext()){ContentValues values=new ContentValues();for(int i=0;i<cols.size();i++){String col=cols.get(i);switch(rows.getType(i)){
            case Cursor.FIELD_TYPE_NULL:values.putNull(col);break;
            case Cursor.FIELD_TYPE_INTEGER:values.put(col,rows.getLong(i));break;
            case Cursor.FIELD_TYPE_FLOAT:values.put(col,rows.getDouble(i));break;
            case Cursor.FIELD_TYPE_BLOB:values.put(col,rows.getBlob(i));break;
            default:values.put(col,rows.getString(i));break;
        }}target.insertOrThrow(table,null,values);}}finally{rows.close();}
    }

    private static long count(android.database.sqlite.SQLiteDatabase db,String table){
        Cursor c=db.rawQuery("SELECT COUNT(*) FROM ["+table.replace("]","]]")+"]",null);
        try{return c.moveToFirst()?c.getLong(0):-1;}finally{c.close();}
    }
    private static long count(SQLiteDatabase db,String table){
        Cursor c=db.rawQuery("SELECT COUNT(*) FROM ["+table.replace("]","]]")+"]",null);
        try{return c.moveToFirst()?c.getLong(0):-1;}finally{c.close();}
    }
}
