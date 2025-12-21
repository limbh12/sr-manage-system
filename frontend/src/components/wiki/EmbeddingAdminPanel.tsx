import React, { useState, useEffect, useRef } from 'react';
import { EmbeddingStats, BulkEmbeddingProgressEvent, ResourceType } from '../../types/aiSearch';
import aiSearchService from '../../services/aiSearchService';

interface EmbeddingAdminPanelProps {
  onClose: () => void;
}

interface ProgressState {
  wiki: BulkEmbeddingProgressEvent | null;
  sr: BulkEmbeddingProgressEvent | null;
  survey: BulkEmbeddingProgressEvent | null;
}

/**
 * 관리자용 임베딩 관리 패널
 * - 임베딩 통계 조회
 * - 일괄 임베딩 생성 (Wiki, SR, Survey) - 비동기 + 진행률 표시
 */
const EmbeddingAdminPanel: React.FC<EmbeddingAdminPanelProps> = ({ onClose }) => {
  const [stats, setStats] = useState<EmbeddingStats | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [progress, setProgress] = useState<ProgressState>({
    wiki: null,
    sr: null,
    survey: null,
  });
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // 구독 해제 함수 저장용 ref
  const unsubscribeRefs = useRef<{
    wiki?: () => void;
    sr?: () => void;
    survey?: () => void;
  }>({});

  // 임베딩 통계 로드
  const loadStats = async () => {
    setIsLoading(true);
    try {
      const data = await aiSearchService.getEmbeddingStats();
      setStats(data);
    } catch (err) {
      console.error('임베딩 통계 조회 실패:', err);
      setMessage({ type: 'error', text: '통계 조회에 실패했습니다.' });
    } finally {
      setIsLoading(false);
    }
  };

  // 컴포넌트 마운트 시 초기 진행 상태 확인
  const checkInitialProgress = async () => {
    const types: ResourceType[] = ['WIKI', 'SR', 'SURVEY'];
    for (const type of types) {
      const currentProgress = await aiSearchService.getBulkProgress(type);
      if (currentProgress && (currentProgress.status === 'STARTED' || currentProgress.status === 'IN_PROGRESS')) {
        // 진행 중인 작업이 있으면 구독 시작
        startSubscription(type);
      } else if (currentProgress) {
        // 완료된 상태도 표시
        setProgress(prev => ({
          ...prev,
          [type.toLowerCase()]: currentProgress,
        }));
      }
    }
  };

  useEffect(() => {
    loadStats();
    checkInitialProgress();

    // 컴포넌트 언마운트 시 모든 구독 해제
    return () => {
      Object.values(unsubscribeRefs.current).forEach(unsubscribe => unsubscribe?.());
    };
  }, []);

  // 진행률 구독 시작
  const startSubscription = (resourceType: ResourceType) => {
    const key = resourceType.toLowerCase() as 'wiki' | 'sr' | 'survey';

    // 기존 구독이 있으면 해제
    unsubscribeRefs.current[key]?.();

    const unsubscribe = aiSearchService.subscribeBulkProgress(
      resourceType,
      (event) => {
        setProgress(prev => ({
          ...prev,
          [key]: event,
        }));
      },
      (event) => {
        // 완료 시
        setProgress(prev => ({
          ...prev,
          [key]: event,
        }));
        setMessage({
          type: 'success',
          text: `${getResourceTypeName(resourceType)} 임베딩 생성 완료: 성공 ${event.successCount}건, 실패 ${event.failureCount}건`,
        });
        loadStats(); // 통계 새로고침
      },
      (error) => {
        // 에러 시
        setMessage({ type: 'error', text: error.message });
      }
    );

    unsubscribeRefs.current[key] = unsubscribe;
  };

  // 리소스 타입 한글명
  const getResourceTypeName = (type: ResourceType): string => {
    switch (type) {
      case 'WIKI': return 'Wiki 문서';
      case 'SR': return 'SR';
      case 'SURVEY': return '현황조사';
      default: return type;
    }
  };

  // 시간 포맷팅
  const formatTime = (ms?: number): string => {
    if (!ms || ms <= 0) return '-';
    if (ms < 1000) return `${ms}ms`;
    const seconds = Math.floor(ms / 1000);
    if (seconds < 60) return `${seconds}초`;
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${minutes}분 ${remainingSeconds}초`;
  };

  // 임베딩 생성 시작
  const handleGenerate = async (resourceType: ResourceType) => {
    const key = resourceType.toLowerCase() as 'wiki' | 'sr' | 'survey';
    const currentProgress = progress[key];

    // 이미 진행 중이면 무시
    if (currentProgress && (currentProgress.status === 'STARTED' || currentProgress.status === 'IN_PROGRESS')) {
      return;
    }

    if (!confirm(`전체 ${getResourceTypeName(resourceType)} 임베딩을 생성하시겠습니까?\n시간이 오래 걸릴 수 있습니다.`)) {
      return;
    }

    setMessage(null);

    try {
      let response;
      switch (resourceType) {
        case 'WIKI':
          response = await aiSearchService.generateAllWikiEmbeddings();
          break;
        case 'SR':
          response = await aiSearchService.generateAllSrEmbeddings();
          break;
        case 'SURVEY':
          response = await aiSearchService.generateAllSurveyEmbeddings();
          break;
      }

      if (response.status === 'IN_PROGRESS') {
        setMessage({ type: 'error', text: response.message });
        return;
      }

      // 진행률 구독 시작
      startSubscription(resourceType);
    } catch (err: any) {
      console.error(`${resourceType} 임베딩 생성 시작 실패:`, err);
      setMessage({ type: 'error', text: err.response?.data?.message || `${getResourceTypeName(resourceType)} 임베딩 생성 시작 실패` });
    }
  };

  // 진행률 표시 컴포넌트
  const ProgressDisplay: React.FC<{ resourceType: ResourceType; progressData: BulkEmbeddingProgressEvent | null }> = ({
    resourceType,
    progressData,
  }) => {
    if (!progressData) return null;

    const isActive = progressData.status === 'STARTED' || progressData.status === 'IN_PROGRESS';
    const isCompleted = progressData.status === 'COMPLETED';
    const isFailed = progressData.status === 'FAILED';

    return (
      <div style={{
        ...styles.progressContainer,
        backgroundColor: isCompleted ? '#d4edda' : isFailed ? '#f8d7da' : '#e3f2fd',
        borderColor: isCompleted ? '#28a745' : isFailed ? '#dc3545' : '#2196f3',
      }}>
        <div style={styles.progressHeader}>
          <span style={styles.progressTitle}>
            {isCompleted ? '✅' : isFailed ? '❌' : '⏳'} {getResourceTypeName(resourceType)}
          </span>
          <span style={{
            ...styles.progressBadge,
            backgroundColor: isCompleted ? '#28a745' : isFailed ? '#dc3545' : '#2196f3',
          }}>
            {progressData.progressPercent}%
          </span>
        </div>

        {/* 진행률 바 */}
        <div style={styles.progressBarContainer}>
          <div style={{
            ...styles.progressBar,
            width: `${progressData.progressPercent}%`,
            backgroundColor: isCompleted ? '#28a745' : isFailed ? '#dc3545' : '#2196f3',
          }} />
        </div>

        {/* 상세 정보 */}
        <div style={styles.progressDetails}>
          <span>{progressData.currentIndex} / {progressData.totalCount} 건</span>
          <span>성공: {progressData.successCount} | 실패: {progressData.failureCount}</span>
        </div>

        {/* 현재 처리 중인 항목 */}
        {isActive && progressData.currentTitle && (
          <div style={styles.currentItem}>
            처리 중: {progressData.currentTitle.length > 40
              ? progressData.currentTitle.substring(0, 40) + '...'
              : progressData.currentTitle}
          </div>
        )}

        {/* 시간 정보 */}
        {isActive && (
          <div style={styles.timeInfo}>
            <span>경과: {formatTime(progressData.elapsedTimeMs)}</span>
            <span>예상 남은 시간: {formatTime(progressData.estimatedRemainingMs)}</span>
          </div>
        )}

        {/* 완료 메시지 */}
        {isCompleted && (
          <div style={styles.completeMessage}>
            총 소요시간: {formatTime(progressData.elapsedTimeMs)}
          </div>
        )}
      </div>
    );
  };

  // 버튼 상태 확인
  const isGenerating = (resourceType: ResourceType): boolean => {
    const key = resourceType.toLowerCase() as 'wiki' | 'sr' | 'survey';
    const p = progress[key];
    return p !== null && (p.status === 'STARTED' || p.status === 'IN_PROGRESS');
  };

  const anyGenerating = isGenerating('WIKI') || isGenerating('SR') || isGenerating('SURVEY');

  return (
    <div style={styles.overlay} onClick={onClose}>
      <div style={styles.panel} onClick={(e) => e.stopPropagation()}>
        <div style={styles.header}>
          <h3 style={styles.title}>AI 임베딩 관리</h3>
          <button style={styles.closeBtn} onClick={onClose}>✕</button>
        </div>

        {/* 메시지 */}
        {message && (
          <div style={{
            ...styles.message,
            backgroundColor: message.type === 'success' ? '#d4edda' : '#f8d7da',
            color: message.type === 'success' ? '#155724' : '#721c24',
            borderColor: message.type === 'success' ? '#c3e6cb' : '#f5c6cb',
          }}>
            {message.text}
          </div>
        )}

        {/* 통계 */}
        <div style={styles.section}>
          <h4 style={styles.sectionTitle}>임베딩 현황</h4>
          {isLoading ? (
            <div style={styles.loading}>로딩 중...</div>
          ) : stats ? (
            <div style={styles.statsGrid}>
              <div style={styles.statCard}>
                <span style={styles.statIcon}>📄</span>
                <div style={styles.statInfo}>
                  <span style={styles.statLabel}>Wiki 문서</span>
                  <span style={styles.statValue}>{stats.wiki}건</span>
                </div>
              </div>
              <div style={styles.statCard}>
                <span style={styles.statIcon}>📋</span>
                <div style={styles.statInfo}>
                  <span style={styles.statLabel}>SR</span>
                  <span style={styles.statValue}>{stats.sr}건</span>
                </div>
              </div>
              <div style={styles.statCard}>
                <span style={styles.statIcon}>📊</span>
                <div style={styles.statInfo}>
                  <span style={styles.statLabel}>현황조사</span>
                  <span style={styles.statValue}>{stats.survey}건</span>
                </div>
              </div>
              <div style={{...styles.statCard, ...styles.totalCard}}>
                <span style={styles.statIcon}>📦</span>
                <div style={styles.statInfo}>
                  <span style={styles.statLabel}>전체</span>
                  <span style={styles.statValue}>{stats.total}건</span>
                </div>
              </div>
            </div>
          ) : (
            <div style={styles.error}>통계를 불러올 수 없습니다.</div>
          )}
          <button style={styles.refreshBtn} onClick={loadStats} disabled={isLoading}>
            🔄 새로고침
          </button>
        </div>

        {/* 일괄 생성 */}
        <div style={styles.section}>
          <h4 style={styles.sectionTitle}>일괄 임베딩 생성</h4>
          <p style={styles.sectionDesc}>
            각 리소스의 전체 데이터에 대해 임베딩을 생성합니다.<br />
            데이터 양에 따라 시간이 오래 걸릴 수 있습니다.
          </p>

          {/* 진행률 표시 */}
          <ProgressDisplay resourceType="WIKI" progressData={progress.wiki} />
          <ProgressDisplay resourceType="SR" progressData={progress.sr} />
          <ProgressDisplay resourceType="SURVEY" progressData={progress.survey} />

          <div style={styles.buttonGroup}>
            <button
              style={{
                ...styles.generateBtn,
                ...styles.wikiBtn,
                ...(anyGenerating ? styles.disabledBtn : {}),
              }}
              onClick={() => handleGenerate('WIKI')}
              disabled={anyGenerating}
            >
              {isGenerating('WIKI') ? '⏳ 생성 중...' : '📄 Wiki 전체 임베딩'}
            </button>
            <button
              style={{
                ...styles.generateBtn,
                ...styles.srBtn,
                ...(anyGenerating ? styles.disabledBtn : {}),
              }}
              onClick={() => handleGenerate('SR')}
              disabled={anyGenerating}
            >
              {isGenerating('SR') ? '⏳ 생성 중...' : '📋 SR 전체 임베딩'}
            </button>
            <button
              style={{
                ...styles.generateBtn,
                ...styles.surveyBtn,
                ...(anyGenerating ? styles.disabledBtn : {}),
              }}
              onClick={() => handleGenerate('SURVEY')}
              disabled={anyGenerating}
            >
              {isGenerating('SURVEY') ? '⏳ 생성 중...' : '📊 현황조사 전체 임베딩'}
            </button>
          </div>
        </div>

        <div style={styles.footer}>
          <p style={styles.footerText}>
            임베딩은 AI 검색의 정확도를 높이기 위해 텍스트를 벡터로 변환하는 과정입니다.
          </p>
        </div>
      </div>
    </div>
  );
};

const styles: { [key: string]: React.CSSProperties } = {
  overlay: {
    position: 'fixed',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    zIndex: 1000,
  },
  panel: {
    backgroundColor: 'var(--bg-primary, white)',
    borderRadius: '12px',
    width: '90%',
    maxWidth: '550px',
    maxHeight: '90vh',
    overflow: 'auto',
    boxShadow: '0 4px 20px rgba(0, 0, 0, 0.15)',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '16px 20px',
    borderBottom: '1px solid var(--border-color, #e0e0e0)',
  },
  title: {
    margin: 0,
    fontSize: '18px',
    fontWeight: '600',
    color: 'var(--text-primary, #333)',
  },
  closeBtn: {
    background: 'none',
    border: 'none',
    fontSize: '18px',
    cursor: 'pointer',
    color: 'var(--text-secondary, #666)',
    padding: '4px 8px',
  },
  message: {
    margin: '16px 20px 0',
    padding: '12px',
    borderRadius: '6px',
    border: '1px solid',
    fontSize: '14px',
  },
  section: {
    padding: '20px',
    borderBottom: '1px solid var(--border-color, #e0e0e0)',
  },
  sectionTitle: {
    margin: '0 0 12px 0',
    fontSize: '15px',
    fontWeight: '600',
    color: 'var(--text-primary, #333)',
  },
  sectionDesc: {
    margin: '0 0 16px 0',
    fontSize: '13px',
    color: 'var(--text-secondary, #666)',
    lineHeight: '1.5',
  },
  loading: {
    textAlign: 'center',
    padding: '20px',
    color: 'var(--text-secondary, #666)',
  },
  error: {
    textAlign: 'center',
    padding: '20px',
    color: '#dc3545',
  },
  statsGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(2, 1fr)',
    gap: '12px',
    marginBottom: '12px',
  },
  statCard: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    padding: '12px',
    backgroundColor: 'var(--bg-secondary, #f8f9fa)',
    borderRadius: '8px',
    border: '1px solid var(--border-color, #e0e0e0)',
  },
  totalCard: {
    gridColumn: 'span 2',
    backgroundColor: '#e3f2fd',
    borderColor: '#90caf9',
  },
  statIcon: {
    fontSize: '24px',
  },
  statInfo: {
    display: 'flex',
    flexDirection: 'column',
    gap: '2px',
  },
  statLabel: {
    fontSize: '12px',
    color: 'var(--text-secondary, #666)',
  },
  statValue: {
    fontSize: '16px',
    fontWeight: '600',
    color: 'var(--text-primary, #333)',
  },
  refreshBtn: {
    display: 'block',
    width: '100%',
    padding: '8px',
    backgroundColor: 'transparent',
    border: '1px solid var(--border-color, #e0e0e0)',
    borderRadius: '6px',
    fontSize: '13px',
    cursor: 'pointer',
    color: 'var(--text-primary, #333)',
  },
  progressContainer: {
    padding: '14px',
    borderRadius: '8px',
    border: '1px solid',
    marginBottom: '12px',
  },
  progressHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '10px',
  },
  progressTitle: {
    fontSize: '14px',
    fontWeight: '600',
    color: '#333',
  },
  progressBadge: {
    padding: '3px 10px',
    borderRadius: '12px',
    fontSize: '12px',
    fontWeight: '700',
    color: 'white',
  },
  progressBarContainer: {
    width: '100%',
    height: '8px',
    backgroundColor: '#e0e0e0',
    borderRadius: '4px',
    overflow: 'hidden',
    marginBottom: '10px',
  },
  progressBar: {
    height: '100%',
    transition: 'width 0.3s ease',
  },
  progressDetails: {
    display: 'flex',
    justifyContent: 'space-between',
    fontSize: '12px',
    color: '#666',
    marginBottom: '6px',
  },
  currentItem: {
    fontSize: '12px',
    color: '#1565c0',
    backgroundColor: '#e3f2fd',
    padding: '6px 10px',
    borderRadius: '4px',
    marginBottom: '6px',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  timeInfo: {
    display: 'flex',
    justifyContent: 'space-between',
    fontSize: '11px',
    color: '#888',
  },
  completeMessage: {
    fontSize: '12px',
    color: '#155724',
    fontWeight: '500',
    textAlign: 'center',
    marginTop: '4px',
  },
  buttonGroup: {
    display: 'flex',
    flexDirection: 'column',
    gap: '10px',
  },
  generateBtn: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: '8px',
    padding: '12px 16px',
    borderRadius: '8px',
    border: 'none',
    fontSize: '14px',
    fontWeight: '600',
    cursor: 'pointer',
    color: 'white',
    transition: 'opacity 0.2s, transform 0.1s',
  },
  wikiBtn: {
    backgroundColor: '#2e7d32',
  },
  srBtn: {
    backgroundColor: '#1565c0',
  },
  surveyBtn: {
    backgroundColor: '#ef6c00',
  },
  disabledBtn: {
    opacity: 0.6,
    cursor: 'not-allowed',
  },
  footer: {
    padding: '16px 20px',
  },
  footerText: {
    margin: 0,
    fontSize: '12px',
    color: 'var(--text-secondary, #666)',
    textAlign: 'center',
  },
};

export default EmbeddingAdminPanel;
