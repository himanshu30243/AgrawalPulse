package com.agrawalpulse.family.entity;

import com.agrawalpulse.common.entity.BaseEntity;
import com.agrawalpulse.common.model.Gender;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "families")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Family extends BaseEntity {

    // No REFERENCES chapters(id) here - chapters is owned by user-service, a separately
    // deployed service; a cross-service FK would couple migration/deploy order between the
    // two (see docs/microservices-contract.md "Database" section). Still indexed (V1__init.sql)
    // since it's the primary tenant-scoping column for every query in this service.
    @Column(name = "chapter_id", nullable = false)
    private UUID chapterId;

    // Split per the family-registration wizard spec (First/Middle/Last, alphabets only,
    // validated in CreateFamilyRequest). headMiddleName stays nullable; the other two mirror
    // headOfFamilyName's original NOT NULL-ness.
    @Column(name = "head_first_name", nullable = false)
    private String headFirstName;

    @Column(name = "head_middle_name")
    private String headMiddleName;

    @Column(name = "head_last_name", nullable = false)
    private String headLastName;

    // Server-computed concatenation of the three fields above (see FamilyServiceImpl) - kept as
    // its own column rather than a @Formula/DTO-only value because it's still the display value
    // used across FamiliesListPage/FamilyMembersDialog and is convenient to query/sort on
    // directly. No longer part of CreateFamilyRequest - the client can't set it independently of
    // the three name parts.
    @Column(name = "head_of_family_name", nullable = false)
    private String headOfFamilyName;

    // Human-facing display code (e.g. "FAM-7K2XQ9RT"), generated once at creation - the UUID id
    // stays the real primary key/foreign-key target everywhere (tenant-safe, non-guessable,
    // no cross-service coordination needed to generate). A DB-native auto-increment secondary
    // column was deliberately not used instead: JPA's @GeneratedValue only applies to @Id fields,
    // and a portable auto-increment on a non-PK column needs dialect-specific handling that
    // would behave differently under Postgres (local/prod) vs H2 (quick local testing profile,
    // see application-h2.yml) - a random code needs no DB-specific machinery and is exactly as
    // readable. See FamilyServiceImpl for how it's generated.
    @Column(name = "family_code", unique = true, updatable = false)
    private String familyCode;

    // All fields below are optional at the DB layer - required-ness for the wizard's "mandatory"
    // fields is enforced only at the CreateFamilyRequest Bean Validation layer, matching this
    // service's existing precedent (only the head-name columns above are DB NOT NULL).

    @Column(name = "family_name")
    private String familyName;

    @Enumerated(EnumType.STRING)
    @Column(name = "head_gender")
    private Gender headGender;

    @Column(name = "head_date_of_birth")
    private LocalDate headDateOfBirth;

    @Column(name = "mobile_number")
    private String mobileNumber;

    @Column(name = "email")
    private String email;

    @Column(name = "aadhaar_number")
    private String aadhaarNumber;

    // Address
    @Column(name = "address")
    private String address;

    @Column(name = "district")
    private String district;

    @Column(name = "country")
    private String country;

    // Deliberately separate from chapters.state/chapters.city (owned by user-service, see
    // analytics-service's population/by-city|state queries): a family's actual residence can
    // differ from the city/state its chapter is nominally headquartered in, and this service
    // must never depend on a cross-service column for its own required fields.
    @Column(name = "state")
    private String state;

    // Server-derived from district (see FamilyServiceImpl.createFamily) - not part of
    // CreateFamilyRequest, matching the wizard spec's "Region/City: auto-populate, read only".
    @Column(name = "city")
    private String city;

    @Column(name = "area_locality")
    private String areaLocality;

    @Column(name = "pin_code")
    private String pinCode;

    // Community
    @Enumerated(EnumType.STRING)
    @Column(name = "samaj")
    private Samaj samaj;

    @Column(name = "gotra")
    private String gotra;

    @Column(name = "native_place")
    private String nativePlace;

    // Financial & social (all optional per source requirements)
    @Column(name = "occupation_business_type")
    private String occupationBusinessType;

    // Free text, not an enum: income brackets are a policy decision nobody has specified yet
    // (e.g. exact rupee cutoffs) - storing free text avoids guessing wrong boundary values.
    @Column(name = "annual_income_range")
    private String annualIncomeRange;

    @Enumerated(EnumType.STRING)
    @Column(name = "family_category")
    private FamilyCategory familyCategory;

    @Column(name = "own_two_wheeler", nullable = false)
    private Boolean ownTwoWheeler;

    @Column(name = "own_four_wheeler", nullable = false)
    private Boolean ownFourWheeler;

    @Column(name = "own_home", nullable = false)
    private Boolean ownHome;

    @Column(name = "own_plot", nullable = false)
    private Boolean ownPlot;

    @Column(name = "willing_to_contribute")
    private Boolean willingToContribute;

    // The user who registered this family. Nullable: rows predating V2__family_owner.sql have no
    // known owner and never count toward anyone's registration cap. Not a JPA relationship -
    // app_users lives in user-service (see that migration's note on cross-service FKs).
    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
