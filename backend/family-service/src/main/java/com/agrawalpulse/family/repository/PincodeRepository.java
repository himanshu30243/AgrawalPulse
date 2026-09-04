package com.agrawalpulse.family.repository;

import com.agrawalpulse.family.entity.Pincode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PincodeRepository extends JpaRepository<Pincode, String> {
}
