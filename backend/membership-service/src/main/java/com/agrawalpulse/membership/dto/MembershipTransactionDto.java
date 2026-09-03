package com.agrawalpulse.membership.dto;

import com.agrawalpulse.membership.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// One membership_payments row, API-facing. Carries every field requirement #4 lists (Family ID,
// Financial Year, Amount, Payment Date, Payment Mode, Transaction Reference, Created By, Created
// Date, Remarks) plus the update audit trail for the directly-editable-payments decision.
public record MembershipTransactionDto(
        UUID id,
        UUID familyId,
        int financialYear,
        BigDecimal amount,
        LocalDate paymentDate,
        PaymentMethod paymentMode,
        String transactionRef,
        String remarks,
        UUID createdBy,
        Instant createdAt,
        UUID updatedBy,
        Instant updatedAt
) {
}
