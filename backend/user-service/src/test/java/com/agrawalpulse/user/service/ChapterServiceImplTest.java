package com.agrawalpulse.user.service;

import com.agrawalpulse.common.exception.ResourceNotFoundException;
import com.agrawalpulse.user.dto.ChapterDto;
import com.agrawalpulse.user.dto.CreateChapterRequest;
import com.agrawalpulse.user.entity.Chapter;
import com.agrawalpulse.user.repository.ChapterRepository;
import com.agrawalpulse.user.repository.ChapterResolutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

// First test class ChapterServiceImpl has had - scoped to the new listUnstaffedChapters method
// (added to support self-registration's chapter auto-creation) plus the existing getChapter's
// not-found path, which had no coverage at all before.
@ExtendWith(MockitoExtension.class)
class ChapterServiceImplTest {

    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private ChapterResolutionRepository chapterResolutionRepository;

    private ChapterServiceImpl chapterService;

    @BeforeEach
    void setUp() {
        chapterService = new ChapterServiceImpl(chapterRepository, chapterResolutionRepository);
    }

    @Test
    void createChapter_persistsAndReturnsTheGivenNameCityState() {
        when(chapterRepository.save(any(Chapter.class))).thenAnswer(inv -> inv.getArgument(0));

        ChapterDto result = chapterService.createChapter(new CreateChapterRequest("Pune Chapter", "Pune", "Maharashtra"));

        assertThat(result.name()).isEqualTo("Pune Chapter");
        assertThat(result.city()).isEqualTo("Pune");
        assertThat(result.state()).isEqualTo("Maharashtra");
    }

    @Test
    void getChapter_throwsResourceNotFound_forAnUnknownId() {
        UUID chapterId = UUID.randomUUID();
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chapterService.getChapter(chapterId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resolveOrCreateChapter_returnsTheFullChapterMatchingTheResolvedId() {
        UUID resolvedId = UUID.randomUUID();
        when(chapterResolutionRepository.resolveOrCreateChapter("Pune", "Maharashtra")).thenReturn(resolvedId);
        Chapter chapter = Chapter.builder().name("Pune Chapter").city("Pune").state("Maharashtra").build();
        chapter.setId(resolvedId);
        when(chapterRepository.findById(resolvedId)).thenReturn(Optional.of(chapter));

        ChapterDto result = chapterService.resolveOrCreateChapter("Pune", "Maharashtra");

        assertThat(result.id()).isEqualTo(resolvedId);
        assertThat(result.city()).isEqualTo("Pune");
    }

    @Test
    void listUnstaffedChapters_delegatesToTheRepositoryQuery() {
        // Chapter's @Builder doesn't cover BaseEntity's inherited `id` field (plain @Builder, not
        // @SuperBuilder) - set it via the inherited setter instead.
        Chapter unstaffed = Chapter.builder().name("New City Chapter").city("Nagpur").state("Maharashtra").build();
        unstaffed.setId(UUID.randomUUID());
        when(chapterRepository.findUnstaffed()).thenReturn(List.of(unstaffed));

        List<ChapterDto> result = chapterService.listUnstaffedChapters();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).city()).isEqualTo("Nagpur");
    }
}
