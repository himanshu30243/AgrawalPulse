package com.agrawalpulse.membership.service;

import com.agrawalpulse.membership.dto.CreateMembershipRequest;
import com.agrawalpulse.membership.dto.MembershipDto;
import com.agrawalpulse.membership.dto.MembershipPaymentDto;
import com.agrawalpulse.membership.dto.RecordPaymentRequest;

import java.util.List;
import java.util.UUID;

public interface MembershipService {

    MembershipDto createMembership(UUID chapterId, CreateMembershipRequest request);

    MembershipDto getMembership(UUID chapterId, UUID membershipId);

    List<MembershipDto> listMembershipsForChapter(UUID chapterId, int year);

    MembershipPaymentDto recordPayment(UUID chapterId, UUID membershipId, RecordPaymentRequest request);

    List<MembershipPaymentDto> listPayments(UUID chapterId, UUID membershipId);
}
