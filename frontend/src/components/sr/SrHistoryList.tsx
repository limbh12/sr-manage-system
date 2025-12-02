import React, { useState, useEffect } from 'react';
import { useSr } from '../../hooks/useSr';

interface SrHistoryListProps {
  srId: number;
}

const SrHistoryList: React.FC<SrHistoryListProps> = ({ srId }) => {
  const { srHistories, fetchSrHistories, createSrHistory } = useSr();
  const [comment, setComment] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [expandedHistoryId, setExpandedHistoryId] = useState<number | null>(null);

  useEffect(() => {
    if (srId) {
      fetchSrHistories(srId);
    }
  }, [srId, fetchSrHistories]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!comment.trim()) return;

    setIsSubmitting(true);
    try {
      await createSrHistory(srId, { content: comment });
      setComment('');
    } finally {
      setIsSubmitting(false);
    }
  };

  const toggleExpand = (id: number) => {
    setExpandedHistoryId(expandedHistoryId === id ? null : id);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString();
  };

  const getHistoryTypeLabel = (type: string) => {
    switch (type) {
      case 'COMMENT': return '댓글';
      case 'STATUS_CHANGE': return '상태 변경';
      case 'PRIORITY_CHANGE': return '우선순위 변경';
      case 'ASSIGNEE_CHANGE': return '담당자 변경';
      case 'INFO_CHANGE': return '정보 변경';
      default: return type;
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <h3 style={{ fontSize: '16px', fontWeight: 600, marginBottom: '16px', color: '#333' }}>이력 및 댓글</h3>
      
      {/* 댓글 입력 폼 */}
      <form onSubmit={handleSubmit} style={{ marginBottom: '20px' }}>
        <div style={{ display: 'flex', gap: '8px' }}>
          <input
            type="text"
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            placeholder="댓글을 입력하세요..."
            style={{
              flex: 1,
              padding: '8px 12px',
              borderRadius: '4px',
              border: '1px solid #ddd',
              fontSize: '14px'
            }}
            disabled={isSubmitting}
          />
          <button
            type="submit"
            disabled={isSubmitting || !comment.trim()}
            className="btn btn-primary"
            style={{ padding: '8px 16px', fontSize: '14px' }}
          >
            등록
          </button>
        </div>
      </form>

      {/* 이력 목록 */}
      <div style={{ flex: 1, overflowY: 'auto', paddingRight: '8px' }}>
        {srHistories.map((history) => {
          const isInfoChange = history.historyType === 'INFO_CHANGE';
          // 요청사항이나 처리내용 변경인 경우에만 아코디언 UI 사용 (긴 텍스트)
          const isLargeTextChange = isInfoChange && (
            history.content.includes('요청사항이') || 
            history.content.includes('처리내용이')
          );
          
          const hasDetails = history.previousValue || history.newValue;
          const isExpanded = expandedHistoryId === history.id;

          return (
            <div key={history.id} style={{ position: 'relative', paddingLeft: '16px', borderLeft: '2px solid #e0e0e0', paddingBottom: '24px' }}>
              <div style={{ 
                position: 'absolute', 
                left: '-5px', 
                top: '0', 
                width: '8px', 
                height: '8px', 
                borderRadius: '50%', 
                backgroundColor: history.historyType === 'COMMENT' ? '#666' : '#1976d2' 
              }}></div>
              
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
                <div style={{ fontSize: '12px', color: '#666' }}>{formatDate(history.createdAt)}</div>
                <span style={{ 
                  fontSize: '11px', 
                  padding: '2px 6px', 
                  borderRadius: '10px', 
                  backgroundColor: '#f0f0f0', 
                  color: '#555' 
                }}>
                  {getHistoryTypeLabel(history.historyType)}
                </span>
              </div>
              
              <div style={{ fontWeight: 500, fontSize: '14px', marginBottom: '4px' }}>
                {history.createdBy.name}
              </div>
              
              <div style={{ fontSize: '13px', color: '#444', whiteSpace: 'pre-wrap' }}>
                {history.content}
              </div>
              
              {/* 일반 변경 이력 (한 줄 표시) - 긴 텍스트 변경이 아닌 경우 */}
              {!isLargeTextChange && hasDetails && (
                <div style={{ fontSize: '12px', color: '#888', marginTop: '4px', backgroundColor: '#f9f9f9', padding: '4px', borderRadius: '4px' }}>
                  {history.previousValue} → {history.newValue}
                </div>
              )}

              {/* 상세 정보 변경 이력 (아코디언) - 긴 텍스트 변경인 경우 */}
              {isLargeTextChange && hasDetails && (
                <div style={{ marginTop: '8px' }}>
                  <button 
                    onClick={() => toggleExpand(history.id)}
                    style={{
                      fontSize: '12px',
                      color: '#1976d2',
                      background: 'none',
                      border: 'none',
                      padding: 0,
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '4px'
                    }}
                  >
                    {isExpanded ? '접기' : '변경 내역 보기'}
                    <span style={{ transform: isExpanded ? 'rotate(180deg)' : 'rotate(0deg)', transition: 'transform 0.2s' }}>▼</span>
                  </button>
                  
                  {isExpanded && (
                    <div style={{ marginTop: '8px', border: '1px solid #eee', borderRadius: '4px', overflow: 'hidden' }}>
                      <div style={{ padding: '8px', backgroundColor: '#fff0f0', borderBottom: '1px solid #eee' }}>
                        <div style={{ fontSize: '11px', fontWeight: 'bold', color: '#d32f2f', marginBottom: '4px' }}>변경 전</div>
                        <div style={{ fontSize: '12px', color: '#333', whiteSpace: 'pre-wrap', maxHeight: '150px', overflowY: 'auto' }}>
                          {history.previousValue || '(내용 없음)'}
                        </div>
                      </div>
                      <div style={{ padding: '8px', backgroundColor: '#f0fff4' }}>
                        <div style={{ fontSize: '11px', fontWeight: 'bold', color: '#2e7d32', marginBottom: '4px' }}>변경 후</div>
                        <div style={{ fontSize: '12px', color: '#333', whiteSpace: 'pre-wrap', maxHeight: '150px', overflowY: 'auto' }}>
                          {history.newValue || '(내용 없음)'}
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>
          );
        })}
        
        {srHistories.length === 0 && (
          <div style={{ textAlign: 'center', color: '#999', padding: '20px 0' }}>
            이력이 없습니다.
          </div>
        )}
      </div>
    </div>
  );
};

export default SrHistoryList;
