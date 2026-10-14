package com.mohammedmustafa.smartcompany;

import org.junit.Test;
import java.math.BigDecimal;

import static org.junit.Assert.*;

public class CurrencySpecTest {
    @Test public void mapsRepresentativeCurrencyPrecision() {
        assertEquals(2, CurrencySpec.of("SDG").getFractionDigits());
        assertEquals(2, CurrencySpec.of("EGP").getFractionDigits());
        assertEquals(3, CurrencySpec.of("KWD").getFractionDigits());
        assertEquals(3, CurrencySpec.of("JOD").getFractionDigits());
        assertEquals(3, CurrencySpec.of("TND").getFractionDigits());
        assertEquals(0, CurrencySpec.of("XAF").getFractionDigits());
        assertEquals(0, CurrencySpec.of("DJF").getFractionDigits());
        assertEquals(2, CurrencySpec.of("MRU").getFractionDigits());
        assertEquals(2, CurrencySpec.of("SOS").getFractionDigits());
    }

    @Test public void currencyCodeIsNormalized() {
        assertEquals("KWD", CurrencySpec.of(" kwd ").getCode());
    }

    @Test public void convertsThreeDecimalCurrencyWithoutLosingPrecision() {
        CurrencySpec kwd = CurrencySpec.of("KWD");
        assertEquals(12345L, kwd.toMinorUnits("12.345"));
        assertEquals(new BigDecimal("12.345"), kwd.fromMinorUnits(12345L));
        assertEquals(12345L, Money.netInvoiceMinorUnits("1", "12.345", "0", 3));
    }

    @Test public void zeroDecimalCurrencyDoesNotInventFractionalUnits() {
        CurrencySpec xaf = CurrencySpec.of("XAF");
        assertEquals(123L, xaf.toMinorUnits("123.4"));
        assertEquals(124L, xaf.toMinorUnits("123.5"));
        assertEquals(new BigDecimal("123"), xaf.fromMinorUnits(123L));
    }

    @Test public void unknownCurrencyIsRejectedInsteadOfDefaultingToTwoDecimals() {
        try {
            CurrencySpec.of("XXX");
            fail("Unknown currency must be explicitly configured");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Unsupported currency"));
        }
    }

    @Test public void invalidScaleIsRejected() {
        try {
            Money.toMinorUnits("1.00", 7);
            fail("Scale outside supported range must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("scale"));
        }
    }
}
