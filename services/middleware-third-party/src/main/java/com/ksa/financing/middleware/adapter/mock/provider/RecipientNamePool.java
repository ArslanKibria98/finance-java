package com.ksa.financing.middleware.adapter.mock.provider;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Pool of single English Muslim names used by the Nafath mock to fill the
 * {@code englishFirstName} and {@code englishThirdName} slots in the
 * demographic payload.
 *
 * The mock picks one entry per slot per onboarding session (cached by transId
 * — see {@link NafathMockProvider}). The resulting full English name flows
 * downstream to customer-service → Keycloak → wallet-service identically, so
 * every API that surfaces a customer name (recipient lookup, transfer
 * history, login token response) reports the same value.
 */
public final class RecipientNamePool {

    private RecipientNamePool() {}

    /** Single English Muslim first names (used for englishFirstName slot). */
    public static final List<String> FIRST_NAMES = List.of(
            "Omar", "Ahmed", "Ali", "Khalid", "Yousef", "Saud", "Ibrahim",
            "Hassan", "Hussain", "Faisal", "Tariq", "Adnan", "Bilal", "Fahad",
            "Hamza", "Idris", "Jamal", "Karim", "Mansoor", "Nasser", "Rashid",
            "Saleh", "Tamer", "Usama", "Waleed", "Yasir", "Zaid", "Abdullah",
            "Mohammed", "Sami", "Rayan", "Anas", "Bader", "Majed", "Mazen",
            "Naif", "Salim", "Talal", "Walid", "Yazeed", "Ziyad", "Sultan",
            "Rakan", "Mishal", "Turki", "Bandar", "Nawaf", "Saif", "Hamad");

    /** 100 single English Muslim names suitable for use as a surname. */
    public static final List<String> NAMES = List.of(
            "Arshad", "Baig", "Shah", "Ehsaan", "Kibriya", "Asim", "Luqman",
            "Khan", "Ahmed", "Ali", "Hussain", "Rahman", "Karim", "Akbar",
            "Hassan", "Ibrahim", "Mahmood", "Siddiqui", "Tariq", "Akhtar",
            "Younis", "Iqbal", "Nawaz", "Farooq", "Anwar", "Sultan", "Pasha",
            "Mehmood", "Raza", "Aslam", "Hashmi", "Mansoor", "Naseer", "Akram",
            "Mehdi", "Sohail", "Kaneria", "Butt", "Alam", "Mustafa", "Haq",
            "Jamshed", "Kamran", "Lateef", "Misbah", "Owais", "Pervez", "Qadir",
            "Sharif", "Sarfaraz", "Tahir", "Akmal", "Waqar", "Yasin", "Zaheer",
            "Adnan", "Bashir", "Chand", "Daud", "Eshaan", "Furqan", "Ghazi",
            "Hadi", "Irfan", "Jameel", "Kabir", "Liaqat", "Mehboob", "Nashit",
            "Qadri", "Musharraf", "Qaiser", "Rashid", "Saqlain", "Tanveer",
            "Khawaja", "Bari", "Wahab", "Yawar", "Zulfiqar", "Saud", "Otaibi",
            "Harbi", "Ghamdi", "Qahtani", "Dossari", "Shehri", "Mutairi",
            "Anazi", "Zahrani", "Maliki", "Subaie", "Sahli", "Rashidi", "Balawi",
            "Juhani", "Asmari", "Shamrani", "Hazmi", "Qarni", "Faraj");

    public static String randomName() {
        return NAMES.get(ThreadLocalRandom.current().nextInt(NAMES.size()));
    }

    public static String randomFirstName() {
        return FIRST_NAMES.get(ThreadLocalRandom.current().nextInt(FIRST_NAMES.size()));
    }

    /** Kept for backwards compatibility with earlier callers; equivalent to {@link #randomName()}. */
    public static String[] splitFirstLast(String fullName) {
        if (fullName == null || fullName.isBlank()) return new String[]{"", ""};
        String[] parts = fullName.trim().split("\\s+", 2);
        return parts.length == 1 ? new String[]{parts[0], ""} : new String[]{parts[0], parts[1]};
    }
}
