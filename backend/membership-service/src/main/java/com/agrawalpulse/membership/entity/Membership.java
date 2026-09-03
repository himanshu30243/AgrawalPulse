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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "memberships")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// One row per family per financial year (see V1's uq_memberships_family_year constraint) - a
// family accrues a new row each FY, not a single mutable "current membership" record. `status`
// here is row-scoped to just this FY (ACTIVE = this FY's fee paid, PENDING_RENEWAL = row exists,
// not yet paid) - it never holds EXPIRED. EXPIRED is a family-level roll-up across every FY row a
// family has, computed at read time by MembershipServiceImpl (see its computeStatus), not stored
// on any single row.
public class Membership extends BaseEntity {

    // Plain indexed UUID, no REFERENCES chapters(id)/families(id): chapter-service and
    // family-service are independently deployed, and a cross-service FK would couple their
    // migration/deploy order to this one (see docs/microservices-contract.md). Existence is
    // instead verified at write time via FamilyClient's REST call to family-service.
    @Column(name = "chapter_id", nullable = false)
    private UUID chapterId;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    // Financial year start year (India, Apr-Mar), not a calendar year - see FinancialYearUtil and
    // this column's own DB comment (V2__financial_year_and_editable_payments.sql). Always compute/
    // compare via FinancialYearUtil, never java.time.Year.now() directly.
    @Column(nullable = false)
    private int year;

    @Column(name = "fee_amount", nullable = false)
    @Builder.Default
    private BigDecimal feeAmount = BigDecimal.valueOf(250);

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MembershipStatus status = MembershipStatus.PENDING_RENEWAL;

    @Column(name = "paid_at")
    private Instant paidAt;
}
