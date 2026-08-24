package com.agrawalpulse.family.dto;

import com.agrawalpulse.common.model.Gender;
import com.agrawalpulse.common.model.MaritalStatus;
import com.agrawalpulse.family.entity.BloodGroup;
import com.agrawalpulse.family.entity.RelationshipToHead;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public record CreateFamilyMemberRequest(
        @NotBlank String name,
        RelationshipToHead relationshipToHead,
        @NotNull @Past LocalDate dateOfBirth,
        @NotNull Gender gender,
        MaritalStatus maritalStatus,
        String education,
        String instituteName,
        String profession,
        String companyName,
        String mobileNumber,
        String email,
        BloodGroup bloodGroup
) {
}
