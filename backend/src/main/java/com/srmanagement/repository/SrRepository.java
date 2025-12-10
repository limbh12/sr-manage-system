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
     * 삭제되지 않은 SR 목록 조회
     * @param pageable 페이지네이션
     * @return Page<Sr>
     */
    Page<Sr> findByDeleted(Boolean deleted, Pageable pageable);

    /**
     * 상태로 SR 목록 조회 (삭제되지 않은 항목만)
     * @param status SR 상태
     * @param deleted 삭제 여부
     * @param pageable 페이지네이션
     * @return Page<Sr>
     */
    Page<Sr> findByStatusAndDeleted(SrStatus status, Boolean deleted, Pageable pageable);

    /**
     * 상태로 SR 목록 조회
     * @param status SR 상태
     * @param pageable 페이지네이션
     * @return Page<Sr>
     */
    Page<Sr> findByStatus(SrStatus status, Pageable pageable);

    /**
     * 우선순위로 SR 목록 조회 (삭제되지 않은 항목만)
     * @param priority 우선순위
     * @param deleted 삭제 여부
     * @param pageable 페이지네이션
     * @return Page<Sr>
     */
    Page<Sr> findByPriorityAndDeleted(Priority priority, Boolean deleted, Pageable pageable);

    /**
     * 우선순위로 SR 목록 조회
     * @param priority 우선순위
     * @param pageable 페이지네이션
     * @return Page<Sr>
     */
    Page<Sr> findByPriority(Priority priority, Pageable pageable);

    /**
     * 상태와 우선순위로 SR 목록 조회 (삭제되지 않은 항목만)
     * @param status SR 상태
     * @param priority 우선순위
     * @param deleted 삭제 여부
     * @param pageable 페이지네이션
     * @return Page<Sr>
     */
    Page<Sr> findByStatusAndPriorityAndDeleted(SrStatus status, Priority priority, Boolean deleted, Pageable pageable);

    /**
     * 상태와 우선순위로 SR 목록 조회
     * @param status SR 상태
     * @param priority 우선순위
     * @param pageable 페이지네이션
     * @return Page<Sr>
     */
    Page<Sr> findByStatusAndPriority(SrStatus status, Priority priority, Pageable pageable);

    /**
     * 제목 또는 설명에서 검색 (삭제 여부 포함)
     * @param search 검색어
     * @param deleted 삭제 여부
     * @param pageable 페이지네이션
     * @return Page<Sr>
     */
    @Query("SELECT s FROM Sr s WHERE s.deleted = :deleted AND (" +
            "LOWER(s.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Sr> searchByTitleOrDescriptionAndDeleted(@Param("search") String search, @Param("deleted") Boolean deleted, Pageable pageable);

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
     * 요청자가 등록한 SR 존재 여부 확인
     * @param requesterId 요청자 ID
     * @return 존재 여부
     */
    boolean existsByRequesterId(Long requesterId);

    /**
     * 담당자로 지정된 SR 존재 여부 확인
     * @param assigneeId 담당자 ID
     * @return 존재 여부
     */
    boolean existsByAssigneeId(Long assigneeId);

    /**
     * 특정 패턴의 SR ID 중 가장 마지막 값을 조회
     * @param pattern SR ID 패턴 (예: SR-2412-%)
     * @return 가장 큰 SR ID
     */
    @Query("SELECT s.srId FROM Sr s WHERE s.srId LIKE :pattern ORDER BY s.srId DESC LIMIT 1")
    String findLastSrId(@Param("pattern") String pattern);

    long countByCategory(String category);
    long countByRequestType(String requestType);
}
