package com.agrawalpulse.membership.service;

import com.agrawalpulse.membership.dto.CollectionSummaryDto;
import com.agrawalpulse.membership.dto.MembershipReportRow;
import com.agrawalpulse.membership.dto.MembershipStatusDto;
import com.agrawalpulse.membership.dto.MembershipTransactionDto;
import com.agrawalpulse.membership.dto.RecordTransactionRequest;
import com.agrawalpulse.membership.dto.UpdateTransactionRequest;
import com.agrawalpulse.membership.entity.MembershipStatus;

import java.util.List;
import java.util.UUID;

public interface MembershipService {

    MembershipStatusDto getStatus(MembershipAccessScope scope, UUID familyId);

    List<MembershipTransactionDto> getTransactionHistory(MembershipAccessScope scope, UUID familyId);

    MembershipTransactionDto recordTransaction(MembershipAccessScope scope, RecordTransactionRequest request);

    MembershipTransactionDto updateTransaction(MembershipAccessScope scope, UUID transactionId,
                                                UpdateTransactionRequest request);

    List<MembershipStatusDto> listMembers(MembershipAccessScope scope, int financialYear,
                                           MembershipStatus statusFilter);

    List<MembershipReportRow> pendingPaymentReport(MembershipAccessScope scope, int financialYear, String familyId,
                                                     String headOfFamilyName, String mobileNumber,
                                                     String areaLocality);

    CollectionSummaryDto collectionSummary(MembershipAccessScope scope, int financialYear);
}
