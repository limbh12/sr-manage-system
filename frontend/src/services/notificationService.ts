import api from './api';
import { Notification, NotificationCountResponse } from '../types/notification';
import { WikiPageResponse } from '../types/wiki';

/**
 * 알림 서비스
 */
class NotificationService {
  /**
   * 알림 목록 조회
   */
  async getNotifications(page = 0, size = 20): Promise<WikiPageResponse<Notification>> {
    const response = await api.get<WikiPageResponse<Notification>>(
      `/wiki/notifications?page=${page}&size=${size}`
    );
    return response.data;
  }

  /**
   * 읽지 않은 알림 목록 조회
   */
  async getUnreadNotifications(): Promise<Notification[]> {
    const response = await api.get<Notification[]>('/wiki/notifications/unread');
    return response.data;
  }

  /**
   * 읽지 않은 알림 개수 조회
   */
  async getUnreadCount(): Promise<number> {
    const response = await api.get<NotificationCountResponse>('/wiki/notifications/unread/count');
    return response.data.count;
  }

  /**
   * 알림 통계 조회 (전체, 읽지않음, 읽음 개수)
   */
  async getNotificationStats(): Promise<{ total: number; unread: number; read: number }> {
    const response = await api.get<{ total: number; unread: number; read: number }>(
      '/wiki/notifications/stats'
    );
    return response.data;
  }

  /**
   * 알림 읽음 처리
   */
  async markAsRead(notificationId: number): Promise<void> {
    await api.post(`/wiki/notifications/${notificationId}/read`);
  }

  /**
   * 모든 알림 읽음 처리
   */
  async markAllAsRead(): Promise<void> {
    await api.post('/wiki/notifications/read-all');
  }

  /**
   * 알림 삭제
   */
  async deleteNotification(notificationId: number): Promise<void> {
    await api.delete(`/wiki/notifications/${notificationId}`);
  }
}

export default new NotificationService();
