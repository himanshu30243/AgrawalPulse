package com.agrawalpulse.family.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// Editable-after-registration subset of CreateFamilyRequest: head name, contact details, and
// address location (country/state/district=city) - matches this project's "Allow Head Name,
// Mobile, and Email to be edited" / "Allow city/chapter changes later" requirements. Everything
// else on a family (samaj, gotra, financial/social fields, etc.) has no edit path yet - not asked
// for, so not added here.
//
// familyCode is deliberately absent, same reasoning as CreateFamilyRequest's absent `city` -
// FamilyServiceImpl only ever calls generateFamilyCode from createFamily, so there is no code
// path by which an update could regenerate or change it.
public record UpdateFamilyRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z ]+$") String headFirstName,
        @Pattern(regexp = "^[A-Za-z ]*$") String headMiddleName,
        @NotBlank @Pattern(regexp = "^[A-Za-z ]+$") String headLastName,
        @NotBlank @Pattern(regexp = "^[0-9]{10}$") String mobileNumber,
        @Email String email,
        @NotBlank String country,
        @NotBlank String state,
        @NotBlank String district
) {
}
