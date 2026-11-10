package com.mohammedmustafa.almizan.bakery;

import android.content.Context;
import android.database.Cursor;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class FinancialPeriodServiceInstrumentedTest {
    @Test public void periodCloseAndReopenRequireRolePermissionAndWriteAudit() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name="financial_period_control_"+System.currentTimeMillis()+".db";
        SQLiteDatabase db=SecureDatabase.open(context,name);
        try {
            EnterpriseSchema.ensure(db);
            db.execSQL("INSERT INTO users(username,display_name,pin_salt,pin_hash,created_at) VALUES('period-owner','Period Owner',X'01',X'02',1)");
            long userId=scalar(db,"SELECT id FROM users WHERE username='period-owner'");
            long roleId=scalar(db,"SELECT id FROM roles WHERE role_code='OWNER'");
            long branchId=scalar(db,"SELECT id FROM branches WHERE branch_code='MAIN'");
            db.execSQL("INSERT INTO user_roles(user_id,role_id,branch_id) VALUES(?,?,?)",
                new Object[]{userId,roleId,branchId});

            FinancialPeriodService.open(db,"FY-2026","2026-01-01","2026-12-31");
            try {
                FinancialPeriodService.close(db,"FY-2026","not-authorized","MAIN");
                fail("Unauthorized actor must not close a financial period");
            } catch(SecurityException expected) { }
            FinancialPeriodService.close(db,"FY-2026","period-owner","MAIN");
            assertEquals("CLOSED",periodStatus(db,"FY-2026"));

            try {
                FinancialPeriodService.reopen(db,"FY-2026","not-authorized","MAIN");
                fail("Unauthorized actor must not reopen a financial period");
            } catch(SecurityException expected) { }
            FinancialPeriodService.reopen(db,"FY-2026","period-owner","MAIN");
            assertEquals("OPEN",periodStatus(db,"FY-2026"));
            assertEquals(3L,scalar(db,"SELECT COUNT(*) FROM enterprise_audit_log WHERE entity_type='FINANCIAL_PERIOD' AND entity_ref='FY-2026'"));
            assertEquals(0L,scalar(db,"SELECT COUNT(*) FROM enterprise_audit_log WHERE actor='not-authorized'"));
        } finally {
            db.close();context.deleteDatabase(name);context.deleteDatabase(name+"-wal");context.deleteDatabase(name+"-shm");
        }
    }

    private static long scalar(SQLiteDatabase db,String sql) {
        Cursor c=db.rawQuery(sql,null);
        try { assertTrue(c.moveToFirst());return c.getLong(0); }
        finally { c.close(); }
    }

    private static String periodStatus(SQLiteDatabase db,String code) {
        Cursor c=db.rawQuery("SELECT status FROM financial_periods WHERE period_code=?",new String[]{code});
        try { assertTrue(c.moveToFirst());return c.getString(0); }
        finally { c.close(); }
    }
}
