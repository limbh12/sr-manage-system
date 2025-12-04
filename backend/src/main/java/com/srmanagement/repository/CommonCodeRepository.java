package com.srmanagement.repository;

import com.srmanagement.entity.CommonCode;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommonCodeRepository extends JpaRepository<CommonCode, Long> {
    List<CommonCode> findByCodeGroupAndIsActiveTrueOrderBySortOrderAsc(String codeGroup);
    List<CommonCode> findByCodeGroupOrderBySortOrderAsc(String codeGroup);
    Optional<CommonCode> findByCodeGroupAndCodeValue(String codeGroup, String codeValue);
    boolean existsByCodeGroupAndCodeValue(String codeGroup, String codeValue);

    @Query("SELECT DISTINCT c.codeGroup FROM CommonCode c ORDER BY c.codeGroup")
    List<String> findDistinctCodeGroups();
}
