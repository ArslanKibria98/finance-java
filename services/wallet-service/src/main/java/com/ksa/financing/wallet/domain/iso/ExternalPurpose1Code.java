package com.ksa.financing.wallet.domain.iso;

/**
 * ISO 20022 ExternalPurpose1Code — purpose of a payment.
 * <p>
 * SAMA mandates a purpose code for transfers ≥ 5,000 SAR.
 * Includes Sharia-specific codes (ZAKT, SADQ, HAJJ).
 * <p>
 * Spec: <a href="https://www.iso20022.org/external_code_list.page">ISO 20022 ExternalPurposeCode</a>
 */
public enum ExternalPurpose1Code {
    SALA("Salary",            "Salary or pension payment"),
    BONU("Bonus",             "Bonus payment"),
    PENS("Pension",           "Pension payment"),
    RENT("Rent",              "Rental property payment"),
    UTIL("Utility",           "Utility bill (water/electricity/etc)"),
    ELEC("Electricity",       "Electricity bill"),
    GASB("Gas Bill",          "Gas bill"),
    WTER("Water",             "Water bill"),
    PHON("Telephone",         "Phone / telecom bill"),
    GOVT("Government",        "Government / ministry payment"),
    TAXS("Tax",               "Tax payment"),
    GDDS("Goods",             "Purchase of goods"),
    SUPP("Supplier",          "Supplier payment"),
    TRAD("Trade",             "Trade settlement"),
    SCVE("Services",          "Purchase of services"),
    EDUC("Education",         "Tuition / school fees"),
    HLTI("Healthcare",        "Hospital / clinic payment"),
    INSU("Insurance",         "Insurance premium"),
    LOAN("Loan",              "Loan repayment"),
    LOAR("Loan Refund",       "Loan refund"),
    INTC("Intra-Company",     "Intra-company transfer"),
    FAMI("Family",             "Family maintenance / allowance"),
    GIFT("Gift",              "Personal gift"),
    PROP("Property",          "Property purchase"),
    CASH("Cash",              "General cash transfer"),
    INVE("Investment",        "Investment / securities"),
    ZAKT("Zakat",             "Zakat payment (Sharia mandatory alms)"),
    SADQ("Sadaqa",            "Sadaqa / voluntary charity (Sharia)"),
    CHAR("Charity",           "Charitable donation"),
    HAJJ("Hajj/Umrah",        "Hajj or Umrah related payment"),
    OTHR("Other",             "Other / not classified");

    private final String shortName;
    private final String description;

    ExternalPurpose1Code(String shortName, String description) {
        this.shortName = shortName;
        this.description = description;
    }

    public String shortName()    { return shortName; }
    public String description()  { return description; }

    public static ExternalPurpose1Code parse(String input) {
        if (input == null || input.isBlank()) return null;
        try {
            return ExternalPurpose1Code.valueOf(input.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
