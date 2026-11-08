package com.mohammedmustafa.almizan.bakery;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class FactoryMath {
    private FactoryMath() {}
    public static long add(long a,long b){if(a<0||b<0)throw new IllegalArgumentException("negative value");if(Long.MAX_VALUE-a<b)throw new ArithmeticException("long overflow");return a+b;}
    public static long multiply(long a,long b){if(a<0||b<0)throw new IllegalArgumentException("negative value");if(a!=0&&b>Long.MAX_VALUE/a)throw new ArithmeticException("long overflow");return a*b;}
    public static long prorate(long total,long part,long whole){
        if(total<0||part<0||whole<=0||part>whole)throw new IllegalArgumentException("invalid allocation");
        return BigDecimal.valueOf(total).multiply(BigDecimal.valueOf(part)).divide(BigDecimal.valueOf(whole),0,RoundingMode.HALF_UP).longValueExact();
    }
    public static long unitCostMilli(long totalCost,long quantityMilli){
        if(totalCost<0||quantityMilli<=0)throw new IllegalArgumentException("invalid cost or quantity");
        return BigDecimal.valueOf(totalCost).multiply(BigDecimal.valueOf(1000000)).divide(BigDecimal.valueOf(quantityMilli),0,RoundingMode.HALF_UP).longValueExact();
    }
    public static long costForStockIssue(long stockValue,long issueQty,long stockQty){
        if(stockValue<0||issueQty<0||stockQty<=0||issueQty>stockQty)throw new IllegalArgumentException("invalid stock valuation");
        return prorate(stockValue,issueQty,stockQty);
    }
    public static long weightedAverageUnitCost(long oldQty,long oldValue,long addedQty,long addedValue){
        if(oldQty<0||oldValue<0||addedQty<0||addedValue<0)throw new IllegalArgumentException("negative inventory value");
        long qty=add(oldQty,addedQty),value=add(oldValue,addedValue);
        if(qty==0)return 0;
        return unitCostMilli(value,qty);
    }
    public static void validateBankakReference(long amount,String reference){
        if(amount<0||(amount>0&&(reference==null||reference.trim().isEmpty())))throw new IllegalArgumentException("invalid Bankak reference");
    }
    public static long debt(long total,long cash,long bank){
        if(total<0||cash<0||bank<0||cash>total||bank>total-cash)throw new IllegalArgumentException("invalid settlement");
        return total-cash-bank;
    }
    public static long returnableQuantity(long sold,long alreadyReturned,long requested){
        if(sold<=0||alreadyReturned<0||requested<=0||alreadyReturned>sold||requested>sold-alreadyReturned)
            throw new IllegalArgumentException("invalid sales return quantity");
        return sold-alreadyReturned-requested;
    }
    public static long proportionalReturnValue(long originalValue,long returnedQty,long soldQty){
        if(originalValue<0||returnedQty<=0||soldQty<=0||returnedQty>soldQty)
            throw new IllegalArgumentException("invalid return valuation");
        return prorate(originalValue,returnedQty,soldQty);
    }
    public static void validateReturnSettlement(long saleCash,long saleBank,long saleDebt,long previousCash,long previousBank,long previousCredit,long returnCash,long returnBank,long returnCredit){
        long[] values={saleCash,saleBank,saleDebt,previousCash,previousBank,previousCredit,returnCash,returnBank,returnCredit};
        for(long value:values)if(value<0)throw new IllegalArgumentException("negative return settlement");
        if(previousCash>saleCash||previousBank>saleBank||previousCredit>saleDebt
            ||returnCash>saleCash-previousCash||returnBank>saleBank-previousBank||returnCredit>saleDebt-previousCredit)
            throw new IllegalArgumentException("return settlement exceeds original invoice balances");
    }
    public static long incrementalWipMaterialCost(long totalMaterialCost,long cumulativeOutput,long previousOutput,long plannedOutput){
        if(totalMaterialCost<0||previousOutput<0||cumulativeOutput<=previousOutput||plannedOutput<=0||cumulativeOutput>plannedOutput)throw new IllegalArgumentException("invalid staged WIP allocation");
        return prorate(totalMaterialCost,cumulativeOutput,plannedOutput)-prorate(totalMaterialCost,previousOutput,plannedOutput);
    }
    public static long workInProgressCost(long materialCost,long labor,long overhead){
        return add(add(materialCost,labor),overhead);
    }
    public static long workInProgressUnitCost(long totalCost,long outputQtyMilli){
        return unitCostMilli(totalCost,outputQtyMilli);
    }
    public static boolean journalIsBalanced(long debit,long credit){
        if(debit<0||credit<0)throw new IllegalArgumentException("negative journal total");
        return debit==credit;
    }
    public static long saleTotal(long quantityMilli,long unitPrice){
        if(quantityMilli<=0||unitPrice<=0)throw new IllegalArgumentException("invalid sale quantity or unit price");
        return BigDecimal.valueOf(quantityMilli).multiply(BigDecimal.valueOf(unitPrice))
            .divide(BigDecimal.valueOf(1000),0,RoundingMode.HALF_UP).longValueExact();
    }
    public static long remainingStock(long stock,long used){if(stock<0||used<0||used>stock)throw new IllegalArgumentException("insufficient stock");return stock-used;}
}