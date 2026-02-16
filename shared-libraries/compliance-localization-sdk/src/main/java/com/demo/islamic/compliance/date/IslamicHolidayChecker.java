package com.demo.islamic.compliance.date;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

/**
 * Checks for Islamic holidays and special dates
 * Used for business day calculations and scheduling
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IslamicHolidayChecker {

    private final HijriCalendar hijriCalendar;

    /**
     * Islamic holidays with fixed Hijri dates
     */
    private static final Map<String, HijriDateRange> FIXED_HOLIDAYS = new HashMap<>();

    static {
        // Ramadan - 9th month (entire month is significant)
        FIXED_HOLIDAYS.put("RAMADAN", new HijriDateRange(9, 1, 9, 30));

        // Eid al-Fitr - 1st to 3rd of Shawwal
        FIXED_HOLIDAYS.put("EID_AL_FITR", new HijriDateRange(10, 1, 10, 3));

        // Hajj Days - 8th to 13th of Dhu al-Hijjah
        FIXED_HOLIDAYS.put("HAJJ", new HijriDateRange(12, 8, 12, 13));

        // Day of Arafah - 9th of Dhu al-Hijjah
        FIXED_HOLIDAYS.put("DAY_OF_ARAFAH", new HijriDateRange(12, 9, 12, 9));

        // Eid al-Adha - 10th to 13th of Dhu al-Hijjah
        FIXED_HOLIDAYS.put("EID_AL_ADHA", new HijriDateRange(12, 10, 12, 13));

        // Islamic New Year - 1st of Muharram
        FIXED_HOLIDAYS.put("ISLAMIC_NEW_YEAR", new HijriDateRange(1, 1, 1, 1));

        // Ashura - 10th of Muharram
        FIXED_HOLIDAYS.put("ASHURA", new HijriDateRange(1, 10, 1, 10));

        // Mawlid al-Nabi - 12th of Rabi' al-Awwal
        FIXED_HOLIDAYS.put("MAWLID", new HijriDateRange(3, 12, 3, 12));

        // Isra and Mi'raj - 27th of Rajab
        FIXED_HOLIDAYS.put("ISRA_MIRAJ", new HijriDateRange(7, 27, 7, 27));

        // Laylat al-Qadr - Last 10 days of Ramadan (particularly odd nights)
        FIXED_HOLIDAYS.put("LAYLAT_AL_QADR", new HijriDateRange(9, 21, 9, 30));
    }

    /**
     * Check if a date is an Islamic holiday
     */
    public boolean isIslamicHoliday(LocalDate date) {
        HijriCalendar.HijriDate hijriDate = hijriCalendar.convertGregorianToHijri(date);

        for (Map.Entry<String, HijriDateRange> entry : FIXED_HOLIDAYS.entrySet()) {
            if (isDateInRange(hijriDate, entry.getValue())) {
                log.debug("Date {} is Islamic holiday: {}", date, entry.getKey());
                return true;
            }
        }

        return false;
    }

    /**
     * Get the Islamic holiday name for a date
     */
    public String getIslamicHolidayName(LocalDate date) {
        HijriCalendar.HijriDate hijriDate = hijriCalendar.convertGregorianToHijri(date);

        for (Map.Entry<String, HijriDateRange> entry : FIXED_HOLIDAYS.entrySet()) {
            if (isDateInRange(hijriDate, entry.getValue())) {
                return entry.getKey();
            }
        }

        return null;
    }

    /**
     * Check if a date is during Ramadan
     */
    public boolean isRamadan(LocalDate date) {
        HijriCalendar.HijriDate hijriDate = hijriCalendar.convertGregorianToHijri(date);
        return hijriDate.getMonth() == 9; // Ramadan is the 9th month
    }

    /**
     * Check if a date is Eid (al-Fitr or al-Adha)
     */
    public boolean isEid(LocalDate date) {
        String holiday = getIslamicHolidayName(date);
        return "EID_AL_FITR".equals(holiday) || "EID_AL_ADHA".equals(holiday);
    }

    /**
     * Get the start date of Ramadan for a given Gregorian year
     */
    public LocalDate getRamadanStartDate(int gregorianYear) {
        // Ramadan moves approximately 11 days earlier each Gregorian year
        // This is an approximation; actual dates should be confirmed
        // by moon sighting committees

        LocalDate searchStart = LocalDate.of(gregorianYear, 1, 1);
        LocalDate searchEnd = LocalDate.of(gregorianYear, 12, 31);

        while (searchStart.isBefore(searchEnd) || searchStart.equals(searchEnd)) {
            HijriCalendar.HijriDate hijriDate = hijriCalendar.convertGregorianToHijri(searchStart);
            if (hijriDate.getMonth() == 9 && hijriDate.getDay() == 1) {
                log.info("Ramadan {} starts on: {}", hijriDate.getYear(), searchStart);
                return searchStart;
            }
            searchStart = searchStart.plusDays(1);
        }

        return null; // Ramadan might not start in this Gregorian year
    }

    /**
     * Get Eid al-Fitr dates for a given Gregorian year
     */
    public List<LocalDate> getEidAlFitrDates(int gregorianYear) {
        List<LocalDate> eidDates = new ArrayList<>();

        LocalDate searchStart = LocalDate.of(gregorianYear, 1, 1);
        LocalDate searchEnd = LocalDate.of(gregorianYear, 12, 31);

        while (searchStart.isBefore(searchEnd) || searchStart.equals(searchEnd)) {
            HijriCalendar.HijriDate hijriDate = hijriCalendar.convertGregorianToHijri(searchStart);

            // Eid al-Fitr is 1st to 3rd of Shawwal
            if (hijriDate.getMonth() == 10 && hijriDate.getDay() >= 1 && hijriDate.getDay() <= 3) {
                eidDates.add(searchStart);
            }

            searchStart = searchStart.plusDays(1);
        }

        return eidDates;
    }

    /**
     * Get Eid al-Adha dates for a given Gregorian year
     */
    public List<LocalDate> getEidAlAdhaDates(int gregorianYear) {
        List<LocalDate> eidDates = new ArrayList<>();

        LocalDate searchStart = LocalDate.of(gregorianYear, 1, 1);
        LocalDate searchEnd = LocalDate.of(gregorianYear, 12, 31);

        while (searchStart.isBefore(searchEnd) || searchStart.equals(searchEnd)) {
            HijriCalendar.HijriDate hijriDate = hijriCalendar.convertGregorianToHijri(searchStart);

            // Eid al-Adha is 10th to 13th of Dhu al-Hijjah
            if (hijriDate.getMonth() == 12 && hijriDate.getDay() >= 10 && hijriDate.getDay() <= 13) {
                eidDates.add(searchStart);
            }

            searchStart = searchStart.plusDays(1);
        }

        return eidDates;
    }

    /**
     * Check if a date is a Friday (Jumu'ah - special day in Islam)
     */
    public boolean isFriday(LocalDate date) {
        return date.getDayOfWeek().getValue() == 5;
    }

    /**
     * Get all Islamic holidays for a Gregorian year
     */
    public Map<String, List<LocalDate>> getAllIslamicHolidays(int gregorianYear) {
        Map<String, List<LocalDate>> holidays = new HashMap<>();

        LocalDate searchStart = LocalDate.of(gregorianYear, 1, 1);
        LocalDate searchEnd = LocalDate.of(gregorianYear, 12, 31);

        while (searchStart.isBefore(searchEnd) || searchStart.equals(searchEnd)) {
            String holidayName = getIslamicHolidayName(searchStart);

            if (holidayName != null) {
                holidays.computeIfAbsent(holidayName, k -> new ArrayList<>()).add(searchStart);
            }

            searchStart = searchStart.plusDays(1);
        }

        return holidays;
    }

    /**
     * Get the next Islamic holiday from a given date
     */
    public IslamicHoliday getNextIslamicHoliday(LocalDate fromDate) {
        LocalDate searchDate = fromDate;
        int maxDaysToSearch = 400; // Search up to ~13 months ahead

        for (int i = 0; i < maxDaysToSearch; i++) {
            String holidayName = getIslamicHolidayName(searchDate);

            if (holidayName != null) {
                return new IslamicHoliday(holidayName, searchDate);
            }

            searchDate = searchDate.plusDays(1);
        }

        return null;
    }

    /**
     * Check if date is in a holiday period (for multi-day holidays)
     */
    private boolean isDateInRange(HijriCalendar.HijriDate date, HijriDateRange range) {
        // Handle ranges within the same month
        if (range.startMonth == range.endMonth) {
            return date.getMonth() == range.startMonth &&
                   date.getDay() >= range.startDay &&
                   date.getDay() <= range.endDay;
        }

        // Handle ranges across months
        if (date.getMonth() == range.startMonth && date.getDay() >= range.startDay) {
            return true;
        }

        if (date.getMonth() == range.endMonth && date.getDay() <= range.endDay) {
            return true;
        }

        // Check months in between
        return date.getMonth() > range.startMonth && date.getMonth() < range.endMonth;
    }

    /**
     * Get business days considering Islamic holidays
     */
    public int getBusinessDays(LocalDate startDate, LocalDate endDate,
                              boolean excludeFridays, boolean excludeSaturdays) {
        int businessDays = 0;
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            boolean isBusinessDay = true;

            // Check weekends
            if (excludeFridays && isFriday(currentDate)) {
                isBusinessDay = false;
            }
            if (excludeSaturdays && currentDate.getDayOfWeek().getValue() == 6) {
                isBusinessDay = false;
            }

            // Check Islamic holidays
            if (isIslamicHoliday(currentDate)) {
                isBusinessDay = false;
            }

            if (isBusinessDay) {
                businessDays++;
            }

            currentDate = currentDate.plusDays(1);
        }

        return businessDays;
    }

    /**
     * Represents a Hijri date range for holidays
     */
    private static class HijriDateRange {
        final int startMonth;
        final int startDay;
        final int endMonth;
        final int endDay;

        HijriDateRange(int startMonth, int startDay, int endMonth, int endDay) {
            this.startMonth = startMonth;
            this.startDay = startDay;
            this.endMonth = endMonth;
            this.endDay = endDay;
        }
    }

    /**
     * Represents an Islamic holiday
     */
    public static class IslamicHoliday {
        private final String name;
        private final LocalDate date;

        public IslamicHoliday(String name, LocalDate date) {
            this.name = name;
            this.date = date;
        }

        public String getName() { return name; }
        public LocalDate getDate() { return date; }
    }
}