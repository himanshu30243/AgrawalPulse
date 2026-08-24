package com.agrawalpulse.user.repository;

import com.agrawalpulse.user.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChapterRepository extends JpaRepository<Chapter, UUID> {
}
