package com.agrawalpulse.membership.entity;

import com.agrawalpulse.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "membership_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipPayment extends BaseEntity {

    // Same reasoning as Membership.chapterId: chapters live in user-service, so no cross-service FK.
    @Column(name = "chapter_id", nullable = false)
    private UUID chapterId;

    // Real FK in the migration (membership_payments.membership_id -> memberships.id): both
    // tables are owned by this same service, so a DB-enforced FK is safe here.
    @Column(name = "membership_id", nullable = false)
    private UUID membershipId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "transaction_ref")
    private String transactionRef;

    @CreationTimestamp
    @Column(name = "paid_at", nullable = false, updatable = false)
    private Instant paidAt;
}
