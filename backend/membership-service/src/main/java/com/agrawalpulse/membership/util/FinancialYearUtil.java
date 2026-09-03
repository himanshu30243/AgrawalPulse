package com.agrawalpulse.membership.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

// India FY: 1-Apr to 31-Mar of the next calendar year, represented everywhere in this service by
// its start year (e.g. 2026 = "FY 2026-27"). memberships.year stores exactly this value - see
// V2__financial_year_and_editable_payments.sql's column comment. All FY math must go through here,
// never Year.now()/LocalDate.getYear() directly, so the Apr/Mar boundary is handled in one place.
public final class FinancialYearUtil {

    public static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");

    private FinancialYearUtil() {
    }

    public static int currentFinancialYear() {
        return financialYearOf(LocalDate.now(INDIA_ZONE));
    }

    public static int financialYearOf(LocalDate date) {
        return date.getMonthValue() >= 4 ? date.getYear() : date.getYear() - 1;
    }

    public static int financialYearOf(Instant instant) {
        return financialYearOf(LocalDate.ofInstant(instant, INDIA_ZONE));
    }

    // Display label, e.g. 2026 -> "2026-27". Used by DTOs/reports; exposed from the backend so the
    // frontend never has to reimplement this and risk drifting from it.
    public static String label(int financialYearStart) {
        return financialYearStart + "-" + String.valueOf(financialYearStart + 1).substring(2);
    }

    public static boolean isPreviousFinancialYear(int candidateYear, int currentYear) {
        return candidateYear == currentYear - 1;
    }
}
