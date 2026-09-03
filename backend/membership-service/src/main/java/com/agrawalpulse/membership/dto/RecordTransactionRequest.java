package com.agrawalpulse.membership.dto;

import com.agrawalpulse.membership.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

// The single admin write entry-point: find-or-create the FY's Membership row for familyId, then
// record this payment against it (see MembershipServiceImpl.recordTransaction). familyId is
// validated against family-service (not just "some UUID") before anything is written.
public record RecordTransactionRequest(
        @NotNull UUID familyId,
        @Min(2000) int financialYear,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotNull LocalDate paymentDate,
        @NotNull PaymentMethod paymentMode,
        String transactionRef,
        @Size(max = 500) String remarks
) {
}
