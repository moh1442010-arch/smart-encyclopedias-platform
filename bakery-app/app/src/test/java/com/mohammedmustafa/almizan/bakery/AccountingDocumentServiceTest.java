package com.mohammedmustafa.almizan.bakery;

import org.json.JSONObject;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class AccountingDocumentServiceTest {
    @Test public void windowSalesMapCashBankAndReceivableToBalancedJournal() throws Exception {
        JSONObject payload=new JSONObject();
        payload.put("cash","1200");payload.put("debt","300");payload.put("bank","500");
        List<AccountingDocumentService.Line> lines=AccountingDocumentService.buildSalesLines("window",payload);
        assertEquals(4,lines.size());
        assertEquals("1000",lines.get(0).account);assertEquals(1200,lines.get(0).debit);
        assertEquals("1010",lines.get(1).account);assertEquals(500,lines.get(1).debit);
        assertEquals("1100",lines.get(2).account);assertEquals(300,lines.get(2).debit);
        assertEquals("4000",lines.get(3).account);assertEquals(2000,lines.get(3).credit);
        assertEquals(2000,lines.get(0).debit+lines.get(1).debit+lines.get(2).debit);
    }

    @Test public void distributionKeepsCustomerSubledgerReferences() throws Exception {
        JSONObject payload=new JSONObject();
        payload.put("rows","عميل أ | 10 | 100 | 50 | TX-1 | 70 | 0 | 41\nعميل ب | 5 | 80 | 0 |  | 30 | 0 | 42");
        List<AccountingDocumentService.Line> lines=AccountingDocumentService.buildSalesLines("distribution",payload);
        assertEquals(5,lines.size());
        assertEquals("1000",lines.get(0).account);assertEquals(180,lines.get(0).debit);
        assertEquals("1010",lines.get(1).account);assertEquals(50,lines.get(1).debit);
        assertEquals("1100",lines.get(2).account);assertEquals(70,lines.get(2).debit);assertEquals(41,lines.get(2).partyId);
        assertEquals("1100",lines.get(3).account);assertEquals(30,lines.get(3).debit);assertEquals(42,lines.get(3).partyId);
        assertEquals("4000",lines.get(4).account);assertEquals(330,lines.get(4).credit);
        long debit=0,credit=0;for(AccountingDocumentService.Line line:lines){debit+=line.debit;credit+=line.credit;}
        assertEquals(debit,credit);assertEquals(330,credit);
    }

    @Test public void distributionDebtWithoutCustomerIsRejected() throws Exception {
        JSONObject payload=new JSONObject();payload.put("rows","عميل | 1 | 0 | 0 |  | 90 | 0");
        try {AccountingDocumentService.buildSalesLines("distribution",payload);fail("Expected missing customer rejection");}
        catch(IllegalArgumentException expected){assertTrue(expected.getMessage().contains("عميلاً"));}
    }

    @Test public void negativeSalesAmountsAreRejected() throws Exception {
        JSONObject payload=new JSONObject();payload.put("cash","-1");payload.put("debt","0");payload.put("bank","0");
        try {AccountingDocumentService.buildSalesLines("window",payload);fail("Expected negative amount rejection");}
        catch(IllegalArgumentException expected){assertTrue(expected.getMessage().contains("سالب"));}
    }

    @Test public void laterDailySavePostsOnlyPositiveFinancialDelta() throws Exception {
        List<AccountingDocumentService.Line> oldLines=AccountingDocumentService.buildSalesLines("window",new JSONObject("{\"cash\":\"100\",\"debt\":\"20\",\"bank\":\"0\"}"));
        List<AccountingDocumentService.Line> newLines=AccountingDocumentService.buildSalesLines("window",new JSONObject("{\"cash\":\"150\",\"debt\":\"20\",\"bank\":\"0\"}"));
        List<AccountingDocumentService.Line> delta=AccountingDocumentService.deltaLines(oldLines,newLines);
        assertEquals(2,delta.size());
        assertEquals("1000",delta.get(0).account);assertEquals(50,delta.get(0).debit);
        assertEquals("4000",delta.get(1).account);assertEquals(50,delta.get(1).credit);
    }

    @Test public void dailyCorrectionDownwardPostsBalancedReclassification() throws Exception {
        List<AccountingDocumentService.Line> oldLines=AccountingDocumentService.buildSalesLines("window",new JSONObject("{\"cash\":\"150\",\"debt\":\"0\",\"bank\":\"0\"}"));
        List<AccountingDocumentService.Line> newLines=AccountingDocumentService.buildSalesLines("window",new JSONObject("{\"cash\":\"100\",\"debt\":\"0\",\"bank\":\"0\"}"));
        List<AccountingDocumentService.Line> delta=AccountingDocumentService.deltaLines(oldLines,newLines);
        assertEquals(2,delta.size());
        assertEquals("1000",delta.get(0).account);assertEquals(50,delta.get(0).credit);
        assertEquals("4000",delta.get(1).account);assertEquals(50,delta.get(1).debit);
        long debit=0,credit=0;for(AccountingDocumentService.Line line:delta){debit+=line.debit;credit+=line.credit;}
        assertEquals(debit,credit);
    }

    @Test public void sameFinancialSnapshotCreatesNoJournalDelta() throws Exception {
        List<AccountingDocumentService.Line> oldLines=AccountingDocumentService.buildSalesLines("window",new JSONObject("{\"cash\":\"100\",\"debt\":\"0\",\"bank\":\"0\"}"));
        List<AccountingDocumentService.Line> newLines=AccountingDocumentService.buildSalesLines("window",new JSONObject("{\"cash\":\"100\",\"debt\":\"0\",\"bank\":\"0\",\"notes\":\"updated\"}"));
        assertTrue(AccountingDocumentService.deltaLines(oldLines,newLines).isEmpty());
    }

    @Test public void distributionCustomerSubledgerChangeCreatesCustomerSpecificDelta() throws Exception {
        List<AccountingDocumentService.Line> oldLines=AccountingDocumentService.buildSalesLines("distribution",new JSONObject("{\"rows\":\"عميل أ | 1 | 0 | 0 |  | 100 | 0 | 41\"}"));
        List<AccountingDocumentService.Line> newLines=AccountingDocumentService.buildSalesLines("distribution",new JSONObject("{\"rows\":\"عميل ب | 1 | 0 | 0 |  | 100 | 0 | 42\"}"));
        List<AccountingDocumentService.Line> delta=AccountingDocumentService.deltaLines(oldLines,newLines);
        assertEquals(2,delta.size());
        assertEquals(41,delta.get(0).partyId);assertEquals(100,delta.get(0).credit);
        assertEquals(42,delta.get(1).partyId);assertEquals(100,delta.get(1).debit);
    }

    @Test public void windowRejectsBankTransferTotalMismatch() throws Exception {
        JSONObject payload=new JSONObject();
        payload.put("cash","0");payload.put("debt","0");payload.put("bank","100");
        payload.put("bank_transfers","90 | TX-90");
        try {AccountingDocumentService.buildSalesLines("window",payload);fail("Expected bank total mismatch");}
        catch(IllegalArgumentException expected){assertTrue(expected.getMessage().contains("لا يطابق"));}
    }

    @Test public void correctionDeltaOverflowIsRejected() {
        List<AccountingDocumentService.Line> oldLines=java.util.Arrays.asList(
            new AccountingDocumentService.Line("1000",Long.MAX_VALUE,0,0),
            new AccountingDocumentService.Line("4000",0,Long.MAX_VALUE,0));
        List<AccountingDocumentService.Line> newLines=java.util.Arrays.asList(
            new AccountingDocumentService.Line("1000",0,Long.MAX_VALUE,0),
            new AccountingDocumentService.Line("4000",Long.MAX_VALUE,0,0));
        try {AccountingDocumentService.deltaLines(oldLines,newLines);fail("Expected overflow rejection");}
        catch(ArithmeticException expected) { assertNotNull(expected); }
    }

    @Test public void windowRejectsLegacyBankAmountWithoutTransactionReference() throws Exception {
        JSONObject payload=new JSONObject();
        payload.put("cash","0");payload.put("debt","0");payload.put("bank","100");
        payload.put("bank_transfers","");payload.put("bank_ref","");
        try {AccountingDocumentService.buildSalesLines("window",payload);fail("Expected missing bank reference");}
        catch(IllegalArgumentException expected){assertTrue(expected.getMessage().contains("رقم عملية"));}
    }

    @Test public void distributionPriceDifferenceIsNotPostedToAnUnmappedAccount() throws Exception {
        JSONObject payload=new JSONObject();
        payload.put("rows","عميل | 1 | 100 | 0 |  | 0 | 25 | 41");
        try {AccountingDocumentService.buildSalesLines("distribution",payload);fail("Expected unmapped price difference rejection");}
        catch(IllegalArgumentException expected){assertTrue(expected.getMessage().contains("حساباً محاسبياً"));}
    }
}
