import React, { useEffect, useState } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import SlidePanel from '../common/SlidePanel';
import { wikiDocumentApi } from '../../services/wikiService';
import type { WikiDocument } from '../../types/wiki';
import './WikiDetailPanel.css';

interface WikiDetailPanelProps {
  documentId: number | null;
  onClose: () => void;
  onSrClick?: (srId: number) => void; // SR 클릭 시 SR 패널 열기
}

const WikiDetailPanel: React.FC<WikiDetailPanelProps> = ({
  documentId,
  onClose,
  onSrClick
}) => {
  const [document, setDocument] = useState<WikiDocument | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (documentId) {
      loadDocument();
    }
  }, [documentId]);

  const loadDocument = async () => {
    if (!documentId) return;

    try {
      setLoading(true);
      setError(null);
      const response = await wikiDocumentApi.get(documentId);
      setDocument(response.data);
    } catch (err) {
      console.error('Wiki 문서 조회 실패:', err);
      setError('Wiki 문서를 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const getStatusText = (status: string) => {
    switch (status) {
      case 'OPEN': return '접수';
      case 'IN_PROGRESS': return '진행중';
      case 'RESOLVED': return '해결';
      case 'CLOSED': return '종료';
      default: return status;
    }
  };

  const getStatusBadgeClass = (status: string) => {
    switch (status) {
      case 'OPEN': return 'status-open';
      case 'IN_PROGRESS': return 'status-in-progress';
      case 'RESOLVED': return 'status-resolved';
      case 'CLOSED': return 'status-closed';
      default: return '';
    }
  };

  return (
    <SlidePanel
      isOpen={documentId !== null}
      onClose={onClose}
      title={document ? document.title : 'Wiki 문서'}
      width="60%"
    >
      {loading && (
        <div style={{ textAlign: 'center', padding: '40px', color: '#586069' }}>
          로딩 중...
        </div>
      )}

      {error && (
        <div style={{
          padding: '20px',
          color: '#d73a49',
          background: '#ffeef0',
          borderRadius: '4px',
          border: '1px solid #fdb8c0'
        }}>
          {error}
        </div>
      )}

      {!loading && !error && document && (
        <div className="wiki-detail-panel">
          {/* 문서 정보 */}
          <div className="wiki-panel-info">
            {document.categoryName && (
              <span className="wiki-panel-category">{document.categoryName}</span>
            )}
            <span className="wiki-panel-meta">
              작성자: {document.createdByName} | 작성일: {new Date(document.createdAt).toLocaleDateString()}
            </span>
            {document.updatedAt && document.updatedAt !== document.createdAt && (
              <span className="wiki-panel-meta">
                | 수정일: {new Date(document.updatedAt).toLocaleDateString()}
              </span>
            )}
            <span className="wiki-panel-meta">
              | 조회수: {document.viewCount}
            </span>
          </div>

          {/* 연계된 SR 목록 */}
          {document.srs && document.srs.length > 0 && (
            <div className="wiki-panel-linked-srs">
              <h3>연계된 SR</h3>
              <div className="wiki-panel-sr-list">
                {document.srs.map(sr => (
                  <div
                    key={sr.id}
                    className="wiki-panel-sr-item"
                    onClick={() => onSrClick && onSrClick(sr.id)}
                  >
                    <span className={`sr-status-badge ${getStatusBadgeClass(sr.status)}`}>
                      {getStatusText(sr.status)}
                    </span>
                    <span className="wiki-panel-sr-title">{sr.title}</span>
                    <span className="link-arrow">→</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* 문서 내용 */}
          <div className="wiki-panel-content-area">
            <div className="markdown-body">
              <ReactMarkdown remarkPlugins={[remarkGfm]}>
                {document.content}
              </ReactMarkdown>
            </div>
          </div>
        </div>
      )}
    </SlidePanel>
  );
};

export default WikiDetailPanel;
