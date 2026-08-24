package com.agrawalpulse.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

// A single grantable capability, e.g. CREATE_FAMILY. permission_code is the contract: it travels
// in the JWT as a PERM_<code> authority and is what @PreAuthorize expressions across every
// service check, so codes must not be renamed without a migration that rewrites both the seed
// rows and the annotations.
@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "permission_id", updatable = false, nullable = false)
    private UUID permissionId;

    @Column(name = "permission_code", nullable = false, unique = true, length = 80)
    private String permissionCode;

    @Column(name = "permission_name", nullable = false, length = 120)
    private String permissionName;

    @Column(name = "description")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
