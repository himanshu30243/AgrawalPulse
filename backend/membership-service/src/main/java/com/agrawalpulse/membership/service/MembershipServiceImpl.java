package com.agrawalpulse.membership.service;

import com.agrawalpulse.common.exception.ResourceNotFoundException;
import com.agrawalpulse.common.notification.NotificationPublisher;
import com.agrawalpulse.membership.client.BranchClient;
import com.agrawalpulse.membership.client.FamilyClient;
import com.agrawalpulse.membership.dto.BranchSummaryDto;
import com.agrawalpulse.membership.dto.CollectionSummaryDto;
import com.agrawalpulse.membership.dto.FamilyDto;
import com.agrawalpulse.membership.dto.MembershipReportRow;
import com.agrawalpulse.membership.dto.MembershipStatusDto;
import com.agrawalpulse.membership.dto.MembershipTransactionDto;
import com.agrawalpulse.membership.dto.RecordTransactionRequest;
import com.agrawalpulse.membership.dto.UpdateTransactionRequest;
import com.agrawalpulse.membership.entity.Membership;
import com.agrawalpulse.membership.entity.MembershipPayment;
import com.agrawalpulse.membership.entity.MembershipStatus;
import com.agrawalpulse.membership.repository.MembershipPaymentRepository;
import com.agrawalpulse.membership.repository.MembershipRepository;
import com.agrawalpulse.membership.util.FinancialYearUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class MembershipServiceImpl implements MembershipService {

    private final MembershipRepository membershipRepository;
    private final MembershipPaymentRepository paymentRepository;
    private final FamilyClient familyClient;
    private final BranchClient branchClient;
    private final NotificationPublisher notificationPublisher;

    public MembershipServiceImpl(MembershipRepository membershipRepository,
                                  MembershipPaymentRepository paymentRepository,
                                  FamilyClient familyClient,
                                  BranchClient branchClient,
                                  NotificationPublisher notificationPublisher) {
        this.membershipRepository = membershipRepository;
        this.paymentRepository = paymentRepository;
        this.familyClient = familyClient;
        this.branchClient = branchClient;
        this.notificationPublisher = notificationPublisher;
    }

    // Own-family and admin-tier visibility are both delegated to family-service (see
    // MembershipAccessScope's javadoc): CHAPTER_ADMIN/STATE_ADMIN/NATIONAL_ADMIN/ADMIN hold the
    // matching VIEW_x_FAMILIES permission at the same tier as their VIEW_x_MEMBERSHIP one (V3 and
    // V6 grant them in parallel), so family-service's own scope check already answers "can this
    // caller see this family" correctly for every tier, not just USER's own-family case. A 404 from
    // family-service is mapped to this service's own 404 - never 403, so a caller can't distinguish
    // "doesn't exist" from "exists but isn't visible to me".
    @Override
    @Transactional(readOnly = true)
    public MembershipStatusDto getStatus(MembershipAccessScope scope, UUID familyId) {
        requireFamily(familyId);
        List<Membership> memberships = membershipRepository.findByFamilyIdOrderByYearDesc(familyId);
        return computeStatus(familyId, memberships, FinancialYearUtil.currentFinancialYear());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MembershipTransactionDto> getTransactionHistory(MembershipAccessScope scope, UUID familyId) {
        requireFamily(familyId);
        List<Membership> memberships = membershipRepository.findByFamilyIdOrderByYearDesc(familyId);
        if (memberships.isEmpty()) {
            return List.of();
        }
        Map<UUID, Integer> yearByMembershipId = memberships.stream()
                .collect(Collectors.toMap(Membership::getId, Membership::getYear));
        List<UUID> membershipIds = memberships.stream().map(Membership::getId).toList();
        // A family never changes chapters (no chapter-transfer feature exists), so every FY row it
        // has shares the same chapter_id - safe to read off any one of them.
        UUID chapterId = memberships.get(0).getChapterId();
        return paymentRepository.findByMembershipIdInAndChapterIdOrderByPaymentDateDesc(membershipIds, chapterId)
                .stream()
                .map(p -> toDto(p, familyId, yearByMembershipId.get(p.getMembershipId())))
                .toList();
    }

    // Find-or-create the FY's Membership row, scoped by the family's own chapterId (resolved fresh
    // from family-service, never taken from the request body). A second transaction for a family+FY
    // that already has one is rejected - corrections go through updateTransaction instead, so
    // recorded revenue is never silently double-counted.
    @Override
    public MembershipTransactionDto recordTransaction(MembershipAccessScope scope, RecordTransactionRequest request) {
        FamilyDto family = familyClient.getFamily(request.familyId())
                .orElseThrow(() -> new IllegalArgumentException("Family not found: " + request.familyId()));

        membershipRepository.findByChapterIdAndFamilyIdAndYear(family.chapterId(), request.familyId(), request.financialYear())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("A transaction already exists for family " + request.familyId()
                            + " in FY " + FinancialYearUtil.label(request.financialYear())
                            + " - edit it instead of recording a new one");
                });

        Membership membership = Membership.builder()
                .chapterId(family.chapterId())
                .familyId(request.familyId())
                .year(request.financialYear())
                .feeAmount(request.amount())
                .status(MembershipStatus.ACTIVE)
                .paidAt(toInstant(request.paymentDate()))
                .build();
        membership = membershipRepository.save(membership);

        MembershipPayment payment = MembershipPayment.builder()
                .chapterId(family.chapterId())
                .membershipId(membership.getId())
                .amount(request.amount())
                .paymentDate(request.paymentDate())
                .paymentMethod(request.paymentMode())
                .transactionRef(request.transactionRef())
                .remarks(request.remarks())
                .createdBy(scope.userId())
                .build();
        // saveAndFlush, not save: createdAt/updatedAt are Hibernate-generated (@CreationTimestamp/
        // @UpdateTimestamp) and are only populated on the in-memory entity during flush, which a
        // plain save() defers to transaction commit - toDto() below would otherwise serialize them
        // as still-null in the response even though the DB row itself is correct.
        payment = paymentRepository.saveAndFlush(payment);

        notificationPublisher.publish("membership.transaction.recorded",
                "Family %s paid %s for FY %s via %s".formatted(request.familyId(), request.amount(),
                        FinancialYearUtil.label(request.financialYear()), request.paymentMode()));

        return toDto(payment, request.familyId(), request.financialYear());
    }

    // Directly-editable payments (MVP decision - see plan): re-stamps the parent Membership's
    // paidAt/feeAmount from the corrected values too, since that row is what computeStatus reads.
    @Override
    public MembershipTransactionDto updateTransaction(MembershipAccessScope scope, UUID transactionId,
                                                        UpdateTransactionRequest request) {
        MembershipPayment payment = findAuthorizedPayment(scope, transactionId);
        payment.setAmount(request.amount());
        payment.setPaymentDate(request.paymentDate());
        payment.setPaymentMethod(request.paymentMode());
        payment.setTransactionRef(request.transactionRef());
        payment.setRemarks(request.remarks());
        payment.setUpdatedBy(scope.userId());
        // saveAndFlush - see recordTransaction's comment on why a plain save() would serialize a
        // stale (pre-flush) updatedAt in the response.
        payment = paymentRepository.saveAndFlush(payment);

        Membership membership = membershipRepository.findById(payment.getMembershipId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));
        membership.setFeeAmount(request.amount());
        membership.setPaidAt(toInstant(request.paymentDate()));
        membershipRepository.save(membership);

        return toDto(payment, membership.getFamilyId(), membership.getYear());
    }

    // Family universe resolved via family-service (same delegation as getStatus/getTransactionHistory
    // - see their comment), which already narrows to just the caller's own family for a plain USER,
    // exactly like FamilyController.listFamilies does for family records themselves.
    @Override
    @Transactional(readOnly = true)
    public List<MembershipStatusDto> listMembers(MembershipAccessScope scope, int financialYear,
                                                  MembershipStatus statusFilter) {
        return familyClient.searchFamilies(null, null, null).stream()
                .map(f -> computeStatus(f.id(), membershipRepository.findByFamilyIdOrderByYearDesc(f.id()), financialYear))
                .filter(s -> statusFilter == null || s.status() == statusFilter)
                .toList();
    }

    // Backend-composed: family-service resolves which families match the search filters (and are
    // visible to this caller at all), this service joins in status/last-payment data. "Pending" means
    // not fully paid for financialYear - families ACTIVE for that FY are excluded from the report.
    @Override
    @Transactional(readOnly = true)
    public List<MembershipReportRow> pendingPaymentReport(MembershipAccessScope scope, int financialYear,
                                                            String familyId, String headOfFamilyName,
                                                            String mobileNumber, String areaLocality) {
        List<FamilyDto> families = familyClient.searchFamilies(headOfFamilyName, mobileNumber, areaLocality);
        Map<UUID, BranchSummaryDto> branchesByChapterId = branchesByChapterId();

        return families.stream()
                .filter(f -> familyId == null || familyId.isBlank()
                        || f.familyCode().toLowerCase().contains(familyId.toLowerCase()))
                .map(f -> {
                    MembershipStatusDto status = computeStatus(f.id(),
                            membershipRepository.findByFamilyIdOrderByYearDesc(f.id()), financialYear);
                    BranchSummaryDto branch = branchesByChapterId.get(f.chapterId());
                    return new MembershipReportRow(f.id(), f.familyCode(), f.headOfFamilyName(), f.mobileNumber(),
                            f.areaLocality(), f.chapterId(), branch != null ? branch.name() : null,
                            status.status(), status.lastPaidFinancialYear(), status.lastPaymentDate());
                })
                .filter(row -> row.status() != MembershipStatus.ACTIVE)
                .toList();
    }

    // Always scoped to the caller's own chapter (CollectionSummaryDto is a single-chapter row) -
    // the "Membership Collection Summary" is a per-chapter admin dashboard widget, not a
    // multi-chapter rollup, regardless of how broad the caller's own read tier is.
    @Override
    @Transactional(readOnly = true)
    public CollectionSummaryDto collectionSummary(MembershipAccessScope scope, int financialYear) {
        UUID chapterId = scope.chapterId();
        List<FamilyDto> chapterFamilies = familyClient.searchFamilies(null, null, null).stream()
                .filter(f -> chapterId.equals(f.chapterId()))
                .toList();

        int active = (int) membershipRepository.countByChapterIdAndYearAndStatus(chapterId, financialYear, MembershipStatus.ACTIVE);
        int pending = 0;
        int expired = 0;
        List<UUID> membershipIdsForYear = new ArrayList<>();
        for (FamilyDto family : chapterFamilies) {
            List<Membership> memberships = membershipRepository.findByFamilyIdOrderByYearDesc(family.id());
            MembershipStatusDto status = computeStatus(family.id(), memberships, financialYear);
            if (status.status() == MembershipStatus.PENDING_RENEWAL) {
                pending++;
            } else if (status.status() == MembershipStatus.EXPIRED) {
                expired++;
            }
            memberships.stream().filter(m -> m.getYear() == financialYear).map(Membership::getId)
                    .forEach(membershipIdsForYear::add);
        }

        BigDecimal totalCollected = membershipIdsForYear.isEmpty() ? BigDecimal.ZERO
                : paymentRepository.findByMembershipIdIn(membershipIdsForYear).stream()
                        .map(MembershipPayment::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        String chapterName = branchClient.getBranch(chapterId).map(BranchSummaryDto::name).orElse(null);

        return new CollectionSummaryDto(chapterId, chapterName, financialYear, totalCollected, active, pending, expired);
    }

    // The grace-period rule (highest-risk logic in this feature): paid for targetFy -> ACTIVE; not
    // paid but paid the immediately preceding FY -> PENDING_RENEWAL; anything else (2+ FYs unpaid,
    // or never had a paid row at all) -> EXPIRED. EXPIRED is never a stored row status - it only
    // exists as this computed roll-up (see MembershipStatus). lastPaid is capped at targetFy so a
    // report for a past FY never leaks a later payment as if it were already known at that point.
    private MembershipStatusDto computeStatus(UUID familyId, List<Membership> familyMemberships, int targetFy) {
        Map<Integer, Membership> byYear = familyMemberships.stream()
                .collect(Collectors.toMap(Membership::getYear, m -> m, (a, b) -> a));
        Membership targetRow = byYear.get(targetFy);
        boolean targetPaid = targetRow != null && targetRow.getStatus() == MembershipStatus.ACTIVE;
        Optional<Membership> lastPaid = familyMemberships.stream()
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE && m.getYear() <= targetFy)
                .max(Comparator.comparingInt(Membership::getYear));

        MembershipStatus status;
        if (targetPaid) {
            status = MembershipStatus.ACTIVE;
        } else if (lastPaid.isPresent() && FinancialYearUtil.isPreviousFinancialYear(lastPaid.get().getYear(), targetFy)) {
            status = MembershipStatus.PENDING_RENEWAL;
        } else {
            status = MembershipStatus.EXPIRED;
        }

        return new MembershipStatusDto(familyId, status, targetFy, targetPaid,
                lastPaid.map(m -> LocalDate.ofInstant(m.getPaidAt(), FinancialYearUtil.INDIA_ZONE)).orElse(null),
                lastPaid.map(Membership::getYear).orElse(null));
    }

    // Same broadest-wins tiers as FamilyAccessScope's own isInScope, applied to an already-resolved
    // Membership row - used only by findAuthorizedPayment, where the lookup starts from a bare
    // transactionId (no familyId in the URL to delegate to family-service directly).
    private boolean isInScope(MembershipAccessScope scope, Membership membership) {
        if (scope.viewAll()) {
            return true;
        }
        if (scope.viewState()) {
            return resolveChapterIdsInCallerState(scope.chapterId()).contains(membership.getChapterId());
        }
        if (scope.viewChapter()) {
            return membership.getChapterId().equals(scope.chapterId());
        }
        return familyClient.getFamily(membership.getFamilyId()).isPresent();
    }

    private List<UUID> resolveChapterIdsInCallerState(UUID callerChapterId) {
        List<BranchSummaryDto> allChapters = branchClient.listAll();
        String callerState = allChapters.stream()
                .filter(c -> c.id().equals(callerChapterId))
                .map(BranchSummaryDto::state)
                .findFirst()
                .orElse(null);
        if (callerState == null) {
            return List.of(callerChapterId);
        }
        return allChapters.stream()
                .filter(c -> callerState.equalsIgnoreCase(c.state()))
                .map(BranchSummaryDto::id)
                .toList();
    }

    private FamilyDto requireFamily(UUID familyId) {
        return familyClient.getFamily(familyId)
                .orElseThrow(() -> new ResourceNotFoundException("Family not found: " + familyId));
    }

    // 404 (never 403) both when the id doesn't exist and when it's out of the caller's scope - see
    // FamilyServiceImpl.findAuthorized for the same convention.
    private MembershipPayment findAuthorizedPayment(MembershipAccessScope scope, UUID transactionId) {
        MembershipPayment payment = paymentRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));
        Membership membership = membershipRepository.findById(payment.getMembershipId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));
        if (!isInScope(scope, membership)) {
            throw new ResourceNotFoundException("Transaction not found: " + transactionId);
        }
        return payment;
    }

    private Map<UUID, BranchSummaryDto> branchesByChapterId() {
        Map<UUID, BranchSummaryDto> branches = new HashMap<>();
        for (BranchSummaryDto branch : branchClient.listAll()) {
            branches.put(branch.id(), branch);
        }
        return branches;
    }

    private Instant toInstant(LocalDate date) {
        return date.atStartOfDay(FinancialYearUtil.INDIA_ZONE).toInstant();
    }

    private MembershipTransactionDto toDto(MembershipPayment payment, UUID familyId, int financialYear) {
        return new MembershipTransactionDto(payment.getId(), familyId, financialYear, payment.getAmount(),
                payment.getPaymentDate(), payment.getPaymentMethod(), payment.getTransactionRef(),
                payment.getRemarks(), payment.getCreatedBy(), payment.getCreatedAt(), payment.getUpdatedBy(),
                payment.getUpdatedAt());
    }
}
