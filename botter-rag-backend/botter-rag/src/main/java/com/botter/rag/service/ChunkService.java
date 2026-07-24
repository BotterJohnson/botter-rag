package com.botter.rag.service;


import com.botter.rag.service.loader.ParseResult;
import com.botter.rag.service.splitter.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChunkService {

    private static final int MIN_CHUNK_LENGTH = 20;

    private final SlidingWindowChunkSplitter slidingWindowSplitter;
    private final StructureAwareChunkSplitter structureAwareSplitter;

    @Value("${rag.chunk.size:512}")
    private int defaultChunkSize;

    @Value("${rag.chunk.overlap:64}")
    private int defaultOverlap;

    /**
     * 对解析结果进行分块。
     * 如果文档有清晰的章节结构，使用结构感知分块；否则使用固定窗口分块。
     */
    public List<ChunkResult> chunk(ParseResult parseResult) {


        return chunk(parseResult, ChunkConfig.defaultConfig());
    }

    public List<ChunkResult> chunk(ParseResult parseResult, ChunkConfig config) {
        if (parseResult == null || !parseResult.isSuccess()) {
            return List.of();
        }
        validateInputs(parseResult, config);

        // 判断是否应该用结构感知分块：文档有明显标题结构
        boolean hasStructure = parseResult.getPages().stream()
                .anyMatch(p -> p.getSectionTitle() != null);

        ChunkSplitter splitter = (hasStructure && config.isStructureAware())
                ? structureAwareSplitter
                : slidingWindowSplitter;

        List<ChunkResult> chunks = normalizeChunks(splitter.split(parseResult, config), config);

        log.debug("[分块] 完成分块：策略={}，共{}块，总字符={}",
                splitter.getClass().getSimpleName(),
                chunks.size(),
                chunks.stream().mapToInt(c -> c.getContent().length()).sum());

        return chunks;
    }

    private void validateInputs(ParseResult parseResult, ChunkConfig config) {
        if (parseResult.getPages() == null) {
            throw new IllegalArgumentException("parseResult.pages must not be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        config.validate();
    }

    /**
     * 清理分块结果并重新编号。
     * 短块优先与同页、同章节的相邻块合并，避免有价值的尾部内容被直接丢弃；
     * 无法安全合并的碎片才会被过滤，最终保证 chunkIndex 从 0 开始连续递增。
     */
    private List<ChunkResult> normalizeChunks(List<ChunkResult> chunks, ChunkConfig config) {
        List<ChunkResult> normalized = new ArrayList<>(chunks.size());
        for (ChunkResult chunk : chunks) {
            if (chunk != null && chunk.getContent() != null && !chunk.getContent().isBlank()) {
                normalized.add(chunk);
            }
        }

        int index = 0;
        while (index < normalized.size()) {
            ChunkResult current = normalized.get(index);
            if (current.getContent().length() >= MIN_CHUNK_LENGTH) {
                index++;
                continue;
            }

            // 优先向前合并，保持正文的自然阅读顺序；失败后再尝试与后块合并。
            if (index > 0) {
                ChunkResult merged = mergeIfPossible(normalized.get(index - 1), current, config);
                if (merged != null) {
                    normalized.set(index - 1, merged);
                    normalized.remove(index);
                    continue;
                }
            }

            if (index + 1 < normalized.size()) {
                ChunkResult merged = mergeIfPossible(current, normalized.get(index + 1), config);
                if (merged != null) {
                    normalized.set(index, merged);
                    normalized.remove(index + 1);
                    continue;
                }
            }

            normalized.remove(index);
        }

        List<ChunkResult> result = new ArrayList<>(normalized.size());
        for (int i = 0; i < normalized.size(); i++) {
            ChunkResult chunk = normalized.get(i);
            result.add(copyChunk(chunk, i, chunk.getContent()));
        }
        return List.copyOf(result);
    }

    /**
     * 只有来源元数据一致且合并后不超过块上限时才允许合并，
     * 防止短块跨页或跨章节后导致引用位置失真。
     */
    private ChunkResult mergeIfPossible(ChunkResult left, ChunkResult right, ChunkConfig config) {
        if (!Objects.equals(left.getPageNum(), right.getPageNum())
                || !Objects.equals(left.getSectionTitle(), right.getSectionTitle())) {
            return null;
        }

        String mergedContent = mergeContent(left.getContent(), right.getContent(), config.getChunkOverlap());
        if (mergedContent.length() > config.getChunkSize()) {
            return null;
        }
        return copyChunk(left, left.getChunkIndex(), mergedContent);
    }

    /**
     * 合并相邻内容时消除滑动窗口产生的公共部分；找不到重叠时用换行连接，
     * 避免两个独立文本片段直接粘连成错误词句。
     */
    private String mergeContent(String left, String right, int overlap) {
        int maxOverlap = Math.min(overlap, Math.min(left.length(), right.length()));
        for (int length = maxOverlap; length > 0; length--) {
            if (left.regionMatches(left.length() - length, right, 0, length)) {
                return left + right.substring(length);
            }
        }
        String separator = Character.isWhitespace(left.charAt(left.length() - 1))
                || Character.isWhitespace(right.charAt(0)) ? "" : "\n";
        return left + separator + right;
    }

    /** 复制结果并重新计算合并后内容的 Token 估算值。 */
    private ChunkResult copyChunk(ChunkResult source, int chunkIndex, String content) {
        return ChunkResult.builder()
                .chunkIndex(chunkIndex)
                .content(content)
                .pageNum(source.getPageNum())
                .sectionTitle(source.getSectionTitle())
                .estimatedTokens(estimateTokens(content))
                .build();
    }

    private int estimateTokens(String text) {
        int chineseChars = 0;
        int otherChars = 0;
        for (char c : text.toCharArray()) {
            if (c >= '\u4e00' && c <= '\u9fff') {
                chineseChars++;
            } else if (!Character.isWhitespace(c)) {
                otherChars++;
            }
        }
        return (int) (chineseChars * 1.5 + otherChars * 0.3);
    }
}
