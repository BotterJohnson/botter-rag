package com.botter.DocumentTest;

import com.botter.rag.service.ChunkService;
import com.botter.rag.service.loader.ParseResult;
import com.botter.rag.service.splitter.ChunkConfig;
import com.botter.rag.service.splitter.ChunkResult;
import com.botter.rag.service.splitter.SlidingWindowChunkSplitter;
import com.botter.rag.service.splitter.StructureAwareChunkSplitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChunkServiceTest {

    private ChunkService chunkService;

    @BeforeEach
    void setUp() {
        SlidingWindowChunkSplitter slidingSplitter = new SlidingWindowChunkSplitter();
        chunkService = new ChunkService(slidingSplitter, new StructureAwareChunkSplitter(slidingSplitter));
    }

    @Test
    void chunksNotTooLargeOrTooSmall() {
        String longText = "这是一段测试文本。".repeat(200); // 约 1800 字符
        ParseResult result = ParseResult.builder()
                .success(true)
                .pages(List.of(ParseResult.PageContent.builder()
                        .pageNum(1)
                        .text(longText)
                        .build()))
                .totalPages(1)
                .build();

        List<ChunkResult> chunks = chunkService.chunk(result);

        assertThat(chunks).isNotEmpty();
        for (ChunkResult chunk : chunks) {
            // 每块不应超过 chunkSize（findGoodBreakPoint 只向前回退断点，不会超出上限）
            assertThat(chunk.getContent().length()).isLessThanOrEqualTo(512);
            // 每块至少 20 字符（ChunkService 已过滤更短的碎片）
            assertThat(chunk.getContent().length()).isGreaterThanOrEqualTo(20);
        }

        // 验证相邻块有重叠
        if (chunks.size() >= 2) {
            String end0 = chunks.get(0).getContent();
            String start1 = chunks.get(1).getContent();
            // 第一块末尾的内容应该出现在第二块开头附近（重叠）
            String overlapPart = end0.substring(Math.max(0, end0.length() - 64));
            assertThat(start1).contains(overlapPart.substring(0, Math.min(30, overlapPart.length())));
        }
    }

    @Test
    void rejectsInvalidConfigBeforeSplitting() {
        ParseResult result = successfulResult("有效正文内容".repeat(10));

        assertThatThrownBy(() -> chunkService.chunk(result, ChunkConfig.builder()
                .chunkSize(0)
                .chunkOverlap(0)
                .build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chunkSize");

        assertThatThrownBy(() -> chunkService.chunk(result, ChunkConfig.builder()
                .chunkSize(20)
                .chunkOverlap(20)
                .build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chunkOverlap");
    }

    @Test
    void reindexesChunksAfterShortFragmentsAreRemoved() {
        SlidingWindowChunkSplitter splitter = new SlidingWindowChunkSplitter() {
            @Override
            public List<ChunkResult> split(ParseResult parseResult, ChunkConfig config) {
                return List.of(
                        chunk(4, "第一页的有效正文内容足够长，应该被正常保留下来。", 1),
                        chunk(8, "短片段", 2),
                        chunk(12, "第三页的有效正文内容足够长，也应该被正常保留。", 3));
            }
        };
        ChunkService service = new ChunkService(splitter, new StructureAwareChunkSplitter(splitter));

        List<ChunkResult> chunks = service.chunk(successfulResult("用于选择滑动窗口策略的正文"));

        assertThat(chunks).extracting(ChunkResult::getChunkIndex).containsExactly(0, 1);
        assertThat(chunks).extracting(ChunkResult::getPageNum).containsExactly(1, 3);
    }

    @Test
    void mergesShortFragmentWithCompatiblePreviousChunk() {
        SlidingWindowChunkSplitter splitter = new SlidingWindowChunkSplitter() {
            @Override
            public List<ChunkResult> split(ParseResult parseResult, ChunkConfig config) {
                return List.of(
                        chunk(0, "同一页中的有效正文内容已经超过最小长度。", 1),
                        chunk(1, "补充内容", 1));
            }
        };
        ChunkService service = new ChunkService(splitter, new StructureAwareChunkSplitter(splitter));

        List<ChunkResult> chunks = service.chunk(successfulResult("用于选择滑动窗口策略的正文"));

        assertThat(chunks).singleElement().satisfies(chunk -> {
            assertThat(chunk.getChunkIndex()).isZero();
            assertThat(chunk.getContent()).contains("有效正文", "补充内容");
        });
    }

    private ParseResult successfulResult(String text) {
        return ParseResult.builder()
                .success(true)
                .pages(List.of(ParseResult.PageContent.builder()
                        .pageNum(1)
                        .text(text)
                        .build()))
                .totalPages(1)
                .build();
    }

    private ChunkResult chunk(int index, String content, int pageNum) {
        return ChunkResult.builder()
                .chunkIndex(index)
                .content(content)
                .pageNum(pageNum)
                .estimatedTokens(content.length())
                .build();
    }
}
