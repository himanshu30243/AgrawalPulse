package com.agrawalpulse.membership.service;

import com.agrawalpulse.common.exception.ResourceNotFoundException;
import com.agrawalpulse.common.notification.NotificationPublisher;
import com.agrawalpulse.membership.client.FamilyClient;
import com.agrawalpulse.membership.dto.CreateMembershipRequest;
import com.agrawalpulse.membership.dto.MembershipDto;
import com.agrawalpulse.membership.dto.MembershipPaymentDto;
import com.agrawalpulse.membership.dto.RecordPaymentRequest;
import com.agrawalpulse.membership.entity.Membership;
import com.agrawalpulse.membership.entity.MembershipPayment;
import com.agrawalpulse.membership.entity.MembershipStatus;
import com.agrawalpulse.membership.repository.MembershipPaymentRepository;
import com.agrawalpulse.membership.repository.MembershipRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class MembershipServiceImpl implements MembershipService {

    private final MembershipRepository membershipRepository;
    private final MembershipPaymentRepository paymentRepository;
    private final FamilyClient familyClient;
    private final NotificationPublisher notificationPublisher;
    private final BigDecimal defaultAnnualFee;

    public MembershipServiceImpl(MembershipRepository membershipRepository,
                                  MembershipPaymentRepository paymentRepository,
                                  FamilyClient familyClient,
                                  NotificationPublisher notificationPublisher,
                                  @Value("${agrawalpulse.membership.default-annual-fee:250}") BigDecimal defaultAnnualFee) {
        this.membershipRepository = membershipRepository;
        this.paymentRepository = paymentRepository;
        this.familyClient = familyClient;
        this.notificationPublisher = notificationPublisher;
        this.defaultAnnualFee = defaultAnnualFee;
    }

    @Override
    public MembershipDto createMembership(UUID chapterId, CreateMembershipRequest request) {
        // Per docs/microservices-contract.md: a 404 from family-service means "reject the write
        // with 400 - family not found", same behavior as the old in-process check.
        if (!familyClient.familyExists(request.familyId())) {
            throw new IllegalArgumentException("Family not found: " + request.familyId());
        }
        membershipRepository.findByFamilyIdAndYear(request.familyId(), request.year())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "Membership already exists for family " + request.familyId() + " in year " + request.year());
                });

        // chapterId is the caller's own JWT-derived tenant (verified above against family-service),
        // never taken from the request body - denormalized here so every later read of this
        // membership is scoped by a direct column check, not a cross-service join.
        Membership membership = Membership.builder()
                .chapterId(chapterId)
                .familyId(request.familyId())
                .year(request.year())
                .feeAmount(defaultAnnualFee)
                .status(MembershipStatus.INACTIVE)
                .build();
        return toDto(membershipRepository.save(membership));
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipDto getMembership(UUID chapterId, UUID membershipId) {
        return toDto(findOwnedByChapter(chapterId, membershipId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MembershipDto> listMembershipsForChapter(UUID chapterId, int year) {
        return membershipRepository.findByChapterIdAndYear(chapterId, year).stream().map(this::toDto).toList();
    }

    @Override
    public MembershipPaymentDto recordPayment(UUID chapterId, UUID membershipId, RecordPaymentRequest request) {
        Membership membership = findOwnedByChapter(chapterId, membershipId);

        MembershipPayment payment = MembershipPayment.builder()
                .chapterId(chapterId)
                .membershipId(membershipId)
                .amount(request.amount())
                .paymentMethod(request.paymentMethod())
                .transactionRef(request.transactionRef())
                .build();
        payment = paymentRepository.save(payment);

        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setPaidAt(Instant.now());
        membershipRepository.save(membership);

        notificationPublisher.publish("membership.payment.recorded",
                "Membership %s paid %s via %s".formatted(membershipId, request.amount(), request.paymentMethod()));

        return toDto(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MembershipPaymentDto> listPayments(UUID chapterId, UUID membershipId) {
        findOwnedByChapter(chapterId, membershipId);
        return paymentRepository.findByMembershipIdAndChapterIdOrderByPaidAtDesc(membershipId, chapterId).stream()
                .map(this::toDto).toList();
    }

    // Primary tenant check is the membership's own chapter_id column - findByIdAndChapterId
    // returns empty for both "doesn't exist" and "exists in a different chapter", so callers get
    // the same 404 either way and cross-chapter probing can't distinguish the two.
    private Membership findOwnedByChapter(UUID chapterId, UUID membershipId) {
        return membershipRepository.findByIdAndChapterId(membershipId, chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found: " + membershipId));
    }

    private MembershipDto toDto(Membership membership) {
        return new MembershipDto(membership.getId(), membership.getFamilyId(), membership.getYear(),
                membership.getFeeAmount(), membership.getStatus(), membership.getPaidAt());
    }

    private MembershipPaymentDto toDto(MembershipPayment payment) {
        return new MembershipPaymentDto(payment.getId(), payment.getMembershipId(), payment.getAmount(),
                payment.getPaymentMethod(), payment.getTransactionRef(), payment.getPaidAt());
    }
}
