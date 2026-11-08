package com.mohammedmustafa.almizan.bakery;

import android.database.Cursor;
import net.zetetic.database.sqlcipher.SQLiteDatabase;

/**
 * Server-side-equivalent authorization boundary for local workflows.
 * Call from the data/service layer before every protected read or write;
 * hiding a screen or button is not an authorization check.
 */
final class PermissionService {
    private PermissionService() {}

    static boolean hasPermission(SQLiteDatabase db,String username,String branchCode,String permissionCode) {
        if(db==null||blank(username)||blank(branchCode)||blank(permissionCode))return false;
        String sql="SELECT 1 FROM users u "+
            "JOIN user_roles ur ON ur.user_id=u.id "+
            "JOIN roles r ON r.id=ur.role_id "+
            "JOIN role_permissions rp ON rp.role_id=r.id "+
            "JOIN branches b ON b.id=ur.branch_id "+
            "WHERE u.username=? AND u.active=1 AND b.branch_code=? AND b.active=1 "+
            "AND rp.permission_code=? LIMIT 1";
        Cursor c=db.rawQuery(sql,new String[]{username.trim(),branchCode.trim(),permissionCode.trim()});
        try{return c.moveToFirst();}finally{c.close();}
    }

    static void requirePermission(SQLiteDatabase db,String username,String branchCode,String permissionCode) {
        if(!hasPermission(db,username,branchCode,permissionCode))
            throw new SecurityException("غير مصرح بتنفيذ العملية: "+permissionCode);
    }

    private static boolean blank(String value){return value==null||value.trim().isEmpty();}
}
