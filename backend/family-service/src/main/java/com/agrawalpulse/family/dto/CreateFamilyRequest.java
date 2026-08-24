package com.agrawalpulse.family.dto;

import com.agrawalpulse.common.model.Gender;
import com.agrawalpulse.family.entity.FamilyCategory;
import com.agrawalpulse.family.entity.Samaj;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

// Field set and required-ness mirror frontend/docs/family-registration.md's wizard steps 1-4.
// city is deliberately absent - it's server-derived from district (see FamilyServiceImpl),
// matching the spec's "Region / City: auto-populate, read only" requirement; a client-supplied
// city could otherwise desync from district.
public record CreateFamilyRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z ]+$") String headFirstName,
        @Pattern(regexp = "^[A-Za-z ]*$") String headMiddleName,
        @NotBlank @Pattern(regexp = "^[A-Za-z ]+$") String headLastName,
        String familyName,
        @NotNull Gender headGender,
        @NotNull @PastOrPresent LocalDate headDateOfBirth,
        @NotBlank @Pattern(regexp = "^[0-9]{10}$") String mobileNumber,
        @Email String email,
        // Optional field, but @Pattern (unlike @Email) doesn't treat "" as automatically valid -
        // it validates the regex against whatever non-null value is actually sent. The wizard
        // sends "" (not null) when the user leaves this blank, so the pattern must explicitly
        // allow empty too, or every submission without an Aadhaar number gets rejected.
        @Pattern(regexp = "^$|^[0-9]{12}$") String aadhaarNumber,
        @NotBlank String address,
        @NotBlank String country,
        @NotBlank String state,
        @NotBlank String district,
        @NotBlank String areaLocality,
        @NotBlank @Pattern(regexp = "^[0-9]{6}$") String pinCode,
        @NotNull Samaj samaj,
        @NotBlank String gotra,
        @NotBlank String nativePlace,
        String occupationBusinessType,
        String annualIncomeRange,
        FamilyCategory familyCategory,
        Boolean ownTwoWheeler,
        Boolean ownFourWheeler,
        Boolean ownHome,
        Boolean ownPlot,
        Boolean willingToContribute
) {
}
