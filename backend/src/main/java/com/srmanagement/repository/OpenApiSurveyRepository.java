package com.srmanagement.repository;

import com.srmanagement.entity.OpenApiSurvey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OpenApiSurveyRepository extends JpaRepository<OpenApiSurvey, Long> {
    
    @Query("SELECT s FROM OpenApiSurvey s LEFT JOIN s.organization o WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(o.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.contactName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "s.contactPhone LIKE CONCAT('%', :keyword, '%'))")
    Page<OpenApiSurvey> search(@Param("keyword") String keyword, Pageable pageable);
}
