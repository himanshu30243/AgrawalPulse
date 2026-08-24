package com.agrawalpulse.user.repository;

import com.agrawalpulse.user.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByRoleCode(String roleCode);

    boolean existsByRoleCode(String roleCode);

    List<Role> findAllByActiveTrueOrderByRoleNameAsc();

    // Both collections are LAZY, so anything that serialises a role (the role APIs, and /me)
    // would otherwise trip LazyInitializationException outside the session. Two separate fetch
    // queries rather than one with both joins: joining two independent to-many associations in a
    // single query produces a cartesian product of permissions x menus.
    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.roleId = :roleId")
    Optional<Role> findByIdWithPermissions(UUID roleId);

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.menus WHERE r.roleId = :roleId")
    Optional<Role> findByIdWithMenus(UUID roleId);

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.roleCode = :roleCode")
    Optional<Role> findByRoleCodeWithPermissions(String roleCode);
}
