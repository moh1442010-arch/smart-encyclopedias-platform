package com.mohammedmustafa.almizan.bakery;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Shared entitlement gate used by every protected Android Activity. */
public final class LicenseAccessGate {
    private LicenseAccessGate() {}
    private static final String[] PUBLIC_KEYS = {
        "MIIBojANBgkqhkiG9w0BAQEFAAOCAY8AMIIBigKCAYEAnWLF2Y4nmN7MWU1q9tzTVJCCacOf7iaGiRYrA21Ej7Xp7vV2/BgMu7N3OmcB0UJyIn1A22z34fKf3YEEDTjBz4pHHEtXeB+bjH+jn7VOcwQS9mZhTUR1+/2gYJxIKUEz9233Ab1eupGcCzuziQck362qiRBdjlHEViW0koE87TmPKRkkqzKtkOm0MzIfYr7tM/DmqncZ9RKUnctvoJd05kDxg0PXdNZ539Ip7tT6y11nmakYg5lJv9XOqNWnQbPRUDjOO9jWIPFnUvaPVofYrBOENPTu8S7414Twb7/0zmhFgNuizHbZQEOYD3ZNK0GfNDYH8Tk1ZEe9H7WHE3+B0UYkCBEfJCdXkHxRIJLb1KSyxpLw7iiGxAJdCQ95SNkcYfl6KTsZFPtt4pssXRH9XvJsvQEmP7OmqtPSJy0ZFSL/KgS5T3EIeuG2DyJom5WJ8lya4zy3noU9hL8gSjpzOdzR87KqiOYSy890nwtvHIi73qxvNpPGxPghG3JIEEBFAgMBAAE=",
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvsK66CYxt4X7R9GcVaJ0FjrheMAkFpzbirdz7/X/lj8UAuqUabNOPN3R5dGuZro90i+raIyngTz+VxRxLcNPRQru4VzWIDLsoBEwi/CrsOaMJFgVLom7jYXFjQ5P25TO5hRxzPN8ZOOG1MTo8j6ipgPOKY5be6Gmsj8R03SbulYnVO8STWC4bSkqwvV9YlEIpZwnyAJVbWHbE4JSKiCC/TMOcfrBgGG0W7qCJeEjnUKPfHAtH85ayZjBQwMIQx+z1CoTFWrqKXywSWN65G7yZbHwsnREovRprxjyVHqmTtLd45VyNQU2zXBKchzkwnpiG1zeD8kfdDeACeaAmxbsiwIDAQAB"
    };
    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("MainActivity_preferences", Context.MODE_PRIVATE);
    }
    private static boolean validToken(Context c, String token) {
        try {
            String[] parts=token.trim().split("\\.",-1);
            if(parts.length!=2)return false;
            int flags=Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING;
            byte[] payloadBytes=Base64.decode(parts[0],flags);
            byte[] signatureBytes=Base64.decode(parts[1],flags);
            boolean verified=false;
            for(String encoded:PUBLIC_KEYS) {
                try {
                    java.security.PublicKey key=KeyFactory.getInstance("RSA").generatePublic(
                        new X509EncodedKeySpec(Base64.decode(encoded,Base64.NO_WRAP)));
                    Signature sig=Signature.getInstance("SHA256withRSA");
                    sig.initVerify(key);sig.update(payloadBytes);
                    if(sig.verify(signatureBytes)){verified=true;break;}
                } catch(Exception ignored) {}
            }
            if(!verified)return false;
            Map<String,String> fields=new HashMap<>();
            for(String field:new String(payloadBytes,StandardCharsets.UTF_8).split(";")) {
                int at=field.indexOf('=');if(at<=0)return false;
                fields.put(field.substring(0,at),field.substring(at+1));
            }
            if(!"com.mohammedmustafa.almizan.bakery".equals(fields.get("app")))return false;
            String androidId=Settings.Secure.getString(c.getContentResolver(),Settings.Secure.ANDROID_ID);
            String current=(androidId!=null&&androidId.matches("(?i)[0-9a-f]{16}"))?"a"+androidId.toLowerCase(Locale.US):null;
            String legacy=prefs(c).getString("license_device_id",null);
            String licensed=fields.get("device");
            if(!((current!=null&&current.equals(licensed))||(legacy!=null&&legacy.equals(licensed))))return false;
            String plan=fields.get("plan");
            if(!"monthly".equals(plan)&&!"annual".equals(plan)&&!"lifetime".equals(plan))return false;
            String max=fields.get("max_version");
            if(!"any".equals(max)&&!("lifetime".equals(plan)&&compatibleLifetimeVersion(max)))return false;
            long expires=Long.parseLong(fields.get("expires"));
            if(expires==0L)return true;
            long now=System.currentTimeMillis()/1000L;
            long seen=prefs(c).getLong("license_last_seen",0L);
            if(seen>0L&&now<seen-300L)return false;
            prefs(c).edit().putLong("license_last_seen",Math.max(seen,now)).apply();
            return now<=expires;
        } catch(Exception ex) { return false; }
    }
    public static boolean isAccessAllowed(Context c) {
        SharedPreferences p=prefs(c);
        String token=p.getString("license_token",null);
        if(token!=null&&validToken(c,token))return true;
        long now=System.currentTimeMillis();
        long start=p.getLong("trial_start",0L);
        long seen=p.getLong("trial_last_seen",0L);
        if(start<=0L) { p.edit().putLong("trial_start",now).putLong("trial_last_seen",now).apply();return true; }
        if(seen>0L&&now<seen-300000L)p.edit().putBoolean("trial_clock_rollback_detected",true).apply();
        if(p.getBoolean("trial_clock_rollback_detected",false))return false;
        p.edit().putLong("trial_last_seen",Math.max(seen,now)).apply();
        return now-start<7L*24L*60L*60L*1000L;
    }
}
