package com.botter.rag.service.splitter;

import com.botter.rag.service.loader.ParseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @ProjectName botter-rag-backend
 * @Author Botter
 * @Create 2026-07-19 16:48
 * @Description 描述信息
 */

@Component
@Slf4j
public class SlidingWindowChunkSplitter implements ChunkSplitter{
    @Override
    public List<ChunkResult> split(ParseResult parseResult, ChunkConfig config) {
        validateInputs(parseResult, config);
        List<ChunkResult> chunks = new ArrayList<>();
        int chunkIndex = 0;
        for (ParseResult.PageContent pageContent : parseResult.getPages()) {
            var text = pageContent.getText();
            if (text == null || text.isBlank()) {
                continue;
            }

            List<String> pageChunks = splitText(text, config.getChunkSize(), config.getChunkOverlap());


            for (String chunkText : pageChunks) {
                if (chunkText.isBlank()) {
                    continue;
                }
                chunks.add(ChunkResult.builder()
                                .chunkIndex(chunkIndex++)
                                .content(chunkText)
                                .pageNum(pageContent.getPageNum())
                                .sectionTitle(pageContent.getSectionTitle())
                                .estimatedTokens(estimateTokens(chunkText))
                                .build());
            }
        }

        log.debug("[分块] 文档分块完成，共{}块，avgSize={}字符",
                chunks.size(),
                chunks.isEmpty() ? 0 : chunks.stream()
                                       .mapToInt(c -> c.getContent().length()).average().orElse(0));

        return chunks;
    }

    /** 独立调用分块器时同样执行校验，不能只依赖 ChunkService 入口。 */
    private void validateInputs(ParseResult parseResult, ChunkConfig config) {
        if (parseResult == null) {
            throw new IllegalArgumentException("parseResult must not be null");
        }
        if (parseResult.getPages() == null) {
            throw new IllegalArgumentException("parseResult.pages must not be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        config.validate();
    }

    private List<String> splitText(String text, int chunkSize, int overlap) {
        List<String> result = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length()); //end是按分块大小确定位置的
            if (end < text.length()) { //如果够分，找一个好的分割点
                end = findGoodBreakPoint(text , end);
            }
            //先分割一下这个块
            String chunk = text.substring(start, end).strip();

            if (!chunk.isBlank()) {
                result.add(chunk);
            }
            // 已经切到文档末尾，不再回退 overlap，避免最后一块的尾部被当成新块重复输出
            if (end >= text.length()) break;
//            否则回退一个overlap开始重新计算
            int nextStart = end - overlap;
            if (nextStart <= start) {
                nextStart = end;
            }
            start = nextStart;
        }
        return result;
    }


    /**
     * 从 position 向前找到一个好的断点（段落 > 句号 > 逗号 > 空格）。
     * 最多回退 MAX_BACKTRACK 字符，找不到就直接断。
     */
    private static final int MAX_BACKTRACK = 100;

    private int findGoodBreakPoint(String text, int position) {
        int searchLimit = position - MAX_BACKTRACK;  // 回退下限，不能比这个再小

        // 优先级：段落换行 > 句号/问号/感叹号 > 分号/逗号 > 空格
        String[] breakChars = {"\n\n", "\n", "。", "！", "？", "；", "，", " "};

        for (String breakChar : breakChars) {
            int idx = text.lastIndexOf(breakChar, position);
            int breakEnd = idx + breakChar.length();
            if (idx > searchLimit && idx > 0 && breakEnd <= position) {
                return breakEnd;
            }
        }

        return position;  // 找不到好的断点，直接截断
    }


    /**
     * 简单的 Token 估算：中文每字约 1.5 Token，英文每字符约 0.3 Token。
     * 不依赖外部 Tokenizer，近似计算。
     */
    private int estimateTokens(String text) {
        if (text == null) return 0;
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
