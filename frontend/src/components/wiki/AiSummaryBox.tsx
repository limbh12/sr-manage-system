import React, { useState, useEffect, useRef, useCallback } from 'react';
import aiSearchService from '../../services/aiSearchService';
import { SummaryResponse } from '../../types/aiSearch';
import './AiSummaryBox.css';

interface AiSummaryBoxProps {
  documentId: number;
  aiSummary?: string;
  summaryGeneratedAt?: string;
  summaryUpToDate?: boolean;
}

const AiSummaryBox: React.FC<AiSummaryBoxProps> = ({
  documentId,
  aiSummary: initialSummary,
  summaryGeneratedAt: initialGeneratedAt,
  summaryUpToDate: initialUpToDate,
}) => {
  const [summary, setSummary] = useState<string | null>(initialSummary || null);
  const [generatedAt, setGeneratedAt] = useState<string | null>(initialGeneratedAt || null);
  const [isUpToDate, setIsUpToDate] = useState<boolean>(initialUpToDate ?? false);
  const [isGenerating, setIsGenerating] = useState(false);
  const [isExpanded, setIsExpanded] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const pollingIntervalRef = useRef<number | null>(null);
  const pollingCountRef = useRef(0);
  const MAX_POLLING_COUNT = 60; // 최대 60회 (2초 간격 = 2분)

  // 초기값이 변경되면 상태 업데이트
  useEffect(() => {
    setSummary(initialSummary || null);
    setGeneratedAt(initialGeneratedAt || null);
    setIsUpToDate(initialUpToDate ?? false);
  }, [initialSummary, initialGeneratedAt, initialUpToDate]);

  // 폴링 중지
  const stopPolling = useCallback(() => {
    if (pollingIntervalRef.current) {
      clearInterval(pollingIntervalRef.current);
      pollingIntervalRef.current = null;
    }
    pollingCountRef.current = 0;
  }, []);

  // 컴포넌트 언마운트 시 폴링 중지
  useEffect(() => {
    return () => stopPolling();
  }, [stopPolling]);

  // 상태 폴링
  const pollSummaryStatus = useCallback(async () => {
    try {
      pollingCountRef.current++;

      // 최대 폴링 횟수 초과 시 중지
      if (pollingCountRef.current > MAX_POLLING_COUNT) {
        stopPolling();
        setIsGenerating(false);
        setError('요약 생성 시간이 초과되었습니다. 다시 시도해주세요.');
        return;
      }

      const response: SummaryResponse = await aiSearchService.getSummaryStatus(documentId);

      if (response.status === 'GENERATING') {
        // 계속 폴링
        return;
      } else if (response.status === 'CACHED' || response.status === 'GENERATED') {
        // 완료
        stopPolling();
        setSummary(response.summary || null);
        setGeneratedAt(response.generatedAt || null);
        setIsUpToDate(true);
        setIsGenerating(false);
      } else if (response.status === 'FAILED') {
        // 실패
        stopPolling();
        setError(response.message || '요약 생성에 실패했습니다');
        setIsGenerating(false);
      } else if (response.status === 'NEEDS_UPDATE') {
        // 요약이 오래됨 - 폴링 중이면 계속 대기
        // (생성 요청은 됐지만 아직 시작 안 됐을 수 있음)
      }
    } catch (err) {
      console.error('요약 상태 폴링 오류:', err);
      // 오류 발생해도 폴링 계속 (일시적 네트워크 오류일 수 있음)
    }
  }, [documentId, stopPolling]);

  // 요약 생성 요청
  const handleGenerateSummary = async (forceRegenerate = false) => {
    setIsGenerating(true);
    setError(null);
    stopPolling();

    try {
      const response: SummaryResponse = await aiSearchService.generateSummary(documentId, forceRegenerate);

      if (response.status === 'FAILED') {
        setError(response.message || '요약 생성에 실패했습니다');
        setIsGenerating(false);
      } else if (response.status === 'CACHED') {
        // 캐시된 요약 반환
        setSummary(response.summary || null);
        setGeneratedAt(response.generatedAt || null);
        setIsUpToDate(true);
        setIsGenerating(false);
      } else if (response.status === 'GENERATING') {
        // 비동기 생성 시작됨 - 폴링 시작
        pollingCountRef.current = 0;
        pollingIntervalRef.current = window.setInterval(pollSummaryStatus, 2000);
      } else if (response.status === 'GENERATED') {
        // 동기적으로 완료됨 (보통 발생하지 않음)
        setSummary(response.summary || null);
        setGeneratedAt(response.generatedAt || null);
        setIsUpToDate(true);
        setIsGenerating(false);
      }
    } catch (err) {
      console.error('요약 생성 오류:', err);
      setError('요약 생성 중 오류가 발생했습니다');
      setIsGenerating(false);
    }
  };

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr);
    return date.toLocaleString('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <div className="ai-summary-box">
      <div className="ai-summary-header" onClick={() => setIsExpanded(!isExpanded)}>
        <div className="ai-summary-title">
          <span className="ai-icon">🤖</span>
          <span>AI 요약</span>
          {isGenerating && <span className="status-badge generating">생성 중...</span>}
          {!isGenerating && isUpToDate && <span className="status-badge up-to-date">최신</span>}
          {!isGenerating && !isUpToDate && summary && <span className="status-badge outdated">업데이트 필요</span>}
        </div>
        <div className="ai-summary-actions">
          {!isGenerating && (
            <button
              className="regenerate-btn"
              onClick={(e) => {
                e.stopPropagation();
                handleGenerateSummary(true);
              }}
              title={summary ? '요약 다시 생성' : '요약 생성'}
            >
              {summary ? '🔄' : '✨'}
            </button>
          )}
          <span className="expand-icon">{isExpanded ? '▼' : '▶'}</span>
        </div>
      </div>

      {isExpanded && (
        <div className="ai-summary-content">
          {isGenerating ? (
            <div className="loading-state">
              <div className="loading-spinner"></div>
              <span>AI가 문서를 분석하고 있습니다...</span>
            </div>
          ) : error ? (
            <div className="error-state">
              <span className="error-icon">⚠️</span>
              <span>{error}</span>
              <button className="retry-btn" onClick={() => handleGenerateSummary(true)}>
                다시 시도
              </button>
            </div>
          ) : summary ? (
            <>
              <p className="summary-text">{summary}</p>
              {generatedAt && (
                <div className="summary-meta">
                  <span>생성: {formatDate(generatedAt)}</span>
                </div>
              )}
            </>
          ) : (
            <div className="empty-state">
              <p>아직 AI 요약이 생성되지 않았습니다.</p>
              <button className="generate-btn" onClick={() => handleGenerateSummary(false)}>
                ✨ 요약 생성하기
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default AiSummaryBox;
