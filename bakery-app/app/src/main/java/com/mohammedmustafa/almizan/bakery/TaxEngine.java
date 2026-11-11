package com.mohammedmustafa.almizan.bakery;

import android.database.Cursor;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import java.math.BigInteger;

/** Effective-dated VAT baseline calculator. It never guesses unverified local tax rules. */
final class TaxEngine {
    private TaxEngine() {}

    static final class Result {
        final String status;
        final long netMinor;
        final long taxMinor;
        final long grossMinor;
        final int rateNumerator;
        final int rateDenominator;
        Result(String status,long net,long tax,long gross,int numerator,int denominator) {
            this.status=status;this.netMinor=net;this.taxMinor=tax;this.grossMinor=gross;
            this.rateNumerator=numerator;this.rateDenominator=denominator;
        }
    }

    static Result calculate(SQLiteDatabase db,String countryCode,String taxCode,String date,
                            long amountMinor,boolean registered,boolean taxInclusive) {
        if(amountMinor<0)throw new IllegalArgumentException("Amount cannot be negative");
        if(blank(countryCode)||blank(date)||blank(taxCode)||!GeneralLedger.validDate(date))throw new IllegalArgumentException("Missing or invalid tax jurisdiction input");
        String profileStatus="",taxType="";
        Cursor profile=db.rawQuery("SELECT profile_status,indirect_tax_type FROM tax_jurisdiction_profiles WHERE country_code=?",new String[]{countryCode});
        try {
            if(!profile.moveToFirst())return new Result("REVIEW_REQUIRED",amountMinor,0,amountMinor,0,1);
            profileStatus=profile.getString(0);taxType=profile.getString(1);
        } finally { profile.close(); }
        // A no-VAT outcome is allowed only for an explicitly confirmed profile.
        // Unverified jurisdictions must never bypass tax review by setting taxType=NO_VAT.
        if(isConfirmedNoVat(profileStatus,taxType))return new Result("NO_VAT_IN_JURISDICTION",amountMinor,0,amountMinor,0,1);
        if(!"VERIFIED_BASELINE".equals(profileStatus)||"SALES_TAX_LOCAL".equals(taxType)||"NO_VAT".equals(taxType))
            return new Result("LOCAL_RULES_REQUIRED",amountMinor,0,amountMinor,0,1);
        if(!registered)return new Result("REGISTRATION_STATUS_REQUIRED",amountMinor,0,amountMinor,0,1);
        int numerator=0,denominator=1;boolean found=false;
        Cursor rule=db.rawQuery("SELECT rate_num,rate_den FROM tax_rules WHERE country_code=? AND tax_code=? AND active=1 AND effective_from<=? AND (effective_to IS NULL OR effective_to='' OR effective_to>=?) ORDER BY effective_from DESC LIMIT 1",
            new String[]{countryCode,taxCode,date,date});
        try { if(rule.moveToFirst()){numerator=rule.getInt(0);denominator=rule.getInt(1);found=true;} }
        finally { rule.close(); }
        if(!found)return new Result("RULE_NOT_FOUND",amountMinor,0,amountMinor,0,1);
        if(numerator<0||denominator<=0)throw new IllegalStateException("Invalid persisted tax rate");
        if(taxInclusive) {
            long inclusiveDenominator=(long)denominator+(long)numerator;
            long net=roundedRatio(amountMinor,denominator,inclusiveDenominator);
            return new Result("CALCULATED",net,amountMinor-net,amountMinor,numerator,denominator);
        }
        long tax=roundedRatio(amountMinor,numerator,denominator);
        if(amountMinor>Long.MAX_VALUE-tax)throw new ArithmeticException("Tax-inclusive total exceeds supported amount");
        return new Result("CALCULATED",amountMinor,tax,amountMinor+tax,numerator,denominator);
    }

    static boolean isConfirmedNoVat(String profileStatus,String taxType) {
        return "NO_VAT_CONFIRMED".equals(profileStatus)&&"NO_VAT".equals(taxType);
    }

    private static boolean blank(String value){return value==null||value.trim().isEmpty();}

    static long roundedRatio(long amount,long numerator,long denominator) {
        if(amount<0||numerator<0||denominator<=0)throw new IllegalArgumentException("Invalid tax calculation operands");
        BigInteger product=BigInteger.valueOf(amount).multiply(BigInteger.valueOf(numerator));
        BigInteger divisor=BigInteger.valueOf(denominator);
        BigInteger rounded=product.add(divisor.shiftRight(1)).divide(divisor);
        return rounded.longValueExact();
    }
}
