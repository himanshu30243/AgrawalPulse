package com.agrawalpulse.membership.repository;

import com.agrawalpulse.membership.entity.MembershipPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MembershipPaymentRepository extends JpaRepository<MembershipPayment, UUID> {

    // Scoped on the payment's own chapter_id column as the primary tenant boundary - membership
    // ownership is verified separately by the caller (see MembershipServiceImpl.findOwnedByChapter).
    List<MembershipPayment> findByMembershipIdAndChapterIdOrderByPaidAtDesc(UUID membershipId, UUID chapterId);
}
