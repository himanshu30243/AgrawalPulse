package com.agrawalpulse.membership.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialYearUtilTest {

    @Test
    void financialYearOf_aprilStartsTheNewFinancialYear() {
        assertThat(FinancialYearUtil.financialYearOf(LocalDate.of(2026, 4, 1))).isEqualTo(2026);
    }

    @Test
    void financialYearOf_marchIsStillThePreviousFinancialYear() {
        assertThat(FinancialYearUtil.financialYearOf(LocalDate.of(2026, 3, 31))).isEqualTo(2025);
    }

    @Test
    void financialYearOf_decemberFallsInTheYearItStartedIn() {
        assertThat(FinancialYearUtil.financialYearOf(LocalDate.of(2026, 12, 15))).isEqualTo(2026);
    }

    @Test
    void label_formatsAsTwoDigitEndYear() {
        assertThat(FinancialYearUtil.label(2026)).isEqualTo("2026-27");
    }

    @Test
    void label_rollsOverAtTheCentury() {
        assertThat(FinancialYearUtil.label(2099)).isEqualTo("2099-00");
    }

    @Test
    void isPreviousFinancialYear_trueOnlyForExactlyOneYearBack() {
        assertThat(FinancialYearUtil.isPreviousFinancialYear(2025, 2026)).isTrue();
        assertThat(FinancialYearUtil.isPreviousFinancialYear(2024, 2026)).isFalse();
        assertThat(FinancialYearUtil.isPreviousFinancialYear(2026, 2026)).isFalse();
    }
}
