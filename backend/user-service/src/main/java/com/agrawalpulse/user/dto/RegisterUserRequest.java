package com.agrawalpulse.user.dto;

import com.agrawalpulse.common.model.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Public self-registration. No role field by design (see UserController.selfRegister) - the
 * server always assigns USER, so a registrant cannot escalate by posting one. middleName is the
 * only optional field; deliberately has no @Pattern of its own (an omitted-vs-blank optional
 * string is exactly the regression class documented on FamilyController's blank-fields test -
 * validated only if non-blank, in UserServiceImpl).
 */
public record RegisterUserRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z ]+$", message = "must contain alphabets only") String firstName,
        String middleName,
        @NotBlank @Pattern(regexp = "^[A-Za-z ]+$", message = "must contain alphabets only") String lastName,
        @NotNull @Past LocalDate dateOfBirth,
        @NotNull Gender gender,
        @NotBlank @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "must be a valid 10-15 digit mobile number") String mobileNumber,
        @NotBlank @Email String emailAddress,
        @NotBlank @Size(min = 8, max = 100, message = "must be at least 8 characters") String password
) {
}
