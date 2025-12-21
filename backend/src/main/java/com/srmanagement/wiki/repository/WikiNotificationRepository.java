package com.srmanagement.wiki.repository;

import com.srmanagement.wiki.entity.WikiNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 위키 알림 리포지토리
 */
@Repository
public interface WikiNotificationRepository extends JpaRepository<WikiNotification, Long> {

    /**
     * 사용자별 알림 목록 조회 (최신순)
     */
    Page<WikiNotification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 사용자별 읽지 않은 알림 목록 조회
     */
    List<WikiNotification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    /**
     * 사용자별 읽지 않은 알림 개수
     */
    long countByUserIdAndIsReadFalse(Long userId);

    /**
     * 사용자별 전체 알림 개수
     */
    long countByUserId(Long userId);

    /**
     * 사용자별 읽은 알림 개수
     */
    long countByUserIdAndIsReadTrue(Long userId);

    /**
     * 사용자의 모든 알림 읽음 처리
     */
    @Modifying
    @Query("UPDATE WikiNotification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.user.id = :userId AND n.isRead = false")
    void markAllAsReadByUserId(@Param("userId") Long userId);

    /**
     * 특정 문서 관련 알림 삭제
     */
    void deleteByDocumentId(Long documentId);

    /**
     * 사용자의 오래된 읽은 알림 삭제 (30일 이상)
     */
    @Modifying
    @Query("DELETE FROM WikiNotification n WHERE n.user.id = :userId AND n.isRead = true AND n.readAt < CURRENT_TIMESTAMP - 30 DAY")
    void deleteOldReadNotifications(@Param("userId") Long userId);
}
