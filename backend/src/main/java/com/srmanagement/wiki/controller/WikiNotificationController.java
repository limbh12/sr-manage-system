package com.srmanagement.wiki.controller;

import com.srmanagement.entity.User;
import com.srmanagement.exception.CustomException;
import com.srmanagement.repository.UserRepository;
import com.srmanagement.wiki.dto.NotificationResponse;
import com.srmanagement.wiki.service.WikiNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 위키 알림 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/wiki/notifications")
@RequiredArgsConstructor
public class WikiNotificationController {

    private final WikiNotificationService notificationService;
    private final UserRepository userRepository;

    /**
     * 알림 목록 조회
     */
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        User user = getAuthenticatedUser(authentication);
        Page<NotificationResponse> notifications = notificationService.getNotifications(user.getId(), pageable);
        return ResponseEntity.ok(notifications);
    }

    /**
     * 읽지 않은 알림 목록 조회
     */
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        List<NotificationResponse> notifications = notificationService.getUnreadNotifications(user.getId());
        return ResponseEntity.ok(notifications);
    }

    /**
     * 읽지 않은 알림 개수 조회
     */
    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        long count = notificationService.getUnreadCount(user.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * 알림 통계 조회 (전체, 읽지않음, 읽음 개수)
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getNotificationStats(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        Map<String, Long> stats = notificationService.getNotificationStats(user.getId());
        return ResponseEntity.ok(stats);
    }

    /**
     * 알림 읽음 처리
     */
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        notificationService.markAsRead(id, user.getId());
        return ResponseEntity.ok().build();
    }

    /**
     * 모든 알림 읽음 처리
     */
    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok().build();
    }

    /**
     * 알림 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable Long id,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        notificationService.deleteNotification(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    private User getAuthenticatedUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));
    }
}
