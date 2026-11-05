package com.mohammedmustafa.almizan.bakery;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class FactoryMath {
    private FactoryMath() {}
    public static long add(long a,long b){if(a<0||b<0)throw new IllegalArgumentException("negative value");return Math.addExact(a,b);}
    public static long multiply(long a,long b){if(a<0||b<0)throw new IllegalArgumentException("negative value");return Math.multiplyExact(a,b);}
    public static long prorate(long total,long part,long whole){
        if(total<0||part<0||whole<=0||part>whole)throw new IllegalArgumentException("invalid allocation");
        return BigDecimal.valueOf(total).multiply(BigDecimal.valueOf(part)).divide(BigDecimal.valueOf(whole),0,RoundingMode.HALF_UP).longValueExact();
    }
    public static long unitCostMilli(long totalCost,long quantityMilli){
        if(totalCost<0||quantityMilli<=0)throw new IllegalArgumentException("invalid cost or quantity");
        return BigDecimal.valueOf(totalCost).multiply(BigDecimal.valueOf(1000)).divide(BigDecimal.valueOf(quantityMilli),0,RoundingMode.HALF_UP).longValueExact();
    }
    public static long debt(long total,long cash,long bank){
        if(total<0||cash<0||bank<0||cash>total||bank>total-cash)throw new IllegalArgumentException("invalid settlement");
        return total-cash-bank;
    }
    public static long remainingStock(long stock,long used){if(stock<0||used<0||used>stock)throw new IllegalArgumentException("insufficient stock");return stock-used;}
}