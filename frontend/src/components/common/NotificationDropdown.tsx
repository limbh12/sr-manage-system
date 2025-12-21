import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import notificationService from '../../services/notificationService';
import { Notification } from '../../types/notification';
import './NotificationDropdown.css';

const NotificationDropdown: React.FC = () => {
  const navigate = useNavigate();
  const [isOpen, setIsOpen] = useState(false);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // 읽지 않은 알림 개수 주기적 조회
  useEffect(() => {
    loadUnreadCount();
    const interval = setInterval(loadUnreadCount, 30000); // 30초마다
    return () => clearInterval(interval);
  }, []);

  // 드롭다운 외부 클릭 시 닫기
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const loadUnreadCount = async () => {
    try {
      const count = await notificationService.getUnreadCount();
      setUnreadCount(count);
    } catch (error) {
      console.error('알림 개수 조회 실패:', error);
    }
  };

  const loadNotifications = async () => {
    setLoading(true);
    try {
      const unread = await notificationService.getUnreadNotifications();
      setNotifications(unread);
    } catch (error) {
      console.error('알림 목록 조회 실패:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleToggle = () => {
    if (!isOpen) {
      loadNotifications();
    }
    setIsOpen(!isOpen);
  };

  const handleNotificationClick = async (notification: Notification) => {
    // 읽음 처리
    if (!notification.isRead) {
      try {
        await notificationService.markAsRead(notification.id);
        setUnreadCount((prev) => Math.max(0, prev - 1));
        setNotifications((prev) =>
          prev.map((n) => (n.id === notification.id ? { ...n, isRead: true } : n))
        );
      } catch (error) {
        console.error('알림 읽음 처리 실패:', error);
      }
    }

    // 리소스 유형에 따라 이동
    if (notification.resourceType === 'SURVEY' && notification.resourceId) {
      navigate(`/survey/${notification.resourceId}`);
      setIsOpen(false);
    } else if (notification.resourceType === 'SR' && notification.resourceId) {
      navigate(`/sr?id=${notification.resourceId}`);
      setIsOpen(false);
    } else if (notification.documentId) {
      // Wiki 문서로 이동
      navigate(`/wiki/${notification.documentId}`);
      setIsOpen(false);
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await notificationService.markAllAsRead();
      setUnreadCount(0);
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
    } catch (error) {
      console.error('모든 알림 읽음 처리 실패:', error);
    }
  };

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr);
    const now = new Date();
    const diff = now.getTime() - date.getTime();

    if (diff < 60000) return '방금 전';
    if (diff < 3600000) return `${Math.floor(diff / 60000)}분 전`;
    if (diff < 86400000) return `${Math.floor(diff / 3600000)}시간 전`;
    if (diff < 604800000) return `${Math.floor(diff / 86400000)}일 전`;

    return date.toLocaleDateString('ko-KR');
  };

  const getNotificationIcon = (type: string) => {
    switch (type) {
      // Wiki 알림
      case 'DOCUMENT_CREATED':
        return '📄';
      case 'DOCUMENT_UPDATED':
        return '✏️';
      case 'DOCUMENT_DELETED':
        return '🗑️';
      case 'MENTIONED':
        return '📢';
      // OPEN API 현황조사 알림
      case 'SURVEY_CREATED':
        return '📋';
      case 'SURVEY_UPDATED':
        return '📝';
      // SR 알림
      case 'SR_CREATED':
        return '🎫';
      case 'SR_UPDATED':
        return '🔄';
      case 'SR_ASSIGNED':
        return '👤';
      case 'SR_STATUS_CHANGED':
        return '📊';
      default:
        return '🔔';
    }
  };

  return (
    <div className="notification-dropdown" ref={dropdownRef}>
      <button className="notification-trigger" onClick={handleToggle}>
        <span className="notification-icon">🔔</span>
        {unreadCount > 0 && <span className="notification-badge">{unreadCount > 99 ? '99+' : unreadCount}</span>}
      </button>

      {isOpen && (
        <div className="notification-menu">
          <div className="notification-header">
            <h4>알림</h4>
            {unreadCount > 0 && (
              <button className="mark-all-read-btn" onClick={handleMarkAllAsRead}>
                모두 읽음
              </button>
            )}
          </div>

          <div className="notification-list">
            {loading ? (
              <div className="notification-loading">로딩 중...</div>
            ) : notifications.length === 0 ? (
              <div className="notification-empty">새 알림이 없습니다</div>
            ) : (
              notifications.map((notification) => (
                <div
                  key={notification.id}
                  className={`notification-item ${!notification.isRead ? 'unread' : ''}`}
                  onClick={() => handleNotificationClick(notification)}
                >
                  <span className="notification-item-icon">
                    {getNotificationIcon(notification.type)}
                  </span>
                  <div className="notification-item-content">
                    <div className="notification-item-title">{notification.title}</div>
                    {notification.message && (
                      <div className="notification-item-message">{notification.message}</div>
                    )}
                    <div className="notification-item-time">{formatDate(notification.createdAt)}</div>
                  </div>
                </div>
              ))
            )}
          </div>

          <div className="notification-footer">
            <button onClick={() => { navigate('/notifications'); setIsOpen(false); }}>
              모든 알림 보기
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default NotificationDropdown;
