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
}
