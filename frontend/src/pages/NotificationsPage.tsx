import { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import notificationService from '../services/notificationService';
import { Notification, NotificationType, ResourceType } from '../types/notification';
import { WikiPageResponse } from '../types/wiki';
import './NotificationsPage.css';

interface NotificationStats {
  total: number;
  unread: number;
  read: number;
}

type FilterType = 'all' | 'unread' | 'read';

/**
 * 모든 알림 보기 페이지
 */
function NotificationsPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  // URL에서 필터 상태 읽기
  const getInitialFilter = (): FilterType => {
    const filterParam = searchParams.get('filter');
    if (filterParam === 'unread' || filterParam === 'read') {
      return filterParam;
    }
    return 'all';
  };

  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [filter, setFilter] = useState<FilterType>(getInitialFilter());
  const [stats, setStats] = useState<NotificationStats>({ total: 0, unread: 0, read: 0 });
  const [totalElements, setTotalElements] = useState(0);
  const observerRef = useRef<IntersectionObserver | null>(null);
  const loadMoreRef = useRef<HTMLDivElement | null>(null);

  // 통계 조회
  const loadStats = useCallback(async () => {
    try {
      const statsData = await notificationService.getNotificationStats();
      setStats(statsData);
    } catch (error) {
      console.error('알림 통계 조회 실패:', error);
    }
  }, []);

  // 알림 목록 조회
  const loadNotifications = useCallback(async (pageNum: number, append: boolean = false) => {
    if (append) {
      setLoadingMore(true);
    } else {
      setLoading(true);
    }

    try {
      const response: WikiPageResponse<Notification> = await notificationService.getNotifications(pageNum, 20);

      if (append) {
        setNotifications((prev) => [...prev, ...response.content]);
      } else {
        setNotifications(response.content);
      }

      setTotalElements(response.totalElements);
      setHasMore(!response.last);
      setPage(pageNum);
    } catch (error) {
      console.error('알림 목록 조회 실패:', error);
    } finally {
      setLoading(false);
      setLoadingMore(false);
    }
  }, []);

  // 초기 로드
  useEffect(() => {
    loadStats();
    loadNotifications(0, false);
  }, [loadStats, loadNotifications]);

  // Intersection Observer 설정 (인피니트 스크롤)
  useEffect(() => {
    if (loading || loadingMore || !hasMore) return;

    observerRef.current = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && hasMore && !loadingMore) {
          loadNotifications(page + 1, true);
        }
      },
      { threshold: 0.1 }
    );

    if (loadMoreRef.current) {
      observerRef.current.observe(loadMoreRef.current);
    }

    return () => {
      if (observerRef.current) {
        observerRef.current.disconnect();
      }
    };
  }, [loading, loadingMore, hasMore, page, loadNotifications]);

  const handleNotificationClick = async (notification: Notification) => {
    // 읽음 처리
    if (!notification.isRead) {
      try {
        await notificationService.markAsRead(notification.id);
        setNotifications((prev) =>
          prev.map((n) => (n.id === notification.id ? { ...n, isRead: true } : n))
        );
        setStats((prev) => ({
          ...prev,
          unread: Math.max(0, prev.unread - 1),
          read: prev.read + 1
        }));
      } catch (error) {
        console.error('알림 읽음 처리 실패:', error);
      }
    }

    // 리소스 유형에 따라 이동
    if (notification.resourceType === 'SURVEY' && notification.resourceId) {
      navigate(`/survey/${notification.resourceId}`);
    } else if (notification.resourceType === 'SR' && notification.resourceId) {
      navigate(`/sr?id=${notification.resourceId}`);
    } else if (notification.documentId) {
      navigate(`/wiki/${notification.documentId}`);
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await notificationService.markAllAsRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
      setStats((prev) => ({
        ...prev,
        unread: 0,
        read: prev.total
      }));
    } catch (error) {
      console.error('모든 알림 읽음 처리 실패:', error);
    }
  };

  const handleDelete = async (notificationId: number, e: React.MouseEvent) => {
    e.stopPropagation();
    if (!confirm('이 알림을 삭제하시겠습니까?')) return;

    try {
      const deletedNotification = notifications.find((n) => n.id === notificationId);
      await notificationService.deleteNotification(notificationId);
      setNotifications((prev) => prev.filter((n) => n.id !== notificationId));

      // 통계 업데이트
      if (deletedNotification) {
        setStats((prev) => ({
          total: prev.total - 1,
          unread: deletedNotification.isRead ? prev.unread : prev.unread - 1,
          read: deletedNotification.isRead ? prev.read - 1 : prev.read
        }));
        setTotalElements((prev) => prev - 1);
      }
    } catch (error) {
      console.error('알림 삭제 실패:', error);
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

    return date.toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getNotificationIcon = (type: NotificationType) => {
    switch (type) {
      case 'DOCUMENT_CREATED':
        return '📄';
      case 'DOCUMENT_UPDATED':
        return '✏️';
      case 'DOCUMENT_DELETED':
        return '🗑️';
      case 'MENTIONED':
        return '📢';
      case 'SURVEY_CREATED':
        return '📋';
      case 'SURVEY_UPDATED':
        return '📝';
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

  const getResourceTypeBadge = (resourceType?: ResourceType) => {
    switch (resourceType) {
      case 'WIKI':
        return <span className="badge badge-wiki">Wiki</span>;
      case 'SURVEY':
        return <span className="badge badge-survey">현황조사</span>;
      case 'SR':
        return <span className="badge badge-sr">SR</span>;
      default:
        return null;
    }
  };

  // 필터 변경 핸들러 (URL 파라미터도 함께 업데이트)
  const handleFilterChange = (newFilter: FilterType) => {
    setFilter(newFilter);
    if (newFilter === 'all') {
      setSearchParams({});
    } else {
      setSearchParams({ filter: newFilter });
    }
  };

  // 필터링된 알림 목록
  const filteredNotifications = notifications.filter((n) => {
    if (filter === 'unread') return !n.isRead;
    if (filter === 'read') return n.isRead;
    return true;
  });

  return (
    <div className="notifications-page">
      <div className="page-header">
        <h1 className="page-title">알림</h1>
        <div className="page-actions">
          {stats.unread > 0 && (
            <button className="btn btn-secondary" onClick={handleMarkAllAsRead}>
              모두 읽음 처리
            </button>
          )}
        </div>
      </div>

      <div className="notifications-filter">
        <button
          className={`filter-btn ${filter === 'all' ? 'active' : ''}`}
          onClick={() => handleFilterChange('all')}
        >
          전체 ({stats.total})
        </button>
        <button
          className={`filter-btn ${filter === 'unread' ? 'active' : ''}`}
          onClick={() => handleFilterChange('unread')}
        >
          읽지 않음 ({stats.unread})
        </button>
        <button
          className={`filter-btn ${filter === 'read' ? 'active' : ''}`}
          onClick={() => handleFilterChange('read')}
        >
          읽음 ({stats.read})
        </button>
      </div>

      <div className="notifications-container">
        {loading ? (
          <div className="notifications-loading">로딩 중...</div>
        ) : filteredNotifications.length === 0 ? (
          <div className="notifications-empty">
            {filter === 'unread' ? '읽지 않은 알림이 없습니다.' : '알림이 없습니다.'}
          </div>
        ) : (
          <div className="notifications-list">
            {filteredNotifications.map((notification) => (
              <div
                key={notification.id}
                className={`notification-card ${!notification.isRead ? 'unread' : ''}`}
                onClick={() => handleNotificationClick(notification)}
              >
                <div className="notification-index">
                  {totalElements - (notifications.findIndex((n) => n.id === notification.id))}
                </div>
                <div className="notification-icon">
                  {getNotificationIcon(notification.type)}
                </div>
                <div className="notification-content">
                  <div className="notification-header">
                    <span className="notification-title">{notification.title}</span>
                    {getResourceTypeBadge(notification.resourceType)}
                  </div>
                  {notification.message && (
                    <p className="notification-message">{notification.message}</p>
                  )}
                  <div className="notification-meta">
                    <span className="notification-time">{formatDate(notification.createdAt)}</span>
                    {notification.triggeredByName && (
                      <span className="notification-author">by {notification.triggeredByName}</span>
                    )}
                  </div>
                </div>
                <div className="notification-actions">
                  <button
                    className="btn-icon delete-btn"
                    onClick={(e) => handleDelete(notification.id, e)}
                    title="삭제"
                  >
                    ×
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* 인피니트 스크롤 로딩 트리거 */}
        {hasMore && !loading && (
          <div ref={loadMoreRef} className="load-more-trigger">
            {loadingMore && <div className="notifications-loading">더 불러오는 중...</div>}
          </div>
        )}

        {/* 모든 알림 로드 완료 메시지 */}
        {!hasMore && notifications.length > 0 && (
          <div className="notifications-end">
            모든 알림을 불러왔습니다. (총 {totalElements}개)
          </div>
        )}
      </div>
    </div>
  );
}

export default NotificationsPage;
