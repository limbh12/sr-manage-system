import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeHighlight from 'rehype-highlight';
import rehypeRaw from 'rehype-raw';
import { AiSearchRequest, AiSearchResponse, SourceDocument, ResourceType, AiSearchHistoryResponse } from '../../types/aiSearch';
import aiSearchService from '../../services/aiSearchService';
import EmbeddingAdminPanel from './EmbeddingAdminPanel';

interface AiSearchBoxProps {
  onDocumentClick?: (documentId: number) => void;
  onSrClick?: (srId: number) => void;
  onSurveyClick?: (surveyId: number) => void;
  isAdmin?: boolean;
}

/**
 * AI 검색 박스 컴포넌트
 * - RAG 기반 자연어 검색 (Wiki, SR, Survey 통합)
 * - 참고 문서 링크 표시
 */
const AiSearchBox: React.FC<AiSearchBoxProps> = ({ onDocumentClick, onSrClick, onSurveyClick, isAdmin = false }) => {
  const navigate = useNavigate();
  const [question, setQuestion] = useState('');
  const [isSearching, setIsSearching] = useState(false);
  const [result, setResult] = useState<AiSearchResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selectedTypes, setSelectedTypes] = useState<ResourceType[]>(['WIKI', 'SR', 'SURVEY']);
  const [recentHistory, setRecentHistory] = useState<AiSearchHistoryResponse[]>([]);
  const [showHistory, setShowHistory] = useState(false);
  const [isLoadingHistory, setIsLoadingHistory] = useState(false);
  const [showAdminPanel, setShowAdminPanel] = useState(false);

  // 최근 검색 이력 로드
  const loadRecentHistory = async () => {
    setIsLoadingHistory(true);
    try {
      const histories = await aiSearchService.getRecentHistory(5);
      setRecentHistory(histories);
    } catch (err) {
      console.error('검색 이력 로드 실패:', err);
    } finally {
      setIsLoadingHistory(false);
    }
  };

  // 컴포넌트 마운트 시 검색 이력 로드
  useEffect(() => {
    loadRecentHistory();
  }, []);

  // 검색 완료 후 이력 새로고침
  const refreshHistoryAfterSearch = () => {
    setTimeout(() => {
      loadRecentHistory();
    }, 500); // 비동기 저장 대기
  };

  // 이력에서 질문 클릭 시 해당 질문으로 검색
  const handleHistoryClick = (historyItem: AiSearchHistoryResponse) => {
    setQuestion(historyItem.question);
    setShowHistory(false);
  };

  // 이력 삭제
  const handleDeleteHistory = async (e: React.MouseEvent, historyId: number) => {
    e.stopPropagation();
    try {
      await aiSearchService.deleteHistory(historyId);
      setRecentHistory(prev => prev.filter(h => h.id !== historyId));
    } catch (err) {
      console.error('이력 삭제 실패:', err);
    }
  };

  const handleSearch = async () => {
    if (!question.trim()) {
      alert('질문을 입력해주세요');
      return;
    }

    setIsSearching(true);
    setError(null);
    setResult(null);

    try {
      const request: AiSearchRequest = {
        question: question.trim(),
        topK: 5,
        similarityThreshold: 0.6,
        resourceTypes: selectedTypes.length > 0 ? selectedTypes : undefined,
        useUnifiedSearch: true,
      };

      const response = await aiSearchService.search(request);
      setResult(response);
      refreshHistoryAfterSearch();
    } catch (err: any) {
      console.error('AI 검색 실패:', err);
      setError(err.response?.data?.message || '검색 중 오류가 발생했습니다');
    } finally {
      setIsSearching(false);
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSearch();
    }
  };

  const handleTypeToggle = (type: ResourceType) => {
    setSelectedTypes(prev => {
      if (prev.includes(type)) {
        return prev.filter(t => t !== type);
      } else {
        return [...prev, type];
      }
    });
  };

  const handleSourceClick = (doc: SourceDocument) => {
    const resourceType = doc.resourceType || 'WIKI';

    switch (resourceType) {
      case 'SR':
        if (onSrClick && doc.resourceId) {
          onSrClick(doc.resourceId);
        } else if (doc.resourceId) {
          navigate(`/sr?highlight=${doc.resourceId}`);
        }
        break;
      case 'SURVEY':
        if (onSurveyClick && doc.resourceId) {
          onSurveyClick(doc.resourceId);
        } else if (doc.resourceId) {
          navigate(`/survey?highlight=${doc.resourceId}`);
        }
        break;
      case 'WIKI':
      default:
        if (onDocumentClick && doc.documentId) {
          onDocumentClick(doc.documentId);
        }
        break;
    }
  };

  const getResourceTypeIcon = (type?: ResourceType): string => {
    switch (type) {
      case 'SR': return '📋';
      case 'SURVEY': return '📊';
      case 'WIKI':
      default: return '📄';
    }
  };

  const getResourceTypeName = (type?: ResourceType): string => {
    switch (type) {
      case 'SR': return 'SR';
      case 'SURVEY': return '현황조사';
      case 'WIKI':
      default: return 'Wiki';
    }
  };

  const getResourceTypeColor = (type?: ResourceType): { bg: string; text: string } => {
    switch (type) {
      case 'SR': return { bg: '#e3f2fd', text: '#1565c0' };
      case 'SURVEY': return { bg: '#fff3e0', text: '#ef6c00' };
      case 'WIKI':
      default: return { bg: '#e8f5e9', text: '#2e7d32' };
    }
  };

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <div style={styles.headerTop}>
          <h3 style={styles.title}>🤖 AI 통합 검색</h3>
          {isAdmin && (
            <button
              style={styles.adminBtn}
              onClick={() => setShowAdminPanel(true)}
              title="임베딩 관리"
            >
              ⚙️ 임베딩 관리
            </button>
          )}
        </div>
        <p style={styles.subtitle}>
          Wiki, SR, 현황조사를 AI가 분석하여 답변합니다
        </p>
      </div>

      {/* 관리자 임베딩 관리 패널 */}
      {showAdminPanel && (
        <EmbeddingAdminPanel onClose={() => setShowAdminPanel(false)} />
      )}

      {/* 리소스 타입 필터 */}
      <div style={styles.typeFilter}>
        <span style={styles.typeFilterLabel}>검색 범위:</span>
        {(['WIKI', 'SR', 'SURVEY'] as ResourceType[]).map((type) => (
          <label key={type} style={styles.typeCheckbox}>
            <input
              type="checkbox"
              checked={selectedTypes.includes(type)}
              onChange={() => handleTypeToggle(type)}
              style={styles.checkbox}
            />
            <span style={{
              ...styles.typeLabel,
              backgroundColor: getResourceTypeColor(type).bg,
              color: getResourceTypeColor(type).text,
            }}>
              {getResourceTypeIcon(type)} {getResourceTypeName(type)}
            </span>
          </label>
        ))}
      </div>

      <div style={styles.searchBox}>
        <div style={styles.textareaWrapper}>
          <textarea
            style={styles.textarea}
            placeholder="예: 결제 관련 SR은 어떤 것들이 있나요? / 국민건강보험공단 시스템 환경은?"
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            onKeyPress={handleKeyPress}
            onFocus={() => recentHistory.length > 0 && setShowHistory(true)}
            rows={3}
          />
          {/* 최근 검색 이력 드롭다운 */}
          {showHistory && recentHistory.length > 0 && (
            <div style={styles.historyDropdown}>
              <div style={styles.historyHeader}>
                <span style={styles.historyTitle}>🕒 최근 검색</span>
                <button
                  style={styles.historyCloseBtn}
                  onClick={() => setShowHistory(false)}
                >
                  ✕
                </button>
              </div>
              {isLoadingHistory ? (
                <div style={styles.historyLoading}>로딩 중...</div>
              ) : (
                recentHistory.map((item) => (
                  <div
                    key={item.id}
                    style={styles.historyItem}
                    onClick={() => handleHistoryClick(item)}
                  >
                    <div style={styles.historyItemContent}>
                      <span style={styles.historyQuestion}>{item.question}</span>
                      <span style={styles.historyMeta}>
                        {item.sourceCount}개 참조 · {new Date(item.createdAt).toLocaleDateString('ko-KR')}
                      </span>
                    </div>
                    <button
                      style={styles.historyDeleteBtn}
                      onClick={(e) => handleDeleteHistory(e, item.id)}
                      title="삭제"
                    >
                      🗑️
                    </button>
                  </div>
                ))
              )}
            </div>
          )}
        </div>
        <button
          style={{
            ...styles.button,
            ...(isSearching ? styles.buttonDisabled : {}),
          }}
          onClick={handleSearch}
          disabled={isSearching}
        >
          {isSearching ? '검색 중...' : '검색'}
        </button>
      </div>

      {error && (
        <div style={styles.error}>
          <strong>오류:</strong> {error}
        </div>
      )}

      {result && (
        <div style={styles.result}>
          <div style={styles.answerSection}>
            <h4 style={styles.answerTitle}>📝 답변</h4>
            <div style={styles.answerContent} className="markdown-body">
              <ReactMarkdown
                remarkPlugins={[remarkGfm]}
                rehypePlugins={[rehypeHighlight, rehypeRaw]}
              >
                {result.answer}
              </ReactMarkdown>
            </div>
            <p style={styles.processingTime}>
              처리 시간: {(result.processingTimeMs / 1000).toFixed(2)}초
            </p>
          </div>

          {result.sources.length > 0 && (
            <div style={styles.sourcesSection}>
              <h4 style={styles.sourcesTitle}>📚 참고 자료</h4>
              <div style={styles.sourcesList}>
                {result.sources.map((source, index) => {
                  const typeColor = getResourceTypeColor(source.resourceType);
                  return (
                    <div
                      key={index}
                      style={styles.sourceCard}
                      onClick={() => handleSourceClick(source)}
                    >
                      <div style={styles.sourceHeader}>
                        <div style={styles.sourceTitleArea}>
                          {/* 리소스 타입 뱃지 */}
                          <span style={{
                            ...styles.resourceTypeBadge,
                            backgroundColor: typeColor.bg,
                            color: typeColor.text,
                          }}>
                            {getResourceTypeIcon(source.resourceType)} {getResourceTypeName(source.resourceType)}
                          </span>
                          <span style={styles.sourceTitle}>{source.title}</span>
                        </div>
                        <span style={styles.relevanceScore}>
                          {(source.relevanceScore * 100).toFixed(0)}% 관련도
                        </span>
                      </div>
                      <div style={styles.sourceMeta}>
                        {source.resourceIdentifier && (
                          <span style={styles.sourceIdentifier}>{source.resourceIdentifier}</span>
                        )}
                        {source.categoryName && (
                          <span style={styles.sourceCategoryBadge}>{source.categoryName}</span>
                        )}
                        {source.status && (
                          <span style={styles.sourceStatusBadge}>{source.status}</span>
                        )}
                      </div>
                      <p style={styles.sourceSnippet}>{source.snippet}</p>
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

const styles: { [key: string]: React.CSSProperties } = {
  container: {
    padding: '24px',
    backgroundColor: 'var(--bg-primary, white)',
    borderRadius: '12px',
    boxShadow: '0 2px 8px rgba(0, 0, 0, 0.08)',
    maxWidth: '900px',
    margin: '0 auto',
  },
  header: {
    marginBottom: '16px',
  },
  headerTop: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '8px',
  },
  title: {
    margin: 0,
    fontSize: '20px',
    fontWeight: '600',
    color: 'var(--text-primary)',
  },
  adminBtn: {
    display: 'flex',
    alignItems: 'center',
    gap: '4px',
    padding: '6px 12px',
    backgroundColor: '#6c757d',
    color: 'white',
    border: 'none',
    borderRadius: '6px',
    fontSize: '13px',
    fontWeight: '500',
    cursor: 'pointer',
    transition: 'background-color 0.2s',
  },
  subtitle: {
    margin: 0,
    fontSize: '14px',
    color: 'var(--text-secondary)',
  },
  typeFilter: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    marginBottom: '16px',
    flexWrap: 'wrap',
  },
  typeFilterLabel: {
    fontSize: '14px',
    color: 'var(--text-secondary)',
    fontWeight: '500',
  },
  typeCheckbox: {
    display: 'flex',
    alignItems: 'center',
    cursor: 'pointer',
  },
  checkbox: {
    marginRight: '4px',
    cursor: 'pointer',
  },
  typeLabel: {
    fontSize: '12px',
    fontWeight: '600',
    padding: '4px 10px',
    borderRadius: '12px',
    userSelect: 'none',
  },
  searchBox: {
    display: 'flex',
    gap: '12px',
    marginBottom: '20px',
  },
  textareaWrapper: {
    flex: 1,
    position: 'relative' as const,
  },
  textarea: {
    width: '100%',
    padding: '12px',
    fontSize: '14px',
    border: '1px solid var(--border-color)',
    borderRadius: '6px',
    fontFamily: 'inherit',
    resize: 'vertical',
    backgroundColor: 'var(--bg-primary)',
    color: 'var(--text-primary)',
    boxSizing: 'border-box' as const,
  },
  historyDropdown: {
    position: 'absolute' as const,
    top: '100%',
    left: 0,
    right: 0,
    backgroundColor: 'var(--bg-primary)',
    border: '1px solid var(--border-color)',
    borderRadius: '6px',
    boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
    zIndex: 100,
    maxHeight: '300px',
    overflowY: 'auto' as const,
    marginTop: '4px',
  },
  historyHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '10px 12px',
    borderBottom: '1px solid var(--border-color)',
    backgroundColor: 'var(--bg-secondary)',
  },
  historyTitle: {
    fontSize: '13px',
    fontWeight: '600',
    color: 'var(--text-primary)',
  },
  historyCloseBtn: {
    background: 'none',
    border: 'none',
    fontSize: '14px',
    cursor: 'pointer',
    color: 'var(--text-secondary)',
    padding: '2px 6px',
  },
  historyLoading: {
    padding: '16px',
    textAlign: 'center' as const,
    color: 'var(--text-secondary)',
    fontSize: '13px',
  },
  historyItem: {
    display: 'flex',
    alignItems: 'center',
    padding: '10px 12px',
    cursor: 'pointer',
    borderBottom: '1px solid var(--border-color)',
    transition: 'background-color 0.2s',
  },
  historyItemContent: {
    flex: 1,
    display: 'flex',
    flexDirection: 'column' as const,
    gap: '4px',
    minWidth: 0,
  },
  historyQuestion: {
    fontSize: '14px',
    color: 'var(--text-primary)',
    whiteSpace: 'nowrap' as const,
    overflow: 'hidden',
    textOverflow: 'ellipsis',
  },
  historyMeta: {
    fontSize: '12px',
    color: 'var(--text-secondary)',
  },
  historyDeleteBtn: {
    background: 'none',
    border: 'none',
    fontSize: '14px',
    cursor: 'pointer',
    padding: '4px 8px',
    opacity: 0.6,
    transition: 'opacity 0.2s',
  },
  button: {
    padding: '12px 24px',
    backgroundColor: '#007bff',
    color: 'white',
    border: 'none',
    borderRadius: '6px',
    fontSize: '14px',
    fontWeight: '600',
    cursor: 'pointer',
    whiteSpace: 'nowrap',
    transition: 'background-color 0.2s',
  },
  buttonDisabled: {
    backgroundColor: '#6c757d',
    cursor: 'not-allowed',
  },
  error: {
    padding: '12px',
    backgroundColor: '#f8d7da',
    color: '#721c24',
    border: '1px solid #f5c6cb',
    borderRadius: '6px',
    marginBottom: '20px',
  },
  result: {
    marginTop: '20px',
  },
  answerSection: {
    padding: '20px',
    backgroundColor: 'var(--bg-primary)',
    borderRadius: '8px',
    marginBottom: '20px',
    border: '1px solid var(--border-color)',
  },
  answerTitle: {
    margin: '0 0 16px 0',
    fontSize: '16px',
    fontWeight: '600',
    color: 'var(--text-primary)',
  },
  answerContent: {
    fontSize: '14px',
    lineHeight: '1.6',
    color: 'var(--text-primary)',
    maxHeight: 'none', /* 높이 제한 없음 - 부모 컨테이너에서 스크롤 처리 */
    overflowWrap: 'break-word',
  },
  processingTime: {
    marginTop: '12px',
    fontSize: '12px',
    color: 'var(--text-secondary)',
    textAlign: 'right',
  },
  sourcesSection: {
    padding: '20px',
    backgroundColor: 'var(--bg-primary)',
    borderRadius: '8px',
    border: '1px solid var(--border-color)',
  },
  sourcesTitle: {
    margin: '0 0 16px 0',
    fontSize: '16px',
    fontWeight: '600',
    color: 'var(--text-primary)',
  },
  sourcesList: {
    display: 'flex',
    flexDirection: 'column',
    gap: '12px',
  },
  sourceCard: {
    padding: '16px',
    backgroundColor: 'var(--bg-secondary)',
    border: '1px solid var(--border-color)',
    borderRadius: '6px',
    cursor: 'pointer',
    transition: 'all 0.2s',
  },
  sourceHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: '8px',
    gap: '8px',
  },
  sourceTitleArea: {
    display: 'flex',
    flexDirection: 'column',
    gap: '6px',
    flex: 1,
  },
  resourceTypeBadge: {
    display: 'inline-block',
    fontSize: '11px',
    fontWeight: '600',
    padding: '3px 8px',
    borderRadius: '10px',
    width: 'fit-content',
  },
  sourceTitle: {
    fontSize: '15px',
    fontWeight: '600',
    color: '#007bff',
  },
  sourceMeta: {
    display: 'flex',
    flexWrap: 'wrap',
    gap: '8px',
    marginBottom: '8px',
  },
  sourceIdentifier: {
    fontSize: '12px',
    color: '#495057',
    backgroundColor: '#f8f9fa',
    padding: '3px 8px',
    borderRadius: '4px',
    fontFamily: 'monospace',
  },
  relevanceScore: {
    fontSize: '12px',
    fontWeight: '600',
    color: '#28a745',
    backgroundColor: '#d4edda',
    padding: '4px 8px',
    borderRadius: '4px',
  },
  sourceCategoryBadge: {
    display: 'inline-block',
    fontSize: '12px',
    color: '#6c757d',
    backgroundColor: '#e9ecef',
    padding: '3px 8px',
    borderRadius: '4px',
  },
  sourceStatusBadge: {
    display: 'inline-block',
    fontSize: '11px',
    color: '#856404',
    backgroundColor: '#fff3cd',
    padding: '3px 8px',
    borderRadius: '4px',
    fontWeight: '500',
  },
  sourceSnippet: {
    margin: 0,
    fontSize: '13px',
    color: 'var(--text-secondary)',
    lineHeight: '1.5',
  },
};

export default AiSearchBox;
