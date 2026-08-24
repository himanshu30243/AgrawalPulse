package com.agrawalpulse.user.entity;

import com.agrawalpulse.common.entity.BaseEntity;
import com.agrawalpulse.common.model.Gender;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "app_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUser extends BaseEntity {

    @Column(name = "chapter_id", nullable = false)
    private UUID chapterId;

    // Name/DOB/gender/mobile are all nullable at the DB level even though V5's registration flow
    // requires them: accounts created before that migration (or via admin-created CreateUserRequest,
    // which still only asks for email/role) have none of this and must remain valid rows - same
    // "legacy rows have NULL X" precedent as family-service's owner_user_id.
    @Column(name = "first_name")
    private String firstName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @Column(name = "mobile_number")
    private String mobileNumber;

    // Physical column is email_address (V5) - the Java/DTO/JWT-claim name stays "email" throughout
    // the codebase (JwtRolesConverter, CurrentTenantResolver, every frontend type), so only this
    // one mapping needed to change rather than every reference to it.
    @Column(name = "email_address", nullable = false, unique = true)
    private String email;

    @Column(name = "cognito_sub", unique = true)
    private String cognitoSub;

    // BCrypt hash, never a plaintext or reversibly-encrypted password (see PasswordEncoderConfig).
    // Nullable for the same legacy-row reason as the name/DOB fields above: an account with no
    // hash simply can never pass DbLocalCredentialAuthenticator's password check, which is the
    // correct behavior for a row that predates password-based login entirely.
    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    // Exactly one role per user (V2 collapsed the previous app_user_roles many-to-many). What
    // the user may actually do comes from the permissions mapped to this role, never from the
    // role code itself.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
