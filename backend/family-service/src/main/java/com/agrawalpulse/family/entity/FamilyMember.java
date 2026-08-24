package com.agrawalpulse.family.entity;

import com.agrawalpulse.common.entity.BaseEntity;
import com.agrawalpulse.common.model.Gender;
import com.agrawalpulse.common.model.MaritalStatus;
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

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "family_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyMember extends BaseEntity {

    // Denormalized from the owning Family at creation time (see FamilyServiceImpl.addFamilyMember)
    // so tenant scoping is a direct row-level column check, not something inferred by joining to
    // families on every read - see the comment at the top of V1__init.sql for why. Also no
    // REFERENCES chapters(id): chapters is owned by user-service (see Family.chapterId).
    @Column(name = "chapter_id", nullable = false)
    private UUID chapterId;

    // Same-service parent (families is owned by this service too), so this one keeps a real FK.
    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(nullable = false)
    private String name;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    // Nullable: a member's relationship to the head is unknown/unset until the registering admin
    // fills it in - the head of family's own row is the natural RelationshipToHead.SELF entry.
    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_to_head")
    private RelationshipToHead relationshipToHead;

    @Column
    private String education;

    @Column(name = "institute_name")
    private String instituteName;

    @Column
    private String profession;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "mobile_number")
    private String mobileNumber;

    @Column
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group")
    private BloodGroup bloodGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "marital_status", nullable = false)
    @Builder.Default
    private MaritalStatus maritalStatus = MaritalStatus.SINGLE;
}
