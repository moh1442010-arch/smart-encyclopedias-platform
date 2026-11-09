package com.mohammedmustafa.almizan.bakery;

import org.junit.Test;
import static org.junit.Assert.*;

public class TaxJurisdictionMappingTest {
    @Test public void supportedSingleIssuerCurrenciesMapToTheirJurisdiction() {
        assertEquals("SD",TaxJurisdictionProfiles.countriesForCurrency("SDG").get(0));
        assertEquals("SA",TaxJurisdictionProfiles.countriesForCurrency("SAR").get(0));
        assertEquals("AE",TaxJurisdictionProfiles.countriesForCurrency("AED").get(0));
        assertEquals("EG",TaxJurisdictionProfiles.countriesForCurrency("EGP").get(0));
        assertEquals("QA",TaxJurisdictionProfiles.countriesForCurrency("QAR").get(0));
        assertEquals("KW",TaxJurisdictionProfiles.countriesForCurrency("KWD").get(0));
    }

    @Test public void allArabLeagueNationalCurrenciesHaveCountryMappings() {
        String[] currencies={"DZD","BHD","KMF","DJF","EGP","IQD","JOD","KWD","LBP","LYD","MRU","MAD","OMR","ILS","QAR","SAR","SOS","SDG","SYP","TND","AED","YER"};
        for(String currency:currencies)assertFalse(currency,TaxJurisdictionProfiles.countriesForCurrency(currency).isEmpty());
    }

    @Test public void sharedUsDollarRequiresCountrySelection() {
        java.util.List<String> countries=TaxJurisdictionProfiles.countriesForCurrency("USD");
        assertTrue(countries.contains("US"));
        assertTrue(countries.contains("LB"));
        assertTrue(countries.contains("PS"));
        assertEquals(3,countries.size());
    }

    @Test public void unknownCurrencyNeverGetsAnInventedCountry() {
        assertTrue(TaxJurisdictionProfiles.countriesForCurrency("XXX").isEmpty());
    }
}
