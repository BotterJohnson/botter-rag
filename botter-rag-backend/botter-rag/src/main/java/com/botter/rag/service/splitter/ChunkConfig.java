package com.botter.rag.service.splitter;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChunkConfig {

    /** 每块最大字符数 */
    @Builder.Default
    private int chunkSize = 512;

    /** 相邻块的重叠字符数，避免信息在块边界被截断 */
    @Builder.Default
    private int chunkOverlap = 64;

    /** 是否启用结构感知分块（默认 true：文档若有 sectionTitle 即用结构感知，没有则自动回落到滑动窗口） */
    @Builder.Default
    private boolean structureAware = true;

    public static ChunkConfig defaultConfig() {
        return ChunkConfig.builder().build();
    }

    /** 校验分块参数，避免窗口无法向前推进或跳过正文。 */
    public void validate() {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be greater than 0");
        }
        if (chunkOverlap < 0 || chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException(
                    "chunkOverlap must be greater than or equal to 0 and less than chunkSize");
        }
    }
}
