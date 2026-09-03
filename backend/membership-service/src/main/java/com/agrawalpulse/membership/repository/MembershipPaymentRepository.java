package com.agrawalpulse.membership.repository;

import com.agrawalpulse.membership.entity.MembershipPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MembershipPaymentRepository extends JpaRepository<MembershipPayment, UUID> {

    // A family's full transaction history across every FY row it has (membershipIds resolved from
    // MembershipRepository.findByFamilyIdOrderByYearDesc first) - the actual requirement #1
    // "transaction history" query, not scoped to one FY at a time.
    List<MembershipPayment> findByMembershipIdInAndChapterIdOrderByPaymentDateDesc(
            Collection<UUID> membershipIds, UUID chapterId);

    // Sum of payments for the collection-summary report - amounts for a given chapter+FY, resolved
    // via the membershipIds already scoped to that chapter+year by the caller.
    List<MembershipPayment> findByMembershipIdIn(Collection<UUID> membershipIds);

    // Editing a transaction uses the inherited findById(UUID) - deliberately no chapterId-scoped
    // variant (unlike the old findByIdAndChapterId this replaces), since state/all-tier admins have
    // no single chapterId to filter by. Scope is instead enforced by
    // MembershipServiceImpl.findAuthorizedPayment resolving the parent Membership and running it
    // through the normal MembershipAccessScope check, same as family-service's findAuthorized.
}
