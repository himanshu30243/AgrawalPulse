package com.agrawalpulse.matrimony.entity;

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

import java.time.Instant;
import java.util.UUID;

// One row per consent grant, per DPDP audit-trail requirement: who consented, when, and to
// what scope. A revoke does not delete the row (that would erase the audit trail) - it stamps
// revokedAt. A member is matrimony-visible only while at least one row has consentGiven=true
// and revokedAt IS NULL (see MatrimonyServiceImpl / the partial index in V1__init.sql).
@Entity
@Table(name = "matrimony_consents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatrimonyConsent extends BaseEntity {

    // Plain indexed UUID, no FK: family_members is owned by family-service, a separately
    // deployed service, and a cross-service FK would couple migration/deploy order between
    // the two (see docs/microservices-contract.md "Database" section). Existence of the
    // referenced family member is instead validated at write time via MatrimonyClient calling
    // family-service's REST API - never via a join or a local read of family_members.
    @Column(name = "chapter_id", nullable = false)
    private UUID chapterId;

    @Column(name = "family_member_id", nullable = false)
    private UUID familyMemberId;

    @Column(name = "consent_given", nullable = false)
    private boolean consentGiven;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_scope", nullable = false)
    private ConsentScope consentScope;

    @CreationTimestamp
    @Column(name = "consented_at", nullable = false, updatable = false)
    private Instant consentedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;
}
