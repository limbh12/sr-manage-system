package com.srmanagement.wiki.dto;

import com.srmanagement.wiki.entity.WikiNotification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 알림 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private Long id;
    private String type;
    private String title;
    private String message;
    private Long documentId;
    private String documentTitle;
    private String resourceType;  // WIKI, SURVEY, SR
    private Long resourceId;      // Survey ID 또는 SR ID
    private Long triggeredById;
    private String triggeredByName;
    private Boolean isRead;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;

    public static NotificationResponse fromEntity(WikiNotification notification) {
        NotificationResponseBuilder builder = NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType().name())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .resourceType(notification.getResourceType())
                .resourceId(notification.getResourceId())
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt());

        if (notification.getDocument() != null) {
            builder.documentId(notification.getDocument().getId())
                   .documentTitle(notification.getDocument().getTitle());
        }

        if (notification.getTriggeredBy() != null) {
            builder.triggeredById(notification.getTriggeredBy().getId())
                   .triggeredByName(notification.getTriggeredBy().getName());
        }

        return builder.build();
    }
}
