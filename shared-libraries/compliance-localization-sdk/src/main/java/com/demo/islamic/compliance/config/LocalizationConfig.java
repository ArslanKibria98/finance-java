package com.demo.islamic.compliance.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Localization configuration for multi-language support
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "compliance.localization")
public class LocalizationConfig {

    /**
     * Default locale for the application
     */
    private String defaultLocale = "ar-SA";

    /**
     * List of supported locales
     */
    private List<String> supportedLocales = List.of("ar-SA", "en-US");

    /**
     * Currency configuration
     */
    private CurrencyConfig currency = new CurrencyConfig();

    /**
     * Date and time formatting
     */
    private DateTimeConfig dateTime = new DateTimeConfig();

    /**
     * Number formatting
     */
    private NumberConfig number = new NumberConfig();

    /**
     * Translation overrides
     */
    private Map<String, Map<String, String>> translations;

    @Data
    public static class CurrencyConfig {
        /**
         * Default currency code
         */
        private String defaultCode = "SAR";

        /**
         * Currency symbol
         */
        private String symbol = "ر.س";

        /**
         * Currency symbol position (before/after)
         */
        private String symbolPosition = "after";

        /**
         * Decimal places for currency
         */
        private int decimalPlaces = 2;

        /**
         * Thousands separator
         */
        private String thousandsSeparator = ",";

        /**
         * Decimal separator
         */
        private String decimalSeparator = ".";

        /**
         * Display format pattern
         */
        private String displayFormat = "#,##0.00";
    }

    @Data
    public static class DateTimeConfig {
        /**
         * Date format pattern
         */
        private String dateFormat = "dd/MM/yyyy";

        /**
         * Time format pattern
         */
        private String timeFormat = "HH:mm:ss";

        /**
         * DateTime format pattern
         */
        private String dateTimeFormat = "dd/MM/yyyy HH:mm:ss";

        /**
         * Enable Hijri calendar display
         */
        private boolean hijriEnabled = true;

        /**
         * Hijri date format
         */
        private String hijriFormat = "dd/MM/yyyy";

        /**
         * Show both calendars
         */
        private boolean showBothCalendars = true;

        /**
         * First day of week (1=Sunday, 7=Saturday)
         */
        private int firstDayOfWeek = 1;

        /**
         * Weekend days
         */
        private List<Integer> weekendDays = List.of(5, 6); // Friday, Saturday
    }

    @Data
    public static class NumberConfig {
        /**
         * Number grouping enabled
         */
        private boolean groupingEnabled = true;

        /**
         * Grouping size
         */
        private int groupingSize = 3;

        /**
         * Grouping separator
         */
        private String groupingSeparator = ",";

        /**
         * Decimal separator
         */
        private String decimalSeparator = ".";

        /**
         * Use Eastern Arabic numerals (٠١٢٣٤٥٦٧٨٩)
         */
        private boolean useEasternArabicNumerals = false;

        /**
         * Percentage format
         */
        private String percentageFormat = "#,##0.00%";

        /**
         * Default decimal places
         */
        private int defaultDecimalPlaces = 2;
    }

    /**
     * Create locale resolver bean
     */
    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver localeResolver = new SessionLocaleResolver();
        localeResolver.setDefaultLocale(parseLocale(defaultLocale));
        return localeResolver;
    }

    /**
     * Create message source for internationalization
     */
    @Bean
    public ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("messages/messages", "messages/validation");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setUseCodeAsDefaultMessage(true);
        return messageSource;
    }

    /**
     * Parse locale string to Locale object
     */
    private Locale parseLocale(String localeString) {
        if (localeString == null || localeString.isEmpty()) {
            return Locale.getDefault();
        }

        String[] parts = localeString.split("-");
        if (parts.length == 1) {
            return new Locale(parts[0]);
        } else if (parts.length == 2) {
            return new Locale(parts[0], parts[1]);
        } else {
            return new Locale(parts[0], parts[1], parts[2]);
        }
    }

    /**
     * Get locale for language code
     */
    public Locale getLocale(String languageCode) {
        return parseLocale(languageCode);
    }

    /**
     * Check if locale is supported
     */
    public boolean isLocaleSupported(String locale) {
        return supportedLocales != null && supportedLocales.contains(locale);
    }

    /**
     * Get translation for key and locale
     */
    public String getTranslation(String locale, String key) {
        if (translations == null) {
            return null;
        }

        Map<String, String> localeTranslations = translations.get(locale);
        if (localeTranslations == null) {
            return null;
        }

        return localeTranslations.get(key);
    }

    /**
     * Format currency amount based on locale
     */
    public String formatCurrency(Number amount, String locale) {
        if (amount == null) {
            return "";
        }

        String formatted = String.format(
            java.util.Locale.forLanguageTag(locale),
            currency.getDisplayFormat(),
            amount
        );

        if ("after".equalsIgnoreCase(currency.getSymbolPosition())) {
            return formatted + " " + currency.getSymbol();
        } else {
            return currency.getSymbol() + " " + formatted;
        }
    }

    /**
     * Convert number to Eastern Arabic numerals if enabled
     */
    public String formatNumber(String number) {
        if (!number.useEasternArabicNumerals) {
            return number;
        }

        return number
            .replace('0', '٠')
            .replace('1', '١')
            .replace('2', '٢')
            .replace('3', '٣')
            .replace('4', '٤')
            .replace('5', '٥')
            .replace('6', '٦')
            .replace('7', '٧')
            .replace('8', '٨')
            .replace('9', '٩');
    }
}