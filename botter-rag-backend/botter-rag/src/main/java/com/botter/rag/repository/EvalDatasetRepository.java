package com.botter.rag.repository;


import com.botter.rag.entity.EvalDataset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvalDatasetRepository extends JpaRepository<EvalDataset, Long> {

    List<EvalDataset> findByKbId(Long kbId);
}