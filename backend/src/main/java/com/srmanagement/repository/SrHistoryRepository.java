package com.srmanagement.repository;

import com.srmanagement.entity.SrHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SrHistoryRepository extends JpaRepository<SrHistory, Long> {
    List<SrHistory> findBySrIdOrderByCreatedAtDesc(Long srId);
}
