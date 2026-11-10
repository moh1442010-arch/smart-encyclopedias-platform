package com.mohammedmustafa.almizan.bakery;

import android.content.Context;
import android.database.Cursor;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import net.zetetic.database.sqlcipher.SQLiteOpenHelper;
import androidx.test.platform.app.InstrumentationRegistry;
import java.lang.reflect.Constructor;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class FactoryProductionAtomicityInstrumentedTest {
    private static long scalar(SQLiteDatabase db,String sql,String[] args) {
        Cursor c=db.rawQuery(sql,args);
        try { assertTrue(c.moveToFirst()); return c.getLong(0); }
        finally { c.close(); }
    }

    @Test public void productionFailureDoesNotChangeProductPriceAndSuccessCommitsPriceWithBatch() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String dbName="almizan_factory_1.db";
        context.deleteDatabase(dbName);
        context.deleteDatabase(dbName+"-wal");
        context.deleteDatabase(dbName+"-shm");
        SQLiteOpenHelper helper=null;
        try {
            Class<?> dbClass=Class.forName("com.mohammedmustafa.almizan.bakery.FactoryActivity$DB");
            Constructor<?> constructor=dbClass.getDeclaredConstructor(Context.class);
            constructor.setAccessible(true);
            helper=(SQLiteOpenHelper)constructor.newInstance(context);
            SQLiteDatabase db=helper.getWritableDatabase();
            db.execSQL("INSERT INTO products(id,name,category,unit_price,unit) VALUES(1,'Atomic Product','Test',123,'piece')");
            db.execSQL("INSERT INTO materials(id,name,unit,qty,value) VALUES(1,'Atomic Flour','kg',500,500)");
            db.execSQL("INSERT INTO recipe(product_id,material_id,qty) VALUES(1,1,1000)");

            java.lang.reflect.Method finalize=dbClass.getDeclaredMethod(
                "finalizeProduction",long.class,String.class,long.class,long.class,long.class,long.class,long.class);
            finalize.setAccessible(true);
            try {
                finalize.invoke(helper,1L,"BATCH-INSUFFICIENT",1L,1000L,200L,100L,777L);
                fail("Insufficient raw material must reject the production batch");
            } catch(java.lang.reflect.InvocationTargetException expected) {
                assertTrue(expected.getCause() instanceof IllegalArgumentException);
            }
            assertEquals("failed batch must preserve previous selling price",123L,
                scalar(db,"SELECT unit_price FROM products WHERE id=1",null));
            assertEquals("failed batch must preserve raw stock",500L,
                scalar(db,"SELECT qty FROM materials WHERE id=1",null));
            assertEquals(0L,scalar(db,"SELECT COUNT(*) FROM batches",null));

            db.execSQL("UPDATE materials SET qty=5000,value=5000 WHERE id=1");
            finalize.invoke(helper,1L,"BATCH-SUCCESS",1L,1000L,200L,100L,777L);
            assertEquals("successful batch commits its selling price",777L,
                scalar(db,"SELECT unit_price FROM products WHERE id=1",null));
            assertEquals("successful batch consumes recipe quantity",4000L,
                scalar(db,"SELECT qty FROM materials WHERE id=1",null));
            assertEquals(1L,scalar(db,"SELECT COUNT(*) FROM batches WHERE batch_ref='BATCH-SUCCESS'",null));
            assertEquals(1L,scalar(db,"SELECT COUNT(*) FROM finished WHERE product_id=1",null));
        } finally {
            if(helper!=null) helper.close();
            context.deleteDatabase(dbName);
            context.deleteDatabase(dbName+"-wal");
            context.deleteDatabase(dbName+"-shm");
        }
    }
}
