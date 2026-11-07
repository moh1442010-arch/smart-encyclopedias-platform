package com.mohammedmustafa.almizan.bakery;

import android.content.ContentValues;
import android.database.Cursor;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Arrays;

/** Portable snapshot for offline party master records and company receiving accounts. */
final class PartyDirectoryBackup {
    static final String TYPE="almizan_party_directory_backup";
    private static final String[] TABLES={"customers","suppliers","company_bank_accounts","party_payments","party_opening_payments","supplier_quick_invoices","purchase_invoices","purchase_lines","supplier_payments"};
    private static final String[][] COLUMNS={
        {"id","customer_code","name","phone","address","opening_balance_minor","currency_code","credit_limit_minor","active","notes","created_at"},
        {"id","supplier_code","name","phone","address","opening_balance_minor","currency_code","active","notes","created_at","default_item","default_unit"},
        {"id","account_name","bank_name","currency_code","iban","swift_bic","account_number","is_default","active","created_at"},
        {"id","party_type","party_id","direction","payment_date","amount_minor","method","currency_code","reference","created_at"},
        {"id","party_type","party_id","payment_date","amount_minor","method","currency_code","reference","created_at"},
        {"id","supplier_id","item_name","quantity_milli","invoice_total_minor","paid_cash_minor","paid_bank_minor","bank_reference","debt_minor","currency_code","invoice_date","created_at"},
        {"id","invoice_no","supplier_id","branch_id","invoice_date","due_date","currency_code","fx_rate_num","fx_rate_den","subtotal_minor","tax_minor","total_minor","paid_minor","status","notes","created_at"},
        {"id","invoice_id","item_name","unit","quantity_milli","unit_cost_minor","line_total_minor","inventory_item_id","created_at"},
        {"id","supplier_id","invoice_id","payment_date","amount_minor","currency_code","method","reference","branch_id","journal_ref","created_at"}
    };
    private PartyDirectoryBackup(){}

    static JSONObject export(SQLiteDatabase db)throws Exception{
        EnterpriseSchema.ensure(db);
        JSONObject root=new JSONObject();root.put("_type",TYPE);root.put("schema_version",1);
        JSONObject tables=new JSONObject();
        for(int i=0;i<TABLES.length;i++){
            JSONArray rows=new JSONArray();Cursor c=db.rawQuery("SELECT * FROM "+TABLES[i],null);
            try{while(c.moveToNext()){
                JSONObject row=new JSONObject();
                for(int j=0;j<c.getColumnCount();j++){
                    int type=c.getType(j);String name=c.getColumnName(j);
                    if(type==Cursor.FIELD_TYPE_NULL)row.put(name,JSONObject.NULL);
                    else if(type==Cursor.FIELD_TYPE_INTEGER)row.put(name,c.getLong(j));
                    else if(type==Cursor.FIELD_TYPE_FLOAT)row.put(name,c.getDouble(j));
                    else if(type==Cursor.FIELD_TYPE_BLOB)throw new IllegalStateException("Unsupported BLOB in party backup");
                    else row.put(name,c.getString(j));
                }
                rows.put(row);
            }}finally{c.close();}
            tables.put(TABLES[i],rows);
        }
        root.put("tables",tables);return root;
    }

    static void validate(JSONObject root)throws Exception{
        if(root==null||!TYPE.equals(root.optString("_type"))||root.optInt("schema_version")!=1)throw new IllegalArgumentException("Unsupported party directory backup");
        JSONObject tables=root.optJSONObject("tables");if(tables==null)throw new IllegalArgumentException("Party backup tables missing");
        for(int i=0;i<TABLES.length;i++){
            JSONArray rows=tables.optJSONArray(TABLES[i]);if(rows==null)throw new IllegalArgumentException("Missing party table "+TABLES[i]);
            for(int n=0;n<rows.length();n++){
                JSONObject row=rows.getJSONObject(n);
                for(String col:COLUMNS[i])if(!row.has(col))throw new IllegalArgumentException("Missing party field "+TABLES[i]+"."+col);
                for(java.util.Iterator<String> it=row.keys();it.hasNext();)if(!Arrays.asList(COLUMNS[i]).contains(it.next()))throw new IllegalArgumentException("Unknown party backup field");
                long id=row.getLong("id");if(id<=0)throw new IllegalArgumentException("Invalid party record ID");
                if("customers".equals(TABLES[i])||"suppliers".equals(TABLES[i])){
                    if(row.optString("name").trim().isEmpty()||row.optString("currency_code").length()!=3||row.getLong("opening_balance_minor")<0)throw new IllegalArgumentException("Invalid party master record");
                }
                if("company_bank_accounts".equals(TABLES[i])){
                    if(row.optString("account_name").trim().isEmpty()||row.optString("bank_name").trim().isEmpty()||row.optString("currency_code").length()!=3)throw new IllegalArgumentException("Invalid company bank account");
                }
                if("party_payments".equals(TABLES[i])||"party_opening_payments".equals(TABLES[i])){
                    if(row.getLong("party_id")<=0||row.getLong("amount_minor")<=0)throw new IllegalArgumentException("Invalid party payment");
                }
            }
        }
    }

    static void restore(SQLiteDatabase db,JSONObject root)throws Exception{
        validate(root);EnterpriseSchema.ensure(db);
        boolean own=!db.inTransaction();if(own)db.beginTransaction();
        try{
            JSONObject tables=root.getJSONObject("tables");
            for(int i=TABLES.length-1;i>=0;i--)db.delete(TABLES[i],null,null);
            for(int i=0;i<TABLES.length;i++){
                JSONArray rows=tables.getJSONArray(TABLES[i]);
                for(int n=0;n<rows.length();n++){
                    JSONObject row=rows.getJSONObject(n);ContentValues v=new ContentValues();
                    for(String col:COLUMNS[i]){
                        Object value=row.get(col);
                        if(value==JSONObject.NULL)v.putNull(col);
                        else if(value instanceof Number)v.put(col,((Number)value).longValue());
                        else v.put(col,String.valueOf(value));
                    }
                    db.insertOrThrow(TABLES[i],null,v);
                }
            }
            if(own)db.setTransactionSuccessful();
        }finally{if(own)db.endTransaction();}
    }
}
