package com.agrawalpulse.user.repository;

import com.agrawalpulse.user.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByPermissionCode(String permissionCode);

    boolean existsByPermissionCode(String permissionCode);

    List<Permission> findAllByOrderByPermissionCodeAsc();

    List<Permission> findAllByPermissionCodeIn(Collection<String> permissionCodes);
}
