package com.botter.DocumentTest;

import com.botter.rag.service.loader.ParseResult;
import com.botter.rag.service.splitter.ChunkConfig;
import com.botter.rag.service.splitter.ChunkResult;
import com.botter.rag.service.splitter.SlidingWindowChunkSplitter;
import com.botter.rag.service.splitter.StructureAwareChunkSplitter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChunkSplitterTest {

    private final SlidingWindowChunkSplitter slidingSplitter = new SlidingWindowChunkSplitter();
    private final StructureAwareChunkSplitter structureSplitter =
            new StructureAwareChunkSplitter(slidingSplitter);

    @Test
    void keepsParserSectionsSeparatedWithTheirOwnMetadata() {
        ParseResult result = ParseResult.builder()
                .success(true)
                .pages(List.of(
                        page(1, "这是概述章节的正文，包含足够的信息用于测试章节边界。", "第一章 概述"),
                        page(2, "这是设计章节的正文，不能被合并到前一个章节中。", "第二章 设计")))
                .totalPages(2)
                .build();

        List<ChunkResult> chunks = structureSplitter.split(result, ChunkConfig.defaultConfig());

        assertThat(chunks).hasSize(2);
        assertThat(chunks).extracting(ChunkResult::getSectionTitle)
                .containsExactly("第一章 概述", "第二章 设计");
        assertThat(chunks).extracting(ChunkResult::getPageNum).containsExactly(1, 2);
        assertThat(chunks.get(0).getContent()).contains("概述章节").doesNotContain("设计章节");
        assertThat(chunks.get(1).getContent()).contains("设计章节");
    }

    @Test
    void preservesShortSectionAndHeadingBoundaryDuringExtraction() {
        ParseResult result = ParseResult.builder()
                .success(true)
                .pages(List.of(page(1,
                        "简短前言\n第一章 正式内容\n这里是第一章的详细正文内容。",
                        null)))
                .totalPages(1)
                .build();

        List<ChunkResult> chunks = structureSplitter.split(result, ChunkConfig.defaultConfig());

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).getContent()).isEqualTo("简短前言");
        assertThat(chunks.get(1).getSectionTitle()).isEqualTo("第一章 正式内容");
        assertThat(chunks.get(1).getContent()).startsWith("第一章 正式内容");
    }

    @Test
    void splitterRejectsInvalidOverlapWhenCalledDirectly() {
        ParseResult result = ParseResult.builder()
                .success(true)
                .pages(List.of(page(1, "正文内容", null)))
                .totalPages(1)
                .build();
        ChunkConfig invalid = ChunkConfig.builder()
                .chunkSize(10)
                .chunkOverlap(-1)
                .build();

        assertThatThrownBy(() -> slidingSplitter.split(result, invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chunkOverlap");
        assertThatThrownBy(() -> structureSplitter.split(result, invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chunkOverlap");
    }

    private ParseResult.PageContent page(int pageNum, String text, String title) {
        return ParseResult.PageContent.builder()
                .pageNum(pageNum)
                .text(text)
                .sectionTitle(title)
                .build();
    }
}
