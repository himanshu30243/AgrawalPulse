package com.agrawalpulse.user.service;

import com.agrawalpulse.user.dto.ChapterDto;
import com.agrawalpulse.user.dto.CreateChapterRequest;

import java.util.List;
import java.util.UUID;

public interface ChapterService {

    ChapterDto createChapter(CreateChapterRequest request);

    ChapterDto updateChapter(UUID chapterId, CreateChapterRequest request);

    ChapterDto getChapter(UUID chapterId);

    List<ChapterDto> listChapters();

    List<ChapterDto> listUnstaffedChapters();

    // Backs both self-registration (UserServiceImpl#registerUser, called locally) and
    // family-service's edit-family flow (called remotely via BranchClient, over REST - see
    // ChapterController's /resolve endpoint) - same resolve-or-create-by-city/state operation
    // either way, see ChapterResolutionRepository.
    ChapterDto resolveOrCreateChapter(String city, String state);
}
