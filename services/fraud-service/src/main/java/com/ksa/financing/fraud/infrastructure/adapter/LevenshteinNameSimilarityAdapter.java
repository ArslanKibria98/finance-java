package com.ksa.financing.fraud.infrastructure.adapter;

import com.ksa.financing.fraud.domain.port.out.NameSimilarityPort;
import org.springframework.stereotype.Component;

@Component
public class LevenshteinNameSimilarityAdapter implements NameSimilarityPort {

    @Override
    public double similarity(String a, String b) {
        if (a == null || b == null) return 0.0;
        var s1 = a.trim().toLowerCase();
        var s2 = b.trim().toLowerCase();
        if (s1.isEmpty() && s2.isEmpty()) return 1.0;
        int distance = levenshtein(s1, s2);
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;
        return 1.0 - ((double) distance / maxLen);
    }

    @Override
    public boolean matches(String a, String b, double threshold) {
        return similarity(a, b) >= threshold;
    }

    private int levenshtein(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[m][n];
    }
}
