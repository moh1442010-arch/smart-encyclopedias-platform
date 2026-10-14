package com.mohammedmustafa.smartcompany;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Explicit currency precision registry for the first multi-currency tranche.
 * Keep currency precision data separate from accounting calculations.
 * Unknown currencies are rejected rather than silently assuming two decimals.
 */
public final class CurrencySpec {
    private static final Map<String, Integer> SCALE_BY_CODE;

    static {
        Map<String, Integer> values = new HashMap<>();
        // Three-decimal currencies represented in the initial product scope.
        for (String code : new String[]{"BHD", "IQD", "JOD", "KWD", "LYD", "OMR", "TND"}) {
            values.put(code, 3);
        }
        // Zero-decimal CFA franc (Central African currency zone).
        values.put("XAF", 0);
        values.put("DJF", 0);
        // Two-decimal currencies relevant to the initial regional scope.
        for (String code : new String[]{
                "AED", "DZD", "EGP", "EUR", "ILS", "LBP", "MAD", "MRU", "SOS",
                "QAR", "SAR", "SDG", "SSP", "SYP", "USD", "YER"
        }) {
            values.put(code, 2);
        }
        SCALE_BY_CODE = Collections.unmodifiableMap(values);
    }

    private final String code;
    private final int fractionDigits;

    private CurrencySpec(String code, int fractionDigits) {
        this.code = code;
        this.fractionDigits = fractionDigits;
    }

    public static CurrencySpec of(String currencyCode) {
        if (currencyCode == null || currencyCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency code is required");
        }
        String normalized = currencyCode.trim().toUpperCase(Locale.ROOT);
        Integer scale = SCALE_BY_CODE.get(normalized);
        if (scale == null) {
            throw new IllegalArgumentException("Unsupported currency code: " + normalized);
        }
        return new CurrencySpec(normalized, scale);
    }

    public String getCode() {
        return code;
    }

    public int getFractionDigits() {
        return fractionDigits;
    }

    public long toMinorUnits(String amount) {
        return Money.toMinorUnits(amount, fractionDigits);
    }

    public java.math.BigDecimal fromMinorUnits(long amount) {
        return Money.fromMinorUnits(amount, fractionDigits);
    }
}
