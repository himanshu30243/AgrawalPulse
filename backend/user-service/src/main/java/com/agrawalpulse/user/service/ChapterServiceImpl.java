package com.agrawalpulse.user.service;

import com.agrawalpulse.common.exception.ResourceNotFoundException;
import com.agrawalpulse.user.dto.ChapterDto;
import com.agrawalpulse.user.dto.CreateChapterRequest;
import com.agrawalpulse.user.entity.Chapter;
import com.agrawalpulse.user.repository.ChapterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
class ChapterServiceImpl implements ChapterService {

    private final ChapterRepository chapterRepository;

    ChapterServiceImpl(ChapterRepository chapterRepository) {
        this.chapterRepository = chapterRepository;
    }

    @Override
    public ChapterDto createChapter(CreateChapterRequest request) {
        Chapter chapter = Chapter.builder()
                .name(request.name())
                .city(request.city())
                .state(request.state())
                .build();
        return toDto(chapterRepository.save(chapter));
    }

    @Override
    @Transactional(readOnly = true)
    public ChapterDto getChapter(UUID chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found: " + chapterId));
        return toDto(chapter);
    }

    @Override
    public ChapterDto updateChapter(UUID chapterId, CreateChapterRequest request) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found: " + chapterId));
        chapter.setName(request.name());
        chapter.setCity(request.city());
        chapter.setState(request.state());
        return toDto(chapter);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChapterDto> listChapters() {
        return chapterRepository.findAll().stream().map(this::toDto).toList();
    }

    private ChapterDto toDto(Chapter chapter) {
        return new ChapterDto(chapter.getId(), chapter.getName(), chapter.getCity(), chapter.getState(),
                chapter.getCreatedAt());
    }
}
