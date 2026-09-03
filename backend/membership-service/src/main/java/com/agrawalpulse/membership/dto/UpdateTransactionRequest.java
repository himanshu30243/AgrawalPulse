package com.agrawalpulse.membership.dto;

import com.agrawalpulse.membership.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

// Directly-editable payments (MVP decision - see plan). familyId/financialYear are not editable:
// correcting which family/FY a transaction belongs to means recording a new one, not moving this
// one - only the payment's own details can be corrected.
public record UpdateTransactionRequest(
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotNull LocalDate paymentDate,
        @NotNull PaymentMethod paymentMode,
        String transactionRef,
        @Size(max = 500) String remarks
) {
}
