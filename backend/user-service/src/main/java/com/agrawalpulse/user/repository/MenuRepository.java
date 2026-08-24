package com.agrawalpulse.user.repository;

import com.agrawalpulse.user.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuRepository extends JpaRepository<Menu, UUID> {

    Optional<Menu> findByMenuKey(String menuKey);

    boolean existsByMenuKey(String menuKey);

    List<Menu> findAllByOrderByDisplayOrderAscMenuNameAsc();

    // The menus a role may see, ordered for direct rendering. Inactive menus are excluded here
    // rather than at the caller so deactivating a menu hides it everywhere at once.
    @Query("""
            SELECT m FROM Role r
            JOIN r.menus m
            WHERE r.roleId = :roleId AND m.active = true
            ORDER BY m.displayOrder ASC, m.menuName ASC
            """)
    List<Menu> findActiveMenusForRole(UUID roleId);
}
