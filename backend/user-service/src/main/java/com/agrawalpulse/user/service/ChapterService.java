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
}
