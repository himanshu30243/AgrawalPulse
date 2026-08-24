package com.agrawalpulse.membership.dto;

import com.agrawalpulse.membership.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MembershipPaymentDto(
        UUID id,
        UUID membershipId,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        String transactionRef,
        Instant paidAt
) {
}
