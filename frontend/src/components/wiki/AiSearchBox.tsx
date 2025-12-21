import React, { useState } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeHighlight from 'rehype-highlight';
import rehypeRaw from 'rehype-raw';
import { AiSearchRequest, AiSearchResponse, SourceDocument } from '../../types/aiSearch';
import aiSearchService from '../../services/aiSearchService';

interface AiSearchBoxProps {
  onDocumentClick?: (documentId: number) => void;
}

/**
 * AI 검색 박스 컴포넌트
 * - RAG 기반 자연어 검색
 * - 참고 문서 링크 표시
 */
const AiSearchBox: React.FC<AiSearchBoxProps> = ({ onDocumentClick }) => {
  const [question, setQuestion] = useState('');
  const [isSearching, setIsSearching] = useState(false);
  const [result, setResult] = useState<AiSearchResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

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
        topK: 3,
        similarityThreshold: 0.7,
      };

      const response = await aiSearchService.search(request);
      setResult(response);
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

  const handleSourceClick = (doc: SourceDocument) => {
    if (onDocumentClick) {
      onDocumentClick(doc.documentId);
    }
  };

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <h3 style={styles.title}>🤖 AI 검색</h3>
        <p style={styles.subtitle}>
          Wiki 문서를 AI가 분석하여 답변합니다
        </p>
      </div>

      <div style={styles.searchBox}>
        <textarea
          style={styles.textarea}
          placeholder="예: SR 생성 API는 어떻게 사용하나요?"
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          onKeyPress={handleKeyPress}
          rows={3}
        />
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
              <h4 style={styles.sourcesTitle}>📚 참고 문서</h4>
              <div style={styles.sourcesList}>
                {result.sources.map((source, index) => (
                  <div
                    key={index}
                    style={styles.sourceCard}
                    onClick={() => handleSourceClick(source)}
                  >
                    <div style={styles.sourceHeader}>
                      <span style={styles.sourceTitle}>{source.title}</span>
                      <span style={styles.relevanceScore}>
                        {(source.relevanceScore * 100).toFixed(0)}% 관련도
                      </span>
                    </div>
                    {source.categoryName && (
                      <div style={styles.sourceCategoryBadge}>{source.categoryName}</div>
                    )}
                    <p style={styles.sourceSnippet}>{source.snippet}</p>
                  </div>
                ))}
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
  title: {
    margin: '0 0 8px 0',
    fontSize: '20px',
    fontWeight: '600',
    color: 'var(--text-primary)',
  },
  subtitle: {
    margin: 0,
    fontSize: '14px',
    color: 'var(--text-secondary)',
  },
  searchBox: {
    display: 'flex',
    gap: '12px',
    marginBottom: '20px',
  },
  textarea: {
    flex: 1,
    padding: '12px',
    fontSize: '14px',
    border: '1px solid var(--border-color)',
    borderRadius: '6px',
    fontFamily: 'inherit',
    resize: 'vertical',
    backgroundColor: 'var(--bg-primary)',
    color: 'var(--text-primary)',
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
    alignItems: 'center',
    marginBottom: '8px',
  },
  sourceTitle: {
    fontSize: '15px',
    fontWeight: '600',
    color: '#007bff',
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
    padding: '4px 8px',
    borderRadius: '4px',
    marginBottom: '8px',
  },
  sourceSnippet: {
    margin: 0,
    fontSize: '13px',
    color: 'var(--text-secondary)',
    lineHeight: '1.5',
  },
};

export default AiSearchBox;
