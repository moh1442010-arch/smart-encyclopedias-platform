package com.mohammedmustafa.almizan.bakery;

import android.content.Context;
import android.database.Cursor;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import net.zetetic.database.sqlcipher.SQLiteOpenHelper;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class FactoryPartyIdentityInstrumentedTest {
    private Object openFactoryHelper(Context context) throws Exception {
        Class<?> type=Class.forName("com.mohammedmustafa.almizan.bakery.FactoryActivity$DB");
        Constructor<?> constructor=type.getDeclaredConstructor(Context.class);
        constructor.setAccessible(true);
        return constructor.newInstance(context);
    }

    private Object invoke(Object target,String name,Class<?>[] types,Object... args) throws Exception {
        Method method=target.getClass().getDeclaredMethod(name,types);
        method.setAccessible(true);
        try { return method.invoke(target,args); }
        catch(InvocationTargetException ex) {
            Throwable cause=ex.getCause();
            if(cause instanceof Exception)throw (Exception)cause;
            throw ex;
        }
    }

    private void expectIdentityRejection(Object db,String method,Class<?>[] types,Object... args) throws Exception {
        try {
            invoke(db,method,types,args);
            fail(method+" must reject a party name that does not match its master ID");
        } catch(IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("لا يطابق"));
        }
    }

    private long count(SQLiteDatabase db,String table) {
        Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+table,null);
        try { assertTrue(c.moveToFirst());return c.getLong(0); }
        finally { c.close(); }
    }

    @Test public void salesCollectionsPurchasesAndSupplierPaymentsRejectMismatchedPartyIdentity() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String database="almizan_factory_1.db";
        context.deleteDatabase(database);
        context.deleteDatabase(database+"-wal");
        context.deleteDatabase(database+"-shm");
        Object helper=openFactoryHelper(context);
        SQLiteDatabase db=null;
        try {
            db=((SQLiteOpenHelper)helper).getWritableDatabase();
            Class<?> longType=Long.TYPE;
            long customerId=(Long)invoke(helper,"addFactoryParty",
                new Class<?>[]{String.class,String.class,String.class,String.class},
                "customer","Customer A","","");
            long supplierId=(Long)invoke(helper,"addFactoryParty",
                new Class<?>[]{String.class,String.class,String.class,String.class},
                "supplier","Supplier A","","");

            expectIdentityRejection(helper,"sell",
                new Class<?>[]{longType,longType,longType,longType,longType,longType,String.class,String.class,String.class},
                1L,customerId,1L,100L,0L,0L,"Customer B","","التوزيع");
            expectIdentityRejection(helper,"collect",
                new Class<?>[]{longType,String.class,longType,longType,String.class},
                customerId,"Customer B",1L,0L,"");
            expectIdentityRejection(helper,"receive",
                new Class<?>[]{String.class,String.class,longType,longType,longType,String.class,longType,longType,String.class},
                "Flour","kg",1000L,100L,supplierId,"Supplier B",0L,0L,"");
            expectIdentityRejection(helper,"paySupplier",
                new Class<?>[]{longType,String.class,longType,longType,String.class},
                supplierId,"Supplier B",1L,0L,"");

            assertEquals(0,count(db,"sales"));
            assertEquals(0,count(db,"collections"));
            assertEquals(0,count(db,"supplier_purchase_records"));
            assertEquals(0,count(db,"supplier_payment_records"));
        } finally {
            if(db!=null&&db.isOpen())db.close();
            ((SQLiteOpenHelper)helper).close();
            context.deleteDatabase(database);
            context.deleteDatabase(database+"-wal");
            context.deleteDatabase(database+"-shm");
        }
    }
}
