package com.agrawalpulse.family.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Read-only reference data (see V4__pincode_reference_data.sql) - the PIN code itself is the
// natural primary key, not a UUID surrogate, since this table is never joined to by other
// entities and every lookup is by pincode directly.
@Entity
@Table(name = "pincodes")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pincode {

    @Id
    @Column(name = "pincode", length = 6)
    private String pincode;

    @Column(name = "district", nullable = false)
    private String district;

    @Column(name = "state", nullable = false)
    private String state;

    @Column(name = "country", nullable = false)
    private String country;
}
