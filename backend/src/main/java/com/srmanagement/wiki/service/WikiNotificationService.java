package com.srmanagement.wiki.service;

import com.srmanagement.entity.Role;
import com.srmanagement.entity.User;
import com.srmanagement.repository.UserRepository;
import com.srmanagement.wiki.dto.NotificationResponse;
import com.srmanagement.wiki.entity.WikiDocument;
import com.srmanagement.wiki.entity.WikiNotification;
import com.srmanagement.wiki.entity.WikiNotification.NotificationType;
import com.srmanagement.wiki.repository.WikiNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 위키 알림 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WikiNotificationService {

    private final WikiNotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * 사용자의 알림 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationResponse::fromEntity);
    }

    /**
     * 사용자의 읽지 않은 알림 목록 조회
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 읽지 않은 알림 개수 조회
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * 알림 통계 조회 (전체, 읽지않음, 읽음 개수)
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, Long> getNotificationStats(Long userId) {
        long total = notificationRepository.countByUserId(userId);
        long unread = notificationRepository.countByUserIdAndIsReadFalse(userId);
        long read = notificationRepository.countByUserIdAndIsReadTrue(userId);
        return java.util.Map.of("total", total, "unread", unread, "read", read);
    }

    /**
     * 알림 읽음 처리
     */
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        WikiNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("알림을 찾을 수 없습니다"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new RuntimeException("권한이 없습니다");
        }

        notification.markAsRead();
        notificationRepository.save(notification);
    }

    /**
     * 모든 알림 읽음 처리
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    /**
     * 문서 생성 알림 발송 (모든 사용자에게)
     */
    @Async
    @Transactional
    public void notifyDocumentCreated(WikiDocument document, User createdBy) {
        log.info("문서 생성 알림 발송: documentId={}, createdBy={}", document.getId(), createdBy.getUsername());

        // 모든 사용자에게 알림 발송 (생성자 제외)
        List<User> recipients = userRepository.findAll();
        int sentCount = 0;

        for (User recipient : recipients) {
            if (!recipient.getId().equals(createdBy.getId())) {
                WikiNotification notification = WikiNotification.builder()
                        .user(recipient)
                        .document(document)
                        .type(NotificationType.DOCUMENT_CREATED)
                        .title("새 위키 문서가 생성되었습니다")
                        .message(String.format("'%s' 문서가 %s님에 의해 생성되었습니다.",
                                document.getTitle(), createdBy.getName()))
                        .triggeredBy(createdBy)
                        .build();
                notificationRepository.save(notification);
                sentCount++;
            }
        }

        log.info("문서 생성 알림 발송 완료: {} 명에게 알림 발송", sentCount);
    }

    /**
     * 문서 수정 알림 발송 (모든 사용자에게)
     */
    @Async
    @Transactional
    public void notifyDocumentUpdated(WikiDocument document, User updatedBy) {
        log.info("문서 수정 알림 발송: documentId={}, updatedBy={}", document.getId(), updatedBy.getUsername());

        // 모든 사용자에게 알림 발송 (수정자 제외)
        List<User> recipients = userRepository.findAll();
        int sentCount = 0;

        for (User recipient : recipients) {
            if (!recipient.getId().equals(updatedBy.getId())) {
                WikiNotification notification = WikiNotification.builder()
                        .user(recipient)
                        .document(document)
                        .type(NotificationType.DOCUMENT_UPDATED)
                        .title("위키 문서가 수정되었습니다")
                        .message(String.format("'%s' 문서가 %s님에 의해 수정되었습니다.",
                                document.getTitle(), updatedBy.getName()))
                        .triggeredBy(updatedBy)
                        .build();
                notificationRepository.save(notification);
                sentCount++;
            }
        }

        log.info("문서 수정 알림 발송 완료: {} 명에게 알림 발송", sentCount);
    }

    /**
     * 문서 삭제 알림 발송 (모든 사용자에게)
     */
    @Async
    @Transactional
    public void notifyDocumentDeleted(String documentTitle, User deletedBy, User documentCreator) {
        log.info("문서 삭제 알림 발송: title={}, deletedBy={}", documentTitle, deletedBy.getUsername());

        // 모든 사용자에게 알림 발송 (삭제자 제외)
        List<User> recipients = userRepository.findAll();
        int sentCount = 0;

        for (User recipient : recipients) {
            if (!recipient.getId().equals(deletedBy.getId())) {
                WikiNotification notification = WikiNotification.builder()
                        .user(recipient)
                        .document(null) // 문서가 삭제되었으므로 null
                        .type(NotificationType.DOCUMENT_DELETED)
                        .title("위키 문서가 삭제되었습니다")
                        .message(String.format("'%s' 문서가 %s님에 의해 삭제되었습니다.",
                                documentTitle, deletedBy.getName()))
                        .triggeredBy(deletedBy)
                        .build();
                notificationRepository.save(notification);
                sentCount++;
            }
        }

        log.info("문서 삭제 알림 발송 완료: {} 명에게 알림 발송", sentCount);
    }

    /**
     * 알림 삭제
     */
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        WikiNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("알림을 찾을 수 없습니다"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new RuntimeException("권한이 없습니다");
        }

        notificationRepository.delete(notification);
    }

    // ================================
    // OPEN API 현황조사 알림
    // ================================

    /**
     * 현황조사 생성 알림 발송 (모든 사용자에게)
     */
    @Async
    @Transactional
    public void notifySurveyCreated(Long surveyId, String organizationName, String systemName, User createdBy) {
        log.info("현황조사 생성 알림 발송: surveyId={}, createdBy={}", surveyId, createdBy.getUsername());

        List<User> recipients = userRepository.findAll();
        int sentCount = 0;

        for (User recipient : recipients) {
            if (!recipient.getId().equals(createdBy.getId())) {
                WikiNotification notification = WikiNotification.builder()
                        .user(recipient)
                        .document(null)
                        .type(NotificationType.SURVEY_CREATED)
                        .resourceType("SURVEY")
                        .resourceId(surveyId)
                        .title("새 현황조사가 등록되었습니다")
                        .message(String.format("[%s] %s 현황조사가 %s님에 의해 등록되었습니다.",
                                organizationName, systemName, createdBy.getName()))
                        .triggeredBy(createdBy)
                        .build();
                notificationRepository.save(notification);
                sentCount++;
            }
        }

        log.info("현황조사 생성 알림 발송 완료: {} 명에게 알림 발송", sentCount);
    }

    /**
     * 현황조사 수정 알림 발송 (모든 사용자에게)
     */
    @Async
    @Transactional
    public void notifySurveyUpdated(Long surveyId, String organizationName, String systemName, User updatedBy) {
        log.info("현황조사 수정 알림 발송: surveyId={}, updatedBy={}", surveyId, updatedBy.getUsername());

        List<User> recipients = userRepository.findAll();
        int sentCount = 0;

        for (User recipient : recipients) {
            if (!recipient.getId().equals(updatedBy.getId())) {
                WikiNotification notification = WikiNotification.builder()
                        .user(recipient)
                        .document(null)
                        .type(NotificationType.SURVEY_UPDATED)
                        .resourceType("SURVEY")
                        .resourceId(surveyId)
                        .title("현황조사가 수정되었습니다")
                        .message(String.format("[%s] %s 현황조사가 %s님에 의해 수정되었습니다.",
                                organizationName, systemName, updatedBy.getName()))
                        .triggeredBy(updatedBy)
                        .build();
                notificationRepository.save(notification);
                sentCount++;
            }
        }

        log.info("현황조사 수정 알림 발송 완료: {} 명에게 알림 발송", sentCount);
    }

    // ================================
    // SR 알림
    // ================================

    /**
     * SR 생성 알림 발송 (담당자에게)
     */
    @Async
    @Transactional
    public void notifySrCreated(Long srId, String srTitle, User assignee, User createdBy) {
        if (assignee == null || assignee.getId().equals(createdBy.getId())) {
            log.info("SR 생성 알림 스킵: 담당자 미지정 또는 본인이 담당자");
            return;
        }

        log.info("SR 생성 알림 발송: srId={}, assignee={}", srId, assignee.getUsername());

        WikiNotification notification = WikiNotification.builder()
                .user(assignee)
                .document(null)
                .type(NotificationType.SR_CREATED)
                .resourceType("SR")
                .resourceId(srId)
                .title("새 SR이 접수되었습니다")
                .message(String.format("'%s' SR이 %s님에 의해 접수되어 귀하에게 배정되었습니다.",
                        srTitle, createdBy.getName()))
                .triggeredBy(createdBy)
                .build();
        notificationRepository.save(notification);

        log.info("SR 생성 알림 발송 완료");
    }

    /**
     * SR 수정 알림 발송 (담당자와 등록자에게)
     */
    @Async
    @Transactional
    public void notifySrUpdated(Long srId, String srTitle, User assignee, User registrant, User updatedBy) {
        log.info("SR 수정 알림 발송: srId={}, updatedBy={}", srId, updatedBy.getUsername());

        int sentCount = 0;

        // 담당자에게 알림 (수정자 제외)
        if (assignee != null && !assignee.getId().equals(updatedBy.getId())) {
            WikiNotification notification = WikiNotification.builder()
                    .user(assignee)
                    .document(null)
                    .type(NotificationType.SR_UPDATED)
                    .resourceType("SR")
                    .resourceId(srId)
                    .title("SR이 수정되었습니다")
                    .message(String.format("'%s' SR이 %s님에 의해 수정되었습니다.",
                            srTitle, updatedBy.getName()))
                    .triggeredBy(updatedBy)
                    .build();
            notificationRepository.save(notification);
            sentCount++;
        }

        // 등록자에게 알림 (수정자 제외, 담당자와 동일인 제외)
        if (registrant != null && !registrant.getId().equals(updatedBy.getId())
                && (assignee == null || !registrant.getId().equals(assignee.getId()))) {
            WikiNotification notification = WikiNotification.builder()
                    .user(registrant)
                    .document(null)
                    .type(NotificationType.SR_UPDATED)
                    .resourceType("SR")
                    .resourceId(srId)
                    .title("SR이 수정되었습니다")
                    .message(String.format("귀하가 등록한 '%s' SR이 %s님에 의해 수정되었습니다.",
                            srTitle, updatedBy.getName()))
                    .triggeredBy(updatedBy)
                    .build();
            notificationRepository.save(notification);
            sentCount++;
        }

        log.info("SR 수정 알림 발송 완료: {} 명에게 알림 발송", sentCount);
    }

    /**
     * SR 담당자 지정 알림 발송
     */
    @Async
    @Transactional
    public void notifySrAssigned(Long srId, String srTitle, User newAssignee, User assignedBy) {
        if (newAssignee == null || newAssignee.getId().equals(assignedBy.getId())) {
            log.info("SR 담당자 지정 알림 스킵: 본인이 담당자");
            return;
        }

        log.info("SR 담당자 지정 알림 발송: srId={}, newAssignee={}", srId, newAssignee.getUsername());

        WikiNotification notification = WikiNotification.builder()
                .user(newAssignee)
                .document(null)
                .type(NotificationType.SR_ASSIGNED)
                .resourceType("SR")
                .resourceId(srId)
                .title("SR이 귀하에게 배정되었습니다")
                .message(String.format("'%s' SR이 %s님에 의해 귀하에게 배정되었습니다.",
                        srTitle, assignedBy.getName()))
                .triggeredBy(assignedBy)
                .build();
        notificationRepository.save(notification);

        log.info("SR 담당자 지정 알림 발송 완료");
    }

    /**
     * SR 상태 변경 알림 발송 (담당자와 등록자에게)
     */
    @Async
    @Transactional
    public void notifySrStatusChanged(Long srId, String srTitle, String newStatus, User assignee, User registrant, User changedBy) {
        log.info("SR 상태 변경 알림 발송: srId={}, newStatus={}", srId, newStatus);

        int sentCount = 0;

        // 담당자에게 알림 (변경자 제외)
        if (assignee != null && !assignee.getId().equals(changedBy.getId())) {
            WikiNotification notification = WikiNotification.builder()
                    .user(assignee)
                    .document(null)
                    .type(NotificationType.SR_STATUS_CHANGED)
                    .resourceType("SR")
                    .resourceId(srId)
                    .title("SR 상태가 변경되었습니다")
                    .message(String.format("'%s' SR의 상태가 '%s'(으)로 변경되었습니다.",
                            srTitle, getStatusLabel(newStatus)))
                    .triggeredBy(changedBy)
                    .build();
            notificationRepository.save(notification);
            sentCount++;
        }

        // 등록자에게 알림 (변경자 제외, 담당자와 동일인 제외)
        if (registrant != null && !registrant.getId().equals(changedBy.getId())
                && (assignee == null || !registrant.getId().equals(assignee.getId()))) {
            WikiNotification notification = WikiNotification.builder()
                    .user(registrant)
                    .document(null)
                    .type(NotificationType.SR_STATUS_CHANGED)
                    .resourceType("SR")
                    .resourceId(srId)
                    .title("SR 상태가 변경되었습니다")
                    .message(String.format("귀하가 등록한 '%s' SR의 상태가 '%s'(으)로 변경되었습니다.",
                            srTitle, getStatusLabel(newStatus)))
                    .triggeredBy(changedBy)
                    .build();
            notificationRepository.save(notification);
            sentCount++;
        }

        log.info("SR 상태 변경 알림 발송 완료: {} 명에게 알림 발송", sentCount);
    }

    /**
     * SR 상태 코드를 한글 레이블로 변환
     */
    private String getStatusLabel(String status) {
        if (status == null) return "알 수 없음";

        switch (status) {
            case "OPEN":
                return "접수";
            case "IN_PROGRESS":
                return "처리중";
            case "RESOLVED":
                return "해결";
            case "CLOSED":
                return "완료";
            default:
                return status;
        }
    }
}
