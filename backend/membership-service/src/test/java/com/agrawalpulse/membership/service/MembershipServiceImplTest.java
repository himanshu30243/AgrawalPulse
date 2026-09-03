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
import com.agrawalpulse.membership.entity.PaymentMethod;
import com.agrawalpulse.membership.repository.MembershipPaymentRepository;
import com.agrawalpulse.membership.repository.MembershipRepository;
import com.agrawalpulse.membership.util.FinancialYearUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Dummy-data unit tests for MembershipServiceImpl - pure Mockito, no Spring context / DB / Docker
// required (mirrors FamilyServiceImplTest's structure exactly). CURRENT_FY is read from
// FinancialYearUtil itself rather than hardcoded, so these tests stay correct on any run date.
@ExtendWith(MockitoExtension.class)
class MembershipServiceImplTest {

    private static final int CURRENT_FY = FinancialYearUtil.currentFinancialYear();
    private static final UUID CHAPTER_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID FAMILY_ID = UUID.randomUUID();

    private static MembershipAccessScope chapterScope() {
        return new MembershipAccessScope(CHAPTER_ID, USER_ID, false, false, true);
    }

    private static MembershipAccessScope ownScope() {
        return new MembershipAccessScope(CHAPTER_ID, USER_ID, false, false, false);
    }

    private static FamilyDto family(UUID familyId, UUID chapterId) {
        return new FamilyDto(familyId, "FAM-ABCD1234", chapterId, "Ramesh Agrawal", "9876500000", "Vijay Nagar");
    }

    private static Membership membershipRow(UUID familyId, int year, MembershipStatus status, LocalDate paidOn) {
        Membership membership = Membership.builder()
                .chapterId(CHAPTER_ID)
                .familyId(familyId)
                .year(year)
                .feeAmount(BigDecimal.valueOf(250))
                .status(status)
                .paidAt(status == MembershipStatus.ACTIVE ? paidOn.atStartOfDay(FinancialYearUtil.INDIA_ZONE).toInstant() : null)
                .build();
        membership.setId(UUID.randomUUID());
        return membership;
    }

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private MembershipPaymentRepository paymentRepository;

    @Mock
    private FamilyClient familyClient;

    @Mock
    private BranchClient branchClient;

    @Mock
    private NotificationPublisher notificationPublisher;

    private MembershipServiceImpl membershipService;

    @BeforeEach
    void setUp() {
        membershipService = new MembershipServiceImpl(membershipRepository, paymentRepository, familyClient,
                branchClient, notificationPublisher);
    }

    // --- computeStatus branches (via getStatus) - the highest-risk logic in this feature ---

    @Test
    void getStatus_activeWhenCurrentFinancialYearIsPaid() {
        when(familyClient.getFamily(FAMILY_ID)).thenReturn(Optional.of(family(FAMILY_ID, CHAPTER_ID)));
        Membership current = membershipRow(FAMILY_ID, CURRENT_FY, MembershipStatus.ACTIVE, LocalDate.now());
        when(membershipRepository.findByFamilyIdOrderByYearDesc(FAMILY_ID)).thenReturn(List.of(current));

        MembershipStatusDto result = membershipService.getStatus(ownScope(), FAMILY_ID);

        assertThat(result.status()).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(result.currentFinancialYearPaid()).isTrue();
        assertThat(result.lastPaidFinancialYear()).isEqualTo(CURRENT_FY);
    }

    @Test
    void getStatus_pendingRenewalWhenOnlyThePreviousFinancialYearWasPaid() {
        when(familyClient.getFamily(FAMILY_ID)).thenReturn(Optional.of(family(FAMILY_ID, CHAPTER_ID)));
        Membership previous = membershipRow(FAMILY_ID, CURRENT_FY - 1, MembershipStatus.ACTIVE, LocalDate.now().minusYears(1));
        when(membershipRepository.findByFamilyIdOrderByYearDesc(FAMILY_ID)).thenReturn(List.of(previous));

        MembershipStatusDto result = membershipService.getStatus(ownScope(), FAMILY_ID);

        assertThat(result.status()).isEqualTo(MembershipStatus.PENDING_RENEWAL);
        assertThat(result.currentFinancialYearPaid()).isFalse();
        assertThat(result.lastPaidFinancialYear()).isEqualTo(CURRENT_FY - 1);
    }

    @Test
    void getStatus_expiredWhenTwoOrMoreFinancialYearsUnpaid() {
        when(familyClient.getFamily(FAMILY_ID)).thenReturn(Optional.of(family(FAMILY_ID, CHAPTER_ID)));
        Membership old = membershipRow(FAMILY_ID, CURRENT_FY - 2, MembershipStatus.ACTIVE, LocalDate.now().minusYears(2));
        when(membershipRepository.findByFamilyIdOrderByYearDesc(FAMILY_ID)).thenReturn(List.of(old));

        MembershipStatusDto result = membershipService.getStatus(ownScope(), FAMILY_ID);

        assertThat(result.status()).isEqualTo(MembershipStatus.EXPIRED);
    }

    @Test
    void getStatus_expiredWhenFamilyNeverHadAPaidRow() {
        when(familyClient.getFamily(FAMILY_ID)).thenReturn(Optional.of(family(FAMILY_ID, CHAPTER_ID)));
        when(membershipRepository.findByFamilyIdOrderByYearDesc(FAMILY_ID)).thenReturn(List.of());

        MembershipStatusDto result = membershipService.getStatus(ownScope(), FAMILY_ID);

        assertThat(result.status()).isEqualTo(MembershipStatus.EXPIRED);
        assertThat(result.lastPaymentDate()).isNull();
        assertThat(result.lastPaidFinancialYear()).isNull();
    }

    @Test
    void getStatus_throwsNotFoundWhenFamilyIsNotVisibleToCaller() {
        when(familyClient.getFamily(FAMILY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> membershipService.getStatus(ownScope(), FAMILY_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- recordTransaction ---

    @Test
    void recordTransaction_stampsMembershipPaidAtFromRequestedPaymentDateNotNow() {
        when(familyClient.getFamily(FAMILY_ID)).thenReturn(Optional.of(family(FAMILY_ID, CHAPTER_ID)));
        when(membershipRepository.findByChapterIdAndFamilyIdAndYear(CHAPTER_ID, FAMILY_ID, CURRENT_FY)).thenReturn(Optional.empty());
        when(membershipRepository.save(any(Membership.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.saveAndFlush(any(MembershipPayment.class))).thenAnswer(inv -> inv.getArgument(0));

        // A deliberately backdated payment - if the bug this replaces (always Instant.now()) were
        // still present, the captured paidAt would equal "now", not this date.
        LocalDate backdatedPaymentDate = LocalDate.now().minusDays(10);
        RecordTransactionRequest request = new RecordTransactionRequest(FAMILY_ID, CURRENT_FY,
                BigDecimal.valueOf(250), backdatedPaymentDate, PaymentMethod.CASH, "TXN-1", null);

        membershipService.recordTransaction(chapterScope(), request);

        ArgumentCaptor<Membership> captor = ArgumentCaptor.forClass(Membership.class);
        verify(membershipRepository).save(captor.capture());
        assertThat(captor.getValue().getPaidAt())
                .isEqualTo(backdatedPaymentDate.atStartOfDay(FinancialYearUtil.INDIA_ZONE).toInstant());
        assertThat(captor.getValue().getStatus()).isEqualTo(MembershipStatus.ACTIVE);
    }

    @Test
    void recordTransaction_createsPaymentWithCreatedByFromScope() {
        when(familyClient.getFamily(FAMILY_ID)).thenReturn(Optional.of(family(FAMILY_ID, CHAPTER_ID)));
        when(membershipRepository.findByChapterIdAndFamilyIdAndYear(CHAPTER_ID, FAMILY_ID, CURRENT_FY)).thenReturn(Optional.empty());
        when(membershipRepository.save(any(Membership.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.saveAndFlush(any(MembershipPayment.class))).thenAnswer(inv -> inv.getArgument(0));

        RecordTransactionRequest request = new RecordTransactionRequest(FAMILY_ID, CURRENT_FY,
                BigDecimal.valueOf(250), LocalDate.now(), PaymentMethod.UPI, "TXN-2", "Paid at kiosk");

        MembershipTransactionDto result = membershipService.recordTransaction(chapterScope(), request);

        assertThat(result.createdBy()).isEqualTo(USER_ID);
        assertThat(result.remarks()).isEqualTo("Paid at kiosk");
        assertThat(result.familyId()).isEqualTo(FAMILY_ID);
    }

    @Test
    void recordTransaction_rejectsDuplicateForSameFamilyAndFinancialYear() {
        when(familyClient.getFamily(FAMILY_ID)).thenReturn(Optional.of(family(FAMILY_ID, CHAPTER_ID)));
        Membership existing = membershipRow(FAMILY_ID, CURRENT_FY, MembershipStatus.ACTIVE, LocalDate.now());
        when(membershipRepository.findByChapterIdAndFamilyIdAndYear(CHAPTER_ID, FAMILY_ID, CURRENT_FY))
                .thenReturn(Optional.of(existing));

        RecordTransactionRequest request = new RecordTransactionRequest(FAMILY_ID, CURRENT_FY,
                BigDecimal.valueOf(250), LocalDate.now(), PaymentMethod.CASH, null, null);

        assertThatThrownBy(() -> membershipService.recordTransaction(chapterScope(), request))
                .isInstanceOf(IllegalArgumentException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void recordTransaction_throwsWhenFamilyIsNotFound() {
        when(familyClient.getFamily(FAMILY_ID)).thenReturn(Optional.empty());

        RecordTransactionRequest request = new RecordTransactionRequest(FAMILY_ID, CURRENT_FY,
                BigDecimal.valueOf(250), LocalDate.now(), PaymentMethod.CASH, null, null);

        assertThatThrownBy(() -> membershipService.recordTransaction(chapterScope(), request))
                .isInstanceOf(IllegalArgumentException.class);
        verify(membershipRepository, never()).save(any());
    }

    // --- updateTransaction / isInScope tiers ---

    @Test
    void updateTransaction_updatesPaymentAndRestampsMembershipPaidAt() {
        UUID transactionId = UUID.randomUUID();
        Membership membership = membershipRow(FAMILY_ID, CURRENT_FY, MembershipStatus.ACTIVE, LocalDate.now().minusDays(5));
        MembershipPayment payment = MembershipPayment.builder()
                .chapterId(CHAPTER_ID).membershipId(membership.getId())
                .amount(BigDecimal.valueOf(250)).paymentDate(LocalDate.now().minusDays(5))
                .paymentMethod(PaymentMethod.CASH).build();
        payment.setId(transactionId);
        when(paymentRepository.findById(transactionId)).thenReturn(Optional.of(payment));
        when(membershipRepository.findById(membership.getId())).thenReturn(Optional.of(membership));
        when(paymentRepository.saveAndFlush(any(MembershipPayment.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDate correctedDate = LocalDate.now().minusDays(3);
        UpdateTransactionRequest request = new UpdateTransactionRequest(BigDecimal.valueOf(300), correctedDate,
                PaymentMethod.UPI, "TXN-CORRECTED", "Corrected amount");

        MembershipTransactionDto result = membershipService.updateTransaction(chapterScope(), transactionId, request);

        assertThat(result.amount()).isEqualByComparingTo(BigDecimal.valueOf(300));
        assertThat(result.paymentMode()).isEqualTo(PaymentMethod.UPI);
        assertThat(result.updatedBy()).isEqualTo(USER_ID);

        ArgumentCaptor<Membership> membershipCaptor = ArgumentCaptor.forClass(Membership.class);
        verify(membershipRepository).save(membershipCaptor.capture());
        assertThat(membershipCaptor.getValue().getPaidAt())
                .isEqualTo(correctedDate.atStartOfDay(FinancialYearUtil.INDIA_ZONE).toInstant());
        assertThat(membershipCaptor.getValue().getFeeAmount()).isEqualByComparingTo(BigDecimal.valueOf(300));
    }

    @Test
    void updateTransaction_throwsNotFoundWhenTransactionDoesNotExist() {
        UUID transactionId = UUID.randomUUID();
        when(paymentRepository.findById(transactionId)).thenReturn(Optional.empty());

        UpdateTransactionRequest request = new UpdateTransactionRequest(BigDecimal.TEN, LocalDate.now(),
                PaymentMethod.CASH, null, null);

        assertThatThrownBy(() -> membershipService.updateTransaction(chapterScope(), transactionId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateTransaction_throwsNotFoundWhenTransactionBelongsToAnotherChapter() {
        UUID transactionId = UUID.randomUUID();
        UUID otherChapterId = UUID.randomUUID();
        Membership membership = Membership.builder().chapterId(otherChapterId).familyId(FAMILY_ID).year(CURRENT_FY)
                .status(MembershipStatus.ACTIVE).build();
        membership.setId(UUID.randomUUID());
        MembershipPayment payment = MembershipPayment.builder().chapterId(otherChapterId)
                .membershipId(membership.getId()).amount(BigDecimal.TEN).paymentDate(LocalDate.now())
                .paymentMethod(PaymentMethod.CASH).build();
        payment.setId(transactionId);
        when(paymentRepository.findById(transactionId)).thenReturn(Optional.of(payment));
        when(membershipRepository.findById(membership.getId())).thenReturn(Optional.of(membership));

        UpdateTransactionRequest request = new UpdateTransactionRequest(BigDecimal.TEN, LocalDate.now(),
                PaymentMethod.CASH, null, null);

        // Chapter-tier scope (own chapter only) must not be able to reach another chapter's
        // transaction, even by guessing its id - 404, matching FamilyServiceImpl's own convention.
        assertThatThrownBy(() -> membershipService.updateTransaction(chapterScope(), transactionId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateTransaction_stateScopeReachesSiblingChapterInSameState() {
        UUID transactionId = UUID.randomUUID();
        UUID siblingChapterId = UUID.randomUUID();
        Membership membership = Membership.builder().chapterId(siblingChapterId).familyId(FAMILY_ID).year(CURRENT_FY)
                .status(MembershipStatus.ACTIVE).build();
        membership.setId(UUID.randomUUID());
        MembershipPayment payment = MembershipPayment.builder().chapterId(siblingChapterId)
                .membershipId(membership.getId()).amount(BigDecimal.TEN).paymentDate(LocalDate.now())
                .paymentMethod(PaymentMethod.CASH).build();
        payment.setId(transactionId);
        when(paymentRepository.findById(transactionId)).thenReturn(Optional.of(payment));
        when(membershipRepository.findById(membership.getId())).thenReturn(Optional.of(membership));
        when(membershipRepository.save(any(Membership.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.saveAndFlush(any(MembershipPayment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(branchClient.listAll()).thenReturn(List.of(
                new BranchSummaryDto(CHAPTER_ID, "Indore Chapter", "Indore", "Madhya Pradesh"),
                new BranchSummaryDto(siblingChapterId, "Bhopal Chapter", "Bhopal", "Madhya Pradesh")));

        MembershipAccessScope stateScope = new MembershipAccessScope(CHAPTER_ID, USER_ID, false, true, false);
        UpdateTransactionRequest request = new UpdateTransactionRequest(BigDecimal.TEN, LocalDate.now(),
                PaymentMethod.CASH, null, null);

        MembershipTransactionDto result = membershipService.updateTransaction(stateScope, transactionId, request);

        assertThat(result).isNotNull();
    }

    @Test
    void updateTransaction_ownTierDelegatesToFamilyClientAndRejectsAnotherFamilysTransaction() {
        UUID transactionId = UUID.randomUUID();
        Membership membership = Membership.builder().chapterId(CHAPTER_ID).familyId(FAMILY_ID).year(CURRENT_FY)
                .status(MembershipStatus.ACTIVE).build();
        membership.setId(UUID.randomUUID());
        MembershipPayment payment = MembershipPayment.builder().chapterId(CHAPTER_ID)
                .membershipId(membership.getId()).amount(BigDecimal.TEN).paymentDate(LocalDate.now())
                .paymentMethod(PaymentMethod.CASH).build();
        payment.setId(transactionId);
        when(paymentRepository.findById(transactionId)).thenReturn(Optional.of(payment));
        when(membershipRepository.findById(membership.getId())).thenReturn(Optional.of(membership));
        when(familyClient.getFamily(FAMILY_ID)).thenReturn(Optional.empty());

        UpdateTransactionRequest request = new UpdateTransactionRequest(BigDecimal.TEN, LocalDate.now(),
                PaymentMethod.CASH, null, null);

        assertThatThrownBy(() -> membershipService.updateTransaction(ownScope(), transactionId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- pendingPaymentReport ---

    @Test
    void pendingPaymentReport_excludesFamiliesAlreadyPaidForRequestedFinancialYear() {
        UUID paidFamilyId = UUID.randomUUID();
        UUID unpaidFamilyId = UUID.randomUUID();
        when(familyClient.searchFamilies(null, null, null)).thenReturn(List.of(
                family(paidFamilyId, CHAPTER_ID), family(unpaidFamilyId, CHAPTER_ID)));
        when(membershipRepository.findByFamilyIdOrderByYearDesc(paidFamilyId))
                .thenReturn(List.of(membershipRow(paidFamilyId, CURRENT_FY, MembershipStatus.ACTIVE, LocalDate.now())));
        when(membershipRepository.findByFamilyIdOrderByYearDesc(unpaidFamilyId)).thenReturn(List.of());
        when(branchClient.listAll()).thenReturn(List.of());

        List<MembershipReportRow> result = membershipService.pendingPaymentReport(
                chapterScope(), CURRENT_FY, null, null, null, null);

        assertThat(result).extracting(MembershipReportRow::familyId).containsExactly(unpaidFamilyId);
    }

    @Test
    void pendingPaymentReport_filtersByFamilyIdTextAgainstFamilyCode() {
        UUID otherFamilyId = UUID.randomUUID();
        FamilyDto target = new FamilyDto(FAMILY_ID, "FAM-TARGET1", CHAPTER_ID, "Ramesh Agrawal", "9876500000", "Area");
        FamilyDto other = new FamilyDto(otherFamilyId, "FAM-OTHER99", CHAPTER_ID, "Someone Else", "9876500001", "Area");
        when(familyClient.searchFamilies(null, null, null)).thenReturn(List.of(target, other));
        when(membershipRepository.findByFamilyIdOrderByYearDesc(any())).thenReturn(List.of());
        when(branchClient.listAll()).thenReturn(List.of());

        List<MembershipReportRow> result = membershipService.pendingPaymentReport(
                chapterScope(), CURRENT_FY, "target", null, null, null);

        assertThat(result).extracting(MembershipReportRow::familyId).containsExactly(FAMILY_ID);
    }

    // --- collectionSummary ---

    @Test
    void collectionSummary_sumsPaymentsAndCountsFamiliesByComputedStatus() {
        UUID activeFamilyId = UUID.randomUUID();
        UUID pendingFamilyId = UUID.randomUUID();
        UUID expiredFamilyId = UUID.randomUUID();
        when(familyClient.searchFamilies(null, null, null)).thenReturn(List.of(
                family(activeFamilyId, CHAPTER_ID), family(pendingFamilyId, CHAPTER_ID), family(expiredFamilyId, CHAPTER_ID)));

        Membership activeRow = membershipRow(activeFamilyId, CURRENT_FY, MembershipStatus.ACTIVE, LocalDate.now());
        when(membershipRepository.findByFamilyIdOrderByYearDesc(activeFamilyId)).thenReturn(List.of(activeRow));
        when(membershipRepository.findByFamilyIdOrderByYearDesc(pendingFamilyId)).thenReturn(
                List.of(membershipRow(pendingFamilyId, CURRENT_FY - 1, MembershipStatus.ACTIVE, LocalDate.now().minusYears(1))));
        when(membershipRepository.findByFamilyIdOrderByYearDesc(expiredFamilyId)).thenReturn(List.of());

        when(membershipRepository.countByChapterIdAndYearAndStatus(CHAPTER_ID, CURRENT_FY, MembershipStatus.ACTIVE))
                .thenReturn(1L);
        when(membershipRepository.findByFamilyIdOrderByYearDesc(activeFamilyId)).thenReturn(List.of(activeRow));
        when(paymentRepository.findByMembershipIdIn(List.of(activeRow.getId())))
                .thenReturn(List.of(MembershipPayment.builder().chapterId(CHAPTER_ID).membershipId(activeRow.getId())
                        .amount(BigDecimal.valueOf(250)).paymentDate(LocalDate.now()).paymentMethod(PaymentMethod.CASH).build()));
        when(branchClient.getBranch(CHAPTER_ID)).thenReturn(Optional.of(
                new BranchSummaryDto(CHAPTER_ID, "Indore Chapter", "Indore", "Madhya Pradesh")));

        CollectionSummaryDto result = membershipService.collectionSummary(chapterScope(), CURRENT_FY);

        assertThat(result.familiesActive()).isEqualTo(1);
        assertThat(result.familiesPending()).isEqualTo(1);
        assertThat(result.familiesExpired()).isEqualTo(1);
        assertThat(result.totalCollected()).isEqualByComparingTo(BigDecimal.valueOf(250));
        assertThat(result.chapterName()).isEqualTo("Indore Chapter");
    }
}
