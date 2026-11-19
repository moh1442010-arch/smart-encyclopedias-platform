package com.mohammedmustafa.tirbal;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.os.SystemClock;
import android.util.Base64;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.UUID;

final class LicenseManager {
    private static final String API = "https://hisab-al-tirbal-licensing.mohammedmustafa.workers.dev";
    private static final String APP_ID = "com.mohammedmustafa.tirbal";
    private static final String PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtXiJgeh0o+EMZfmqPnB5+ewloD9QpftQv9S3SWI2WwG/zr748BOXMI1VXf2xbHzsLpyBHqivm4YGvsmFqurUx2oKF9Bekyy6rjLVQ7DrTTB/jVhIVDoeJPah+rQmrPvZfdqYzKokY0TqztElkWW0bwq+dzuEbmU17J3NfLQM8iXjXIvSqcLLd59syK/bEQn90JmejKarE/u/6zw4Btb5fSxgb7Up6nVczvsaMiJoq7BAuMdEHH8SWV0f75VAZzSv45L4HjhWP9aSyEVl6aQ8ClAjgqbkDFEJj9hY6G86sClLSFgDqwwzDOObLa6rJQN3JuZVJb2Tvyqf5XTfFXLcFwIDAQAB";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final String PREF = "tirbal_license";
    interface Callback { void complete(Result result); }
    interface JsonCallback { void complete(JSONObject result, String error); }
    static final class Result {
        final boolean unlocked;
        final boolean expired;
        final String deviceId;
        final String message;
        final long expires;
        Result(boolean ok, boolean expired, String device, String message, long expires) {
            this.unlocked=ok; this.expired=expired; this.deviceId=device; this.message=message; this.expires=expires;
        }
    }

    static void check(Context context, Callback callback) {
        Context app=context.getApplicationContext();
        EXECUTOR.execute(() -> {
            String device=deviceId(app);
            try {
                JSONObject body=new JSONObject().put("deviceId",device);
                JSONObject response=request("POST","/api/bootstrap",body);
                String status=response.optString("status","");
                String token=response.optString("token","");
                long server=response.optLong("serverTime",0);
                if ("trial".equals(status)||"licensed".equals(status)) {
                    long expires="trial".equals(status)?response.optLong("expires",0):tokenExpiry(token);
                    if (!verifyToken(token,device,server)) throw new Exception("INVALID_LICENSE_SIGNATURE");
                    save(app,token,server);
                    callback.complete(new Result(true,false,device,"صالح",expires));
                } else if ("expired".equals(status)) {
                    callback.complete(new Result(false,true,device,"انتهت الفترة التجريبية. اختر اشتراكاً للمتابعة.",0));
                } else callback.complete(new Result(false,false,device,"تعذر التحقق من الترخيص.",0));
            } catch(Exception network) {
                String cached=prefs(app).getString("token","");
                if (!cached.isEmpty() && cachedValid(app)) {
                    callback.complete(new Result(true,false,device,"وضع عدم الاتصال. يلزم الاتصال دورياً للتحقق من الاشتراك.",tokenExpiry(cached)));
                } else callback.complete(new Result(false,false,device,"يلزم الاتصال بالإنترنت لأول تحقق من الترخيص أو بعد إعادة تشغيل الهاتف.",0));
            }
        });
    }

    static void loadConfig(JsonCallback callback) {
        EXECUTOR.execute(() -> {
            try { callback.complete(request("GET","/api/config",null),null); }
            catch(Exception e) { callback.complete(null,"تعذر تحميل بيانات الاشتراك والدفع."); }
        });
    }

    static void submitOrder(String customer,String contact,String plan,String receipt,String device,JsonCallback callback) {
        EXECUTOR.execute(() -> {
            try {
                JSONObject body=new JSONObject().put("customerName",customer).put("contact",contact)
                        .put("plan",plan).put("receiptReference",receipt).put("deviceId",device);
                callback.complete(request("POST","/api/orders",body),null);
            } catch(Exception e) { callback.complete(null,"تعذر إرسال طلب الاشتراك. تحقق من الاتصال والبيانات."); }
        });
    }

    private static JSONObject request(String method,String path,JSONObject body) throws Exception {
        HttpURLConnection connection=(HttpURLConnection)new URL(API+path).openConnection();
        connection.setRequestMethod(method); connection.setConnectTimeout(12000); connection.setReadTimeout(12000);
        connection.setRequestProperty("Accept","application/json");
        if(body!=null) {
            connection.setDoOutput(true); connection.setRequestProperty("Content-Type","application/json; charset=utf-8");
            try(OutputStream out=connection.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}
        }
        int code=connection.getResponseCode();
        InputStream stream=code>=200&&code<300?connection.getInputStream():connection.getErrorStream();
        String text=read(stream); connection.disconnect();
        JSONObject result=new JSONObject(text);
        if(code<200||code>=300) throw new Exception(result.optString("error","HTTP_"+code));
        return result;
    }

    private static String read(InputStream in) throws Exception {
        if(in==null)return "{}";
        try(InputStream input=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] b=new byte[4096];int n;while((n=input.read(b))!=-1)out.write(b,0,n);
            return out.toString("UTF-8");
        }
    }

    private static boolean verifyToken(String token,String device,long serverTime) throws Exception {
        String[] parts=token.split("\\.",-1); if(parts.length!=2)return false;
        byte[] payload=Base64.decode(pad(parts[0]),Base64.URL_SAFE|Base64.NO_WRAP);
        byte[] signature=Base64.decode(pad(parts[1]),Base64.URL_SAFE|Base64.NO_WRAP);
        java.security.PublicKey key=KeyFactory.getInstance("RSA").generatePublic(
                new X509EncodedKeySpec(Base64.decode(PUBLIC_KEY,Base64.DEFAULT)));
        Signature verifier=Signature.getInstance("SHA256withRSA");
        verifier.initVerify(key); verifier.update(payload);
        if(!verifier.verify(signature))return false;
        String value=new String(payload,StandardCharsets.UTF_8);
        String[] fields=value.split(";");
        String app="",boundDevice="",plan="";long expires=-1;
        for(String field:fields){int i=field.indexOf('=');if(i<1)continue;String k=field.substring(0,i),v=field.substring(i+1);
            if(k.equals("app"))app=v;else if(k.equals("device"))boundDevice=v;else if(k.equals("plan"))plan=v;else if(k.equals("expires"))expires=Long.parseLong(v);}
        return APP_ID.equals(app)&&device.equals(boundDevice)&&
                ("trial".equals(plan)||"monthly".equals(plan)||"annual".equals(plan)||"lifetime".equals(plan))&&
                (expires==0||expires>serverTime);
    }

    private static long tokenExpiry(String token) {
        try {String p=token.split("\\.",-1)[0];String value=new String(Base64.decode(pad(p),Base64.URL_SAFE|Base64.NO_WRAP),StandardCharsets.UTF_8);
            for(String f:value.split(";"))if(f.startsWith("expires="))return Long.parseLong(f.substring(8));
        }catch(Exception ignored){} return 0;
    }
    private static String pad(String value){StringBuilder b=new StringBuilder(value);int n=(4-value.length()%4)%4;for(int i=0;i<n;i++)b.append('=');return b.toString();}
    private static String deviceId(Context c) {
        String id=Settings.Secure.getString(c.getContentResolver(),Settings.Secure.ANDROID_ID);
        if(id!=null&&!id.trim().isEmpty())return id;
        String fallback=prefs(c).getString("fallback_device_id","");
        if(fallback.isEmpty()){fallback=UUID.randomUUID().toString().replace("-","");prefs(c).edit().putString("fallback_device_id",fallback).apply();}
        return fallback;
    }
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences(PREF,Context.MODE_PRIVATE);}
    static boolean cachedValid(Context context) {
        String token=prefs(context).getString("token","");
        if(token.isEmpty())return false;
        try {
            if(tokenExpiry(token)==0)return verifyToken(token,deviceId(context),0);
            long serverTime=prefs(context).getLong("server_time",0);
            long savedElapsed=prefs(context).getLong("elapsed",-1);
            long currentElapsed=SystemClock.elapsedRealtime();
            long savedBoot=prefs(context).getLong("boot_count",-1);
            long currentBoot=bootCount(context);
            if(serverTime<=0||savedElapsed<0||currentElapsed<savedElapsed)return false;
            if(savedBoot>=0&&currentBoot>=0&&savedBoot!=currentBoot)return false;
            long trustedNow=serverTime+(currentElapsed-savedElapsed)/1000;
            return verifyToken(token,deviceId(context),trustedNow);
        } catch(Exception e) { return false; }
    }

    private static long bootCount(Context context) {
        if(android.os.Build.VERSION.SDK_INT<24)return -1;
        try{return android.provider.Settings.Global.getInt(context.getContentResolver(),"boot_count",-1);}
        catch(Exception e){return -1;}
    }

    private static void save(Context c,String token,long serverTime) {
        prefs(c).edit().putString("token",token).putLong("server_time",serverTime)
                .putLong("elapsed",SystemClock.elapsedRealtime()).putLong("boot_count",bootCount(c)).apply();
    }
}
