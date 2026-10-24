package com.mohammedmustafa.almizan.bakery;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Exact integer arithmetic for bakery production and daily reconciliation. */
public final class BakeryMath {
    private BakeryMath() {}
    /** Sum manually entered work lines in the format: work number | loaf count. */
    public static long sumWorkRows(String rows) {
        if (rows == null || rows.trim().isEmpty()) return 0L;
        long total=0L;
        String[] lines=rows.split("\\r?\\n");
        java.util.HashSet<Long> workNumbers=new java.util.HashSet<>();
        for(int i=0;i<lines.length;i++) {
            String line=lines[i].trim();
            if(line.isEmpty()) continue;
            String[] parts=line.split("\\|",-1);
            if(parts.length!=2) throw new IllegalArgumentException("سطر القطعيات "+(i+1)+": اكتب رقم الشغل | عدد العيش.");
            long work, loaves;
            try {
                work=Long.parseLong(normalizeDigits(parts[0].trim()));
                loaves=Long.parseLong(normalizeDigits(parts[1].trim()));
            } catch(NumberFormatException ex) {
                throw new IllegalArgumentException("سطر القطعيات "+(i+1)+": أدخل أرقامًا صحيحة لرقم الشغل وعدد العيش.");
            }
            if(work<=0 || loaves<0) throw new IllegalArgumentException("رقم الشغل يبدأ من 1 وعدد العيش لا يكون سالبًا.");
            if(!workNumbers.add(work)) throw new IllegalArgumentException("رقم الشغل "+work+" مكرر.");
            total=Math.addExact(total,loaves);
        }
        return total;
    }
    private static String normalizeDigits(String value) {
        StringBuilder out=new StringBuilder();
        for(int i=0;i<value.length();i++) {
            char c=value.charAt(i);
            if(c>='٠' && c<='٩') out.append((char)('0'+c-'٠'));
            else if(c>='۰' && c<='۹') out.append((char)('0'+c-'۰'));
            else out.append(c);
        }
        return out.toString();
    }
    public static long productionLoaves(long workUnits, long loavesPerUnit) {
        if (workUnits < 0 || loavesPerUnit < 0) throw new IllegalArgumentException("Negative quantity");
        return Math.multiplyExact(workUnits, loavesPerUnit);
    }
    public static long valueForLoaves(long loaves, long loavesPerThousand) {
        if (loaves < 0) throw new IllegalArgumentException("Negative loaf count");
        if (loavesPerThousand <= 0) throw new IllegalArgumentException("Loaves per 1,000 SDG must be positive");
        return BigDecimal.valueOf(loaves).multiply(BigDecimal.valueOf(1000))
                .divide(BigDecimal.valueOf(loavesPerThousand), 0, RoundingMode.HALF_UP).longValueExact();
    }
    public static long sum(long... values) {
        long result=0; for(long value:values) result=Math.addExact(result,value); return result;
    }
    public static long accountedTotal(long cash, long debt, long bank, long priceDifference, long wages, long expenses, long spoilageValue) {
        return sum(cash, debt, bank, priceDifference, wages, expenses, spoilageValue);
    }
    public static long difference(long expectedProduction, long recordedCategories) {
        return Math.subtractExact(expectedProduction, recordedCategories);
    }
    /** Daily ingredient/fuel cost. Quantities and unit prices must be non-negative. */
    public static long operatingCost(long fuelQty,long fuelPrice,long flourBags,long flourBagPrice,
            long yeastCount,long yeastPrice,long oilQty,long oilPoundPrice,
            long saltQty,long saltPrice,long bagsTotalPrice) {
        long[] values={fuelQty,fuelPrice,flourBags,flourBagPrice,yeastCount,yeastPrice,
                oilQty,oilPoundPrice,saltQty,saltPrice,bagsTotalPrice};
        for(long value:values) if(value<0) throw new IllegalArgumentException("Negative cost input");
        return sum(Math.multiplyExact(fuelQty,fuelPrice),Math.multiplyExact(flourBags,flourBagPrice),
                Math.multiplyExact(yeastCount,yeastPrice),Math.multiplyExact(oilQty,oilPoundPrice),
                Math.multiplyExact(saltQty,saltPrice),bagsTotalPrice);
    }
    /** Reconciles stock after a change to the daily number of flour bags used. */
    public static long stockAfterUsage(long currentStock, long previouslyAppliedUsage, long newUsage) {
        if (currentStock < 0 || previouslyAppliedUsage < 0 || newUsage < 0)
            throw new IllegalArgumentException("Negative stock or usage");
        return Math.subtractExact(currentStock, Math.subtractExact(newUsage, previouslyAppliedUsage));
    }

    /** Net daily margin after spoilage, wages, shop expenses, and operating costs. */
    public static long netMargin(long productionValue,long spoilageValue,long wages,long expenses,long operatingCost) {
        long[] values={productionValue,spoilageValue,wages,expenses,operatingCost};
        for(long value:values) if(value<0) throw new IllegalArgumentException("Negative amount");
        return Math.subtractExact(Math.subtractExact(Math.subtractExact(Math.subtractExact(productionValue,spoilageValue),wages),expenses),operatingCost);
    }

    /** Estimated margin before any additional costs not included in the supplied cost list. */
    public static long grossMargin(long productionValue,long operatingCost) {
        if(productionValue<0 || operatingCost<0) throw new IllegalArgumentException("Negative amount");
        return Math.subtractExact(productionValue,operatingCost);
    }
}
