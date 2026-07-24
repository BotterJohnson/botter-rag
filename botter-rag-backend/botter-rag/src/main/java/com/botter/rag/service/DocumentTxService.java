package com.botter.rag.service;

import com.botter.rag.entity.KbDocument;
import com.botter.rag.repository.KbDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * @ProjectName botter-rag-backend
 * @Author Botter
 * @Create 2026-07-22 18:57
 * @Description 描述信息
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentTxService {

    private final KbDocumentRepository documentRepository;
    private final MinioStorageService minioService;

    /**
     * 在一个事务里完成：上传新文件 + 更新文档记录。
     * 返回旧文件路径，供事务提交后异步删除。
     */
    @Transactional
    public String updateDocumentRecord(Long docId, MultipartFile newFile) {
        KbDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("文档不存在：" + docId));
        if (Boolean.TRUE.equals(doc.getIsDeleted())) {
            throw new RuntimeException("文档已删除，无法替换：" + docId);
        }

        String oldMinioPath = doc.getMinioPath();
        String newMinioPath = minioService.upload(doc.getKbId(),newFile );

        doc.setFileName(newFile.getOriginalFilename());
        doc.setFileSize(newFile.getSize());
        doc.setMinioPath(newMinioPath);
        doc.setVersion(doc.getVersion() + 1);
        doc.setStatus(KbDocument.DocumentStatus.PENDING);
        doc.setErrorMsg(null);
        // 重建索引前清理上一轮的统计字段——避免前端轮询时看到旧值产生"已索引完"的误解
        doc.setChunkCount(null);
        doc.setTokenCount(null);
        doc.setIndexedAt(null);
        documentRepository.save(doc);

        log.info("[DocumentTx] 文档记录更新：docId={}，newVersion={}，newFile={}",
                docId, doc.getVersion(), newFile.getOriginalFilename());
        return oldMinioPath;
    }

    /**
     * 强制重建索引（文件未变，只是想用新策略重新切分/向量化）。
     */
    @Transactional
    public void forceReindex(Long docId) {
        KbDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("文档不存在：" + docId));
        if (Boolean.TRUE.equals(doc.getIsDeleted())) {
            throw new RuntimeException("文档已删除，无法重建：" + docId);
        }

        doc.setVersion(doc.getVersion() + 1);
        doc.setStatus(KbDocument.DocumentStatus.PENDING);
        doc.setErrorMsg(null);
        // 同样清理统计字段
        doc.setChunkCount(null);
        doc.setTokenCount(null);
        doc.setIndexedAt(null);
        documentRepository.save(doc);

        log.info("[DocumentTx] 强制重建版本递增：docId={}，newVersion={}", docId, doc.getVersion());
    }

}
