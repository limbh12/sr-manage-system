package com.srmanagement.repository;

import com.srmanagement.entity.Priority;
import com.srmanagement.entity.Sr;
import com.srmanagement.entity.SrStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * SR 레포지토리
 */
@Repository
public interface SrRepository extends JpaRepository<Sr, Long> {

    /**
     * 상태로 SR 목록 조회
     * @param status SR 상태
     * @param pageable 페이지네이션
     * @return Page<Sr>
     */
    Page<Sr> findByStatus(SrStatus status, Pageable pageable);

    /**
     * 우선순위로 SR 목록 조회
     * @param priority 우선순위
     * @param pageable 페이지네이션
     * @return Page<Sr>
     */
    Page<Sr> findByPriority(Priority priority, Pageable pageable);

    /**
     * 상태와 우선순위로 SR 목록 조회
     * @param status SR 상태
     * @param priority 우선순위
     * @param pageable 페이지네이션
     * @return Page<Sr>
     */
    Page<Sr> findByStatusAndPriority(SrStatus status, Priority priority, Pageable pageable);

    /**
     * 제목 또는 설명에서 검색
     * @param search 검색어
     * @param pageable 페이지네이션
     * @return Page<Sr>
     */
    @Query("SELECT s FROM Sr s WHERE " +
            "LOWER(s.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.description) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Sr> searchByTitleOrDescription(@Param("search") String search, Pageable pageable);

    /**
     * 요청자 ID로 SR 목록 조회
     * @param requesterId 요청자 ID
     * @param pageable 페이지네이션
     * @return Page<Sr>
     */
    Page<Sr> findByRequesterId(Long requesterId, Pageable pageable);

    /**
     * 담당자 ID로 SR 목록 조회
     * @param assigneeId 담당자 ID
     * @param pageable 페이지네이션
     * @return Page<Sr>
     */
    Page<Sr> findByAssigneeId(Long assigneeId, Pageable pageable);

    /**
     * 특정 패턴의 SR ID 중 가장 마지막 값을 조회
     * @param pattern SR ID 패턴 (예: SR-2412-%)
     * @return 가장 큰 SR ID
     */
    @Query("SELECT s.srId FROM Sr s WHERE s.srId LIKE :pattern ORDER BY s.srId DESC LIMIT 1")
    String findLastSrId(@Param("pattern") String pattern);
}
