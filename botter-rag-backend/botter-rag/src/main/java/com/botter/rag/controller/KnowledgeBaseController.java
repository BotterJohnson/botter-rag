package com.botter.rag.controller;


import com.botter.rag.dto.*;
import com.botter.rag.entity.IndexTask;
import com.botter.rag.entity.KbDocument;
import com.botter.rag.entity.KnowledgeBase;
import com.botter.rag.repository.IndexTaskRepository;
import com.botter.rag.repository.KbDocumentRepository;
import com.botter.rag.service.KnowledgeBaseService;
import com.botter.rag.service.MinioStorageService;
import com.botter.rag.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/kb")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final PermissionService permissionService;
    private final KnowledgeBaseService kbService;
    private final KbDocumentRepository documentRepository;
    private final IndexTaskRepository taskRepository;
    private final MinioStorageService minioService;

    /** 查询当前用户可访问的知识库列表（含权限级别） */
    @GetMapping
    public ApiResponse<List<KnowledgeBaseVO>> list() {
        return ApiResponse.ok(kbService.listAccessible());
    }

    /** 创建知识库 */
    @PostMapping
    public ApiResponse<KnowledgeBase> create(@RequestBody KnowledgeBaseCreateRequest req) {
        return ApiResponse.ok(kbService.create(req));
    }

    /** 上传文档到知识库 */
    @PostMapping("/{kbId}/documents")
    public ApiResponse<DocumentUploadResponse> upload(
            @PathVariable Long kbId,
            @RequestParam("file") MultipartFile file) {
        try {
            permissionService.requireWrite(kbId);
        }catch (Exception e){
            log.error("当前用户无写入权限");
        }

        KbDocument doc = kbService.uploadDocument(kbId, file);
        return ApiResponse.ok(DocumentUploadResponse.submitted(doc.getId(), doc.getFileName()));
    }

    /** 查询文档索引状态（前端轮询用） */
    @GetMapping("/{kbId}/documents/{docId}/status")
    public ApiResponse<IndexStatusResponse> getStatus(
            @PathVariable Long kbId,
            @PathVariable Long docId) {
        permissionService.requireRead(kbId);

        KbDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("文档不存在"));

        // 查最新的索引任务（可能有重试）
        IndexTask latestTask = taskRepository
                .findTopByDocIdOrderByCreatedAtDesc(docId)
                .orElse(null);

        IndexStatusResponse resp = new IndexStatusResponse();
        resp.setDocId(doc.getId());
        resp.setFileName(doc.getFileName());
        resp.setStatus(doc.getStatus().name());
        resp.setErrorMsg(doc.getErrorMsg());
        resp.setChunkCount(doc.getChunkCount());
        resp.setTokenCount(doc.getTokenCount());
        resp.setIndexedAt(doc.getIndexedAt() != null ? doc.getIndexedAt().toString() : null);
        resp.setRetryCount(latestTask != null ? latestTask.getRetryCount() : 0);
        return ApiResponse.ok(resp);
    }

    /** 查询知识库的文档列表 */
    @GetMapping("/{kbId}/documents")
    public ApiResponse<List<KbDocument>> listDocuments(@PathVariable Long kbId) {
        permissionService.requireRead(kbId);
        List<KbDocument> docs = documentRepository.findByKbIdAndIsDeletedFalse(kbId);
        return ApiResponse.ok(docs);
    }

    /** 删除文档 */
    @DeleteMapping("/{kbId}/documents/{docId}")
    public ApiResponse<Void> deleteDocument(
            @PathVariable Long kbId,
            @PathVariable Long docId) {
        permissionService.requireWrite(kbId);
        kbService.deleteDocument(docId);
        return ApiResponse.ok(null);
    }

    /** 下载原始文件 */
    @GetMapping("/{kbId}/documents/{docId}/download")
    public ResponseEntity<byte[]> download(
            @PathVariable Long kbId,
            @PathVariable Long docId) {
        permissionService.requireRead(kbId);
        KbDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("文档不存在"));
        byte[] content = minioService.download(doc.getMinioPath());
        String encodedName = URLEncoder.encode(doc.getFileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(content);
    }

    /** 重建索引（文档内容更新后触发） */
    @PostMapping("/{kbId}/documents/{docId}/reindex")
    public ApiResponse<String> reindex(
            @PathVariable Long kbId,
            @PathVariable Long docId) {
        permissionService.requireWrite(kbId);
        kbService.reindex(docId);
        return ApiResponse.ok("重建索引任务已提交，请通过 /status 接口查询进度");
    }
}