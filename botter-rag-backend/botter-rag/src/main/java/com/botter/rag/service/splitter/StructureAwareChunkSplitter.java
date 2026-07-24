package com.botter.rag.service.splitter;

import com.botter.rag.service.loader.ParseResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @ProjectName botter-rag-backend
 * @Author Botter
 * @Create 2026-07-19 17:58
 * @Description 描述信息
 */
@Component("structureAwareSplitter")
public class StructureAwareChunkSplitter implements  ChunkSplitter {
    private static final Pattern HEADING_PATTERN = Pattern.compile(
            "^(#{1,3}\\s+|第[一二三四五六七八九十百\\d]+[章节]|[一二三四五六七八九十]+、|\\d+(\\.\\d+)*\\.?\\s*)(.{2,60})$"
    );

    private final SlidingWindowChunkSplitter slidingSplitter;

    public StructureAwareChunkSplitter(SlidingWindowChunkSplitter slidingSplitter) {
        this.slidingSplitter = slidingSplitter;
    }

    @Override
    public List<ChunkResult> split(ParseResult parseResult, ChunkConfig config) {
        validateInputs(parseResult, config);
        // 先按标题边界切分,超大的节再降级到固定窗口
        List<TextSection> sections = extractSections(parseResult);
        List<ChunkResult> chunks = new ArrayList<>();
        int chunkIndex = 0;

        for (TextSection section : sections) {
            if (section.text().length() <= config.getChunkSize()) {
                // 节大小 ≤ chunkSize：整节直接作为一块，保留完整语义
                chunks.add(ChunkResult.builder()
                        .chunkIndex(chunkIndex++)
                        .content(section.text())
                        .pageNum(section.pageNum())
                        .sectionTitle(section.title())
                        .estimatedTokens(estimateTokens(section.text()))
                        .build());
            } else {
                // 节太大，降级到固定窗口切分
                ParseResult sectionResult = ParseResult.builder()
                        .success(true)
                        .pages(List.of(ParseResult.PageContent.builder()
                                .pageNum(section.pageNum())
                                .text(section.text())
                                .sectionTitle(section.title())
                                .build()))
                        .totalPages(1)
                        .build();

                List<ChunkResult> subChunks = slidingSplitter.split(sectionResult, config);
                for (ChunkResult sub : subChunks) {
                    sub.setChunkIndex(chunkIndex++);
                    if (sub.getSectionTitle() == null) sub.setSectionTitle(section.title());
                    chunks.add(sub);
                }
            }
        }

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

    /**
     * 同时识别解析器提供的章节元数据和正文中的标题行。
     * 解析器标题发生变化时必须先收束旧章节，否则后续章节会继承错误的标题和起始页。
     */
    private List<TextSection> extractSections(ParseResult parseResult) {
        List<TextSection> sections = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String currentTitle = null;
        int sectionStartPage = 1;

        for (ParseResult.PageContent page : parseResult.getPages()) {
            String pageTitle = normalizeTitle(page.getSectionTitle());
            if (pageTitle != null && !pageTitle.equals(currentTitle)) {
                // 新的解析器章节是强边界，即使旧章节很短也不能吞并到新章节中。
                addSection(sections, currentTitle, current, sectionStartPage);
                current = new StringBuilder();
                currentTitle = pageTitle;
                sectionStartPage = page.getPageNum();
            }

            String pageText = page.getText();
            if (pageText == null || pageText.isBlank()) {
                continue;
            }

            if (current.isEmpty()) {
                sectionStartPage = page.getPageNum();
            }

            String[] lines = pageText.split("\n");
            for (String line : lines) {
                String stripped = line.strip();
                boolean isHeading = !stripped.isEmpty() && HEADING_PATTERN.matcher(stripped).matches();

                if (isHeading) {
                    // 标题行属于新章节正文，保留一次可提升 Embedding 对章节语义的感知。
                    addSection(sections, currentTitle, current, sectionStartPage);
                    current = new StringBuilder();
                    sectionStartPage = page.getPageNum();
                    currentTitle = stripped;
                    current.append(stripped).append("\n");
                    continue;
                }

                current.append(line).append("\n");
            }
        }

        addSection(sections, currentTitle, current, sectionStartPage);

        return sections;
    }

    /** 将已累积的非空正文固化为章节，空白内容不会生成无效块。 */
    private void addSection(List<TextSection> sections, String title, StringBuilder text, int pageNum) {
        String content = text.toString().strip();
        if (!content.isBlank()) {
            sections.add(new TextSection(title, content, pageNum));
        }
    }

    private String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return null;
        }
        return title.strip();
    }

    private int estimateTokens(String text) {
        if (text == null) return 0;
        int chinese = 0, other = 0;
        for (char c : text.toCharArray()) {
            if (c >= '\u4e00' && c <= '\u9fff') chinese++;
            else if (!Character.isWhitespace(c)) other++;
        }
        return (int) (chinese * 1.5 + other * 0.3);
    }

    record TextSection(String title, String text, int pageNum) {}
}
