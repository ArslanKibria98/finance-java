package com.ksa.financing.lending.application.util;

import java.util.Map;

/**
 * Static map of Saudi bank Arabic names by SAMA bank code or English name.
 * Used to enrich bank account responses with nameAr.
 */
public final class BankNameAr {

    private BankNameAr() {}

    private static final Map<String, String> BY_CODE = Map.ofEntries(
            Map.entry("80", "بنك الراجحي"),
            Map.entry("10", "البنك الأهلي السعودي"),
            Map.entry("15", "بنك البلاد"),
            Map.entry("05", "بنك الإنماء"),
            Map.entry("30", "البنك العربي"),
            Map.entry("60", "بنك الجزيرة"),
            Map.entry("76", "بنك مسقط"),
            Map.entry("55", "البنك السعودي الفرنسي"),
            Map.entry("95", "البنك الإماراتي"),
            Map.entry("90", "بنك الخليج"),
            Map.entry("40", "البنك السعودي البريطاني"),
            Map.entry("45", "بنك الرياض"),
            Map.entry("50", "البنك السعودي للاستثمار")
    );

    private static final Map<String, String> BY_NAME = Map.ofEntries(
            Map.entry("Al Rajhi Bank", "بنك الراجحي"),
            Map.entry("National Commercial Bank", "البنك الأهلي السعودي"),
            Map.entry("Saudi National Bank", "البنك الأهلي السعودي"),
            Map.entry("Al Bilad Bank", "بنك البلاد"),
            Map.entry("Al Inma Bank", "بنك الإنماء"),
            Map.entry("Arab National Bank", "البنك العربي"),
            Map.entry("Bank Al Jazira", "بنك الجزيرة"),
            Map.entry("Bank Muscat", "بنك مسقط"),
            Map.entry("Banque Saudi Fransi", "البنك السعودي الفرنسي"),
            Map.entry("Emirates Bank", "البنك الإماراتي"),
            Map.entry("Gulf International Bank", "بنك الخليج"),
            Map.entry("Saudi British Bank", "البنك السعودي البريطاني"),
            Map.entry("Riyad Bank", "بنك الرياض"),
            Map.entry("Saudi Investment Bank", "البنك السعودي للاستثمار")
    );

    public static String lookup(String bankCode, String bankName) {
        if (bankCode != null) {
            String byCode = BY_CODE.get(bankCode);
            if (byCode != null) return byCode;
        }
        if (bankName != null) {
            String byName = BY_NAME.get(bankName);
            if (byName != null) return byName;
        }
        return null;
    }
}
