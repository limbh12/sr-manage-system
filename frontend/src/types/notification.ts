/**
 * 알림 유형
 */
export type NotificationType =
  | 'DOCUMENT_CREATED'
  | 'DOCUMENT_UPDATED'
  | 'DOCUMENT_DELETED'
  | 'CATEGORY_CREATED'
  | 'CATEGORY_UPDATED'
  | 'MENTIONED'
  // OPEN API 현황조사 알림
  | 'SURVEY_CREATED'
  | 'SURVEY_UPDATED'
  // SR 알림
  | 'SR_CREATED'
  | 'SR_UPDATED'
  | 'SR_ASSIGNED'
  | 'SR_STATUS_CHANGED';

/**
 * 리소스 유형
 */
export type ResourceType = 'WIKI' | 'SURVEY' | 'SR';

/**
 * 알림 정보
 */
export interface Notification {
  id: number;
  type: NotificationType;
  title: string;
  message?: string;
  documentId?: number;
  documentTitle?: string;
  resourceType?: ResourceType;
  resourceId?: number;
  triggeredById?: number;
  triggeredByName?: string;
  isRead: boolean;
  readAt?: string;
  createdAt: string;
}

/**
 * 알림 개수 응답
 */
export interface NotificationCountResponse {
  count: number;
}
