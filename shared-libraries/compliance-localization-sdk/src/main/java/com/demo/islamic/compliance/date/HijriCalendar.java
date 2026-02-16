package com.demo.islamic.compliance.date;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.chrono.HijrahChronology;
import java.time.chrono.HijrahDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DecimalStyle;
import java.time.temporal.ChronoField;
import java.util.Locale;

/**
 * Hijri Calendar utility for Islamic date conversions
 * Uses Java's built-in HijrahChronology for accurate date calculations
 */
@Slf4j
@Component
public class HijriCalendar {

    private static final Locale ARABIC_LOCALE = new Locale("ar", "SA");
    private static final HijrahChronology HIJRI_CHRONO = HijrahChronology.INSTANCE;

    // Arabic month names
    private static final String[] ARABIC_MONTHS = {
        "محرم",           // Muharram
        "صفر",            // Safar
        "ربيع الأول",      // Rabi' al-Awwal
        "ربيع الآخر",      // Rabi' al-Thani
        "جمادى الأولى",    // Jumada al-Awwal
        "جمادى الآخرة",    // Jumada al-Thani
        "رجب",            // Rajab
        "شعبان",          // Sha'ban
        "رمضان",          // Ramadan
        "شوال",           // Shawwal
        "ذو القعدة",      // Dhu al-Qi'dah
        "ذو الحجة"        // Dhu al-Hijjah
    };

    // English transliteration of month names
    private static final String[] ENGLISH_MONTHS = {
        "Muharram",
        "Safar",
        "Rabi' al-Awwal",
        "Rabi' al-Thani",
        "Jumada al-Awwal",
        "Jumada al-Thani",
        "Rajab",
        "Sha'ban",
        "Ramadan",
        "Shawwal",
        "Dhu al-Qi'dah",
        "Dhu al-Hijjah"
    };

    /**
     * Get current Hijri date
     */
    public HijriDate getCurrentHijriDate() {
        HijrahDate hijrahDate = HijrahDate.now();
        return convertToHijriDate(hijrahDate);
    }

    /**
     * Convert Gregorian date to Hijri date
     */
    public HijriDate convertGregorianToHijri(LocalDate gregorianDate) {
        if (gregorianDate == null) {
            return null;
        }

        HijrahDate hijrahDate = HijrahDate.from(gregorianDate);
        HijriDate hijriDate = convertToHijriDate(hijrahDate);

        log.debug("Converted Gregorian {} to Hijri {}-{}-{}",
            gregorianDate, hijriDate.getYear(), hijriDate.getMonth(), hijriDate.getDay());

        return hijriDate;
    }

    /**
     * Convert Hijri date to Gregorian date
     */
    public LocalDate convertHijriToGregorian(HijriDate hijriDate) {
        if (hijriDate == null) {
            return null;
        }

        HijrahDate hijrahDate = HijrahDate.of(
            hijriDate.getYear(),
            hijriDate.getMonth(),
            hijriDate.getDay()
        );

        LocalDate gregorianDate = LocalDate.from(hijrahDate);

        log.debug("Converted Hijri {}-{}-{} to Gregorian {}",
            hijriDate.getYear(), hijriDate.getMonth(), hijriDate.getDay(),
            gregorianDate);

        return gregorianDate;
    }

    /**
     * Format Hijri date in Arabic
     * Example output: "15 رمضان 1445"
     */
    public String formatArabic(HijriDate hijriDate) {
        if (hijriDate == null) {
            return "";
        }

        String monthName = ARABIC_MONTHS[hijriDate.getMonth() - 1];
        return String.format("%d %s %d",
            hijriDate.getDay(), monthName, hijriDate.getYear());
    }

    /**
     * Format Hijri date in English
     * Example output: "15 Ramadan 1445"
     */
    public String formatEnglish(HijriDate hijriDate) {
        if (hijriDate == null) {
            return "";
        }

        String monthName = ENGLISH_MONTHS[hijriDate.getMonth() - 1];
        return String.format("%d %s %d",
            hijriDate.getDay(), monthName, hijriDate.getYear());
    }

    /**
     * Format Hijri date in standard format
     * Example output: "1445-09-15"
     */
    public String formatStandard(HijriDate hijriDate) {
        if (hijriDate == null) {
            return "";
        }

        return String.format("%04d-%02d-%02d",
            hijriDate.getYear(), hijriDate.getMonth(), hijriDate.getDay());
    }

    /**
     * Parse Hijri date from string
     * Accepts format: "yyyy-MM-dd" (e.g., "1445-09-15")
     */
    public HijriDate parseHijriDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }

        try {
            String[] parts = dateString.split("-");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid date format: " + dateString);
            }

            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);

            HijrahDate hijrahDate = HijrahDate.of(year, month, day);
            return convertToHijriDate(hijrahDate);
        } catch (Exception e) {
            log.error("Failed to parse Hijri date: {}", dateString, e);
            throw new IllegalArgumentException("Invalid Hijri date: " + dateString, e);
        }
    }

    /**
     * Get the month name in Arabic
     */
    public String getMonthNameArabic(int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Invalid month: " + month);
        }
        return ARABIC_MONTHS[month - 1];
    }

    /**
     * Get the month name in English
     */
    public String getMonthNameEnglish(int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Invalid month: " + month);
        }
        return ENGLISH_MONTHS[month - 1];
    }

    /**
     * Check if a given year is a Hijri leap year
     */
    public boolean isLeapYear(int hijriYear) {
        HijrahDate date = HijrahDate.of(hijriYear, 1, 1);
        return date.isLeapYear();
    }

    /**
     * Get the number of days in a Hijri month
     */
    public int getDaysInMonth(int hijriYear, int hijriMonth) {
        HijrahDate date = HijrahDate.of(hijriYear, hijriMonth, 1);
        return date.lengthOfMonth();
    }

    /**
     * Get the number of days in a Hijri year
     */
    public int getDaysInYear(int hijriYear) {
        HijrahDate date = HijrahDate.of(hijriYear, 1, 1);
        return date.lengthOfYear();
    }

    /**
     * Calculate the difference in days between two Hijri dates
     */
    public long getDaysBetween(HijriDate from, HijriDate to) {
        LocalDate fromGregorian = convertHijriToGregorian(from);
        LocalDate toGregorian = convertHijriToGregorian(to);

        return toGregorian.toEpochDay() - fromGregorian.toEpochDay();
    }

    /**
     * Add days to a Hijri date
     */
    public HijriDate addDays(HijriDate date, int days) {
        LocalDate gregorian = convertHijriToGregorian(date);
        LocalDate newGregorian = gregorian.plusDays(days);
        return convertGregorianToHijri(newGregorian);
    }

    /**
     * Add months to a Hijri date
     */
    public HijriDate addMonths(HijriDate date, int months) {
        HijrahDate hijrahDate = HijrahDate.of(
            date.getYear(), date.getMonth(), date.getDay()
        );
        HijrahDate newHijrahDate = hijrahDate.plus(months, java.time.temporal.ChronoUnit.MONTHS);
        return convertToHijriDate(newHijrahDate);
    }

    /**
     * Add years to a Hijri date
     */
    public HijriDate addYears(HijriDate date, int years) {
        HijrahDate hijrahDate = HijrahDate.of(
            date.getYear(), date.getMonth(), date.getDay()
        );
        HijrahDate newHijrahDate = hijrahDate.plus(years, java.time.temporal.ChronoUnit.YEARS);
        return convertToHijriDate(newHijrahDate);
    }

    /**
     * Get the day of week for a Hijri date
     * Returns: 1 (Sunday) to 7 (Saturday)
     */
    public int getDayOfWeek(HijriDate date) {
        LocalDate gregorian = convertHijriToGregorian(date);
        return gregorian.getDayOfWeek().getValue() % 7 + 1;
    }

    /**
     * Get the day of week name in Arabic
     */
    public String getDayOfWeekArabic(HijriDate date) {
        String[] arabicDays = {
            "الأحد",    // Sunday
            "الإثنين",   // Monday
            "الثلاثاء",  // Tuesday
            "الأربعاء",  // Wednesday
            "الخميس",   // Thursday
            "الجمعة",   // Friday
            "السبت"     // Saturday
        };

        int dayOfWeek = getDayOfWeek(date);
        return arabicDays[dayOfWeek - 1];
    }

    /**
     * Convert HijrahDate to custom HijriDate
     */
    private HijriDate convertToHijriDate(HijrahDate hijrahDate) {
        return new HijriDate(
            hijrahDate.get(ChronoField.YEAR),
            hijrahDate.get(ChronoField.MONTH_OF_YEAR),
            hijrahDate.get(ChronoField.DAY_OF_MONTH)
        );
    }

    /**
     * Custom Hijri date representation
     */
    public static class HijriDate {
        private final int year;
        private final int month;
        private final int day;

        public HijriDate(int year, int month, int day) {
            this.year = year;
            this.month = month;
            this.day = day;
        }

        public int getYear() { return year; }
        public int getMonth() { return month; }
        public int getDay() { return day; }

        @Override
        public String toString() {
            return String.format("%04d-%02d-%02d", year, month, day);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof HijriDate)) return false;
            HijriDate other = (HijriDate) obj;
            return year == other.year && month == other.month && day == other.day;
        }

        @Override
        public int hashCode() {
            return year * 10000 + month * 100 + day;
        }
    }
}