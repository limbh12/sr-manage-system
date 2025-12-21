import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Sr, SrStatus, Priority, OpenApiSurvey } from '../../types';
import * as surveyService from '../../services/surveyService';
import { commonCodeService } from '../../services/commonCodeService';
import { wikiDocumentApi } from '../../services/wikiService';
import type { WikiDocument } from '../../types/wiki';
import type { SrEmbeddingStatusResponse } from '../../types/aiSearch';
import OpenApiSurveyInfoCard from './OpenApiSurveyInfoCard';
import SrHistoryList from './SrHistoryList';
import { formatPhoneNumber } from '../../utils/formatUtils';
import aiSearchService from '../../services/aiSearchService';

interface SrDetailProps {
  sr: Sr;
  onClose: () => void;
  onEdit: () => void;
  onStatusChange: (status: SrStatus) => void;
  onWikiClick?: (wikiId: number) => void; // Wiki 문서 클릭 시 콜백 (선택사항)
  isModal?: boolean; // true: 모달로 렌더링 (기본값), false: 패널 내용만 렌더링
}

/**
 * 상태 한글명 반환
 */
const getStatusLabel = (status: SrStatus): string => {
  switch (status) {
    case 'OPEN':
      return '신규';
    case 'IN_PROGRESS':
      return '처리중';
    case 'RESOLVED':
      return '해결됨';
    case 'CLOSED':
      return '종료';
    default:
      return status;
  }
};

/**
 * 우선순위 한글명 반환
 */
const getPriorityLabel = (priority: Priority): string => {
  switch (priority) {
    case 'LOW':
      return '낮음';
    case 'MEDIUM':
      return '보통';
    case 'HIGH':
      return '높음';
    case 'CRITICAL':
      return '긴급';
    default:
      return priority;
  }
};

/**
 * 우선순위 뱃지 클래스 반환
 */
const getPriorityBadgeClass = (priority: Priority): string => {
  switch (priority) {
    case 'LOW':
      return 'badge badge-low';
    case 'MEDIUM':
      return 'badge badge-medium';
    case 'HIGH':
      return 'badge badge-high';
    case 'CRITICAL':
      return 'badge badge-critical';
    default:
      return 'badge';
  }
};

/**
 * 처리예정일자가 내일인지 확인
 */
const isDueTomorrow = (expectedDate?: string): boolean => {
  if (!expectedDate) return false;
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  tomorrow.setHours(0, 0, 0, 0);

  const dueDate = new Date(expectedDate);
  dueDate.setHours(0, 0, 0, 0);

  return dueDate.getTime() === tomorrow.getTime();
};

/**
 * SR 상세 컴포넌트
 */
function SrDetail({ sr, onClose, onEdit, onStatusChange, onWikiClick, isModal = true }: SrDetailProps) {
  const navigate = useNavigate();
  const statusOptions: SrStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];
  const [linkedSurvey, setLinkedSurvey] = useState<OpenApiSurvey | null>(null);
  const [linkedWikiDocs, setLinkedWikiDocs] = useState<WikiDocument[]>([]);
  const [categoryName, setCategoryName] = useState('');
  const [requestTypeName, setRequestTypeName] = useState('');

  // 임베딩 상태
  const [embeddingStatus, setEmbeddingStatus] = useState<SrEmbeddingStatusResponse | null>(null);
  const [embeddingLoading, setEmbeddingLoading] = useState(false);
  const [isGeneratingEmbedding, setIsGeneratingEmbedding] = useState(false);

  useEffect(() => {
    if (sr.openApiSurveyId) {
      surveyService.getSurveyById(sr.openApiSurveyId)
        .then(setLinkedSurvey)
        .catch(err => console.error('Failed to load linked survey', err));
    } else {
      setLinkedSurvey(null);
    }
  }, [sr.openApiSurveyId]);

  // 연계된 Wiki 문서 로드
  useEffect(() => {
    wikiDocumentApi.getBySr(sr.id)
      .then(response => setLinkedWikiDocs(response.data))
      .catch(err => console.error('Failed to load linked wiki documents', err));
  }, [sr.id]);

  // 임베딩 상태 로드
  useEffect(() => {
    const loadEmbeddingStatus = async () => {
      try {
        setEmbeddingLoading(true);
        const status = await aiSearchService.getSrEmbeddingStatus(sr.id);
        setEmbeddingStatus(status);
      } catch (err) {
        console.error('Failed to load SR embedding status', err);
      } finally {
        setEmbeddingLoading(false);
      }
    };
    loadEmbeddingStatus();
  }, [sr.id]);

  // 임베딩 생성 핸들러
  const handleGenerateEmbedding = async () => {
    try {
      setIsGeneratingEmbedding(true);
      await aiSearchService.generateSrEmbedding(sr.id);
      // 상태 새로고침
      const status = await aiSearchService.getSrEmbeddingStatus(sr.id);
      setEmbeddingStatus(status);
    } catch (err) {
      console.error('Failed to generate SR embedding', err);
      alert('임베딩 생성에 실패했습니다.');
    } finally {
      setIsGeneratingEmbedding(false);
    }
  };

  useEffect(() => {
    if (sr.category) {
      commonCodeService.getActiveCodesByGroup('SR_CATEGORY').then(codes => {
        const found = codes.find(c => c.codeValue === sr.category);
        if (found) setCategoryName(found.codeName);
      }).catch(console.error);
    } else {
      setCategoryName('');
    }

    if (sr.requestType) {
      commonCodeService.getActiveCodesByGroup('SR_REQUEST_TYPE').then(codes => {
        const found = codes.find(c => c.codeValue === sr.requestType);
        if (found) setRequestTypeName(found.codeName);
      }).catch(console.error);
    } else {
      setRequestTypeName('');
    }
  }, [sr.category, sr.requestType]);

  useEffect(() => {
    const handleEsc = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', handleEsc);
    return () => {
      window.removeEventListener('keydown', handleEsc);
    };
  }, [onClose]);

  // SR 상세 내용 (모달/패널 공통)
  const detailContent = (
    <>
      {isModal && (
        <div className="modal-header">
          <h2 className="modal-title">SR 상세 정보</h2>
          <button className="modal-close" onClick={onClose}>
            ×
          </button>
        </div>
      )}

      <div style={{ display: 'flex', gap: '32px', minHeight: '400px' }}>
          {/* 좌측: 상세 정보 */}
          <div style={{ flex: 1 }}>
            <div style={{ marginBottom: '16px' }}>
              <strong>ID:</strong> {sr.srId || sr.id}
            </div>

            <div style={{ marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <strong>제목:</strong>
              <span>{sr.title}</span>
              <span className={getPriorityBadgeClass(sr.priority)}>
                {getPriorityLabel(sr.priority)}
              </span>
            </div>

            <div style={{ marginBottom: '16px' }}>
              <strong>상태:</strong>
              <div style={{ marginTop: '8px', display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                {statusOptions.map((status) => (
                  <button
                    key={status}
                    className={`btn ${sr.status === status ? 'btn-primary' : 'btn-secondary'}`}
                    onClick={() => onStatusChange(status)}
                    disabled={sr.status === status}
                    style={sr.status === status ? { opacity: 1, cursor: 'default' } : {}}
                  >
                    {getStatusLabel(status)}
                  </button>
                ))}
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '16px' }}>
              <div>
                <strong>분류:</strong> {categoryName || sr.category || '-'}
              </div>
              <div>
                <strong>요청구분:</strong> {requestTypeName || sr.requestType || '-'}
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '16px' }}>
              <div>
                <strong>접수자:</strong> {sr.requester.name || sr.requester.username} ({sr.requester.email})
              </div>
              <div>
                <strong>담당자:</strong>{' '}
                {sr.assignee
                  ? `${sr.assignee.name || sr.assignee.username} (${sr.assignee.email})`
                  : '(미지정)'}
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '16px' }}>
              <div>
                <strong>요청자:</strong> {sr.applicantName || '-'}
              </div>
              <div>
                <strong>연락처:</strong> {sr.applicantPhone ? formatPhoneNumber(sr.applicantPhone) : '-'}
              </div>
            </div>

            <div style={{ marginBottom: '16px' }}>
              <strong>처리예정일자:</strong>{' '}
              {sr.expectedCompletionDate ? (
                <span style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}>
                  <span>{new Date(sr.expectedCompletionDate).toLocaleDateString()}</span>
                  {isDueTomorrow(sr.expectedCompletionDate) && (
                    <span className="badge badge-critical" style={{ fontSize: '12px' }}>
                      D-1 (내일 마감)
                    </span>
                  )}
                </span>
              ) : (
                '(미지정)'
              )}
            </div>

            {linkedSurvey && (
              <div style={{ marginBottom: '16px' }}>
                <strong>OPEN API 현황조사 정보:</strong>
                <div style={{ marginTop: '8px' }}>
                  <OpenApiSurveyInfoCard survey={linkedSurvey} />
                </div>
              </div>
            )}

            <div style={{ marginBottom: '16px' }}>
              <strong>요청사항:</strong>
              <div className="markdown-body bg-history-detail" style={{ marginTop: '8px', padding: '12px', borderRadius: '4px', overflowX: 'auto' }}>
                {sr.description ? (
                  <ReactMarkdown remarkPlugins={[remarkGfm]}>
                    {sr.description}
                  </ReactMarkdown>
                ) : (
                  '(요청사항 없음)'
                )}
              </div>
            </div>

            {sr.processingDetails && (
              <div style={{ marginBottom: '16px' }}>
                <strong>처리내용:</strong>
                <div className="markdown-body bg-info-light" style={{ marginTop: '8px', padding: '12px', borderRadius: '4px', overflowX: 'auto' }}>
                  <ReactMarkdown remarkPlugins={[remarkGfm]}>
                    {sr.processingDetails}
                  </ReactMarkdown>
                </div>
              </div>
            )}

            {/* 연계된 Wiki 문서 목록 */}
            {linkedWikiDocs.length > 0 && (
              <div style={{ marginBottom: '16px' }}>
                <strong>연계된 Wiki 문서:</strong>
                <div style={{ marginTop: '8px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  {linkedWikiDocs.map(doc => (
                    <div
                      key={doc.id}
                      onClick={() => {
                        if (onWikiClick) {
                          onWikiClick(doc.id);
                        } else {
                          onClose();
                          navigate(`/wiki/${doc.id}`);
                        }
                      }}
                      style={{
                        padding: '10px 12px',
                        background: '#f6f8fa',
                        border: '1px solid #d1d5da',
                        borderRadius: '4px',
                        cursor: 'pointer',
                        transition: 'all 0.2s',
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                      }}
                      onMouseEnter={(e) => {
                        e.currentTarget.style.background = '#e1f0ff';
                        e.currentTarget.style.borderColor = '#0366d6';
                      }}
                      onMouseLeave={(e) => {
                        e.currentTarget.style.background = '#f6f8fa';
                        e.currentTarget.style.borderColor = '#d1d5da';
                      }}
                    >
                      <span style={{ fontSize: '14px', color: '#24292e', fontWeight: 500 }}>
                        {doc.title}
                      </span>
                      <span style={{ fontSize: '16px', color: '#0366d6', fontWeight: 'bold' }}>→</span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '16px', fontSize: '0.9em' }}>
              <div>
                <strong>등록일(접수일):</strong> {new Date(sr.createdAt).toLocaleString()}
              </div>
              <div>
                <strong>수정일:</strong> {new Date(sr.updatedAt).toLocaleString()}
              </div>
            </div>

            {/* AI 검색 임베딩 상태 */}
            <div style={{
              marginBottom: '16px',
              padding: '12px',
              borderRadius: '4px',
              background: 'var(--bg-info-light, #e8f4fd)',
              border: '1px solid var(--border-info, #b3d7f5)'
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                <strong style={{ fontSize: '14px' }}>AI 검색 임베딩</strong>
                <button
                  className="btn btn-sm btn-secondary"
                  onClick={handleGenerateEmbedding}
                  disabled={isGeneratingEmbedding || embeddingLoading}
                  style={{ fontSize: '12px', padding: '4px 10px' }}
                >
                  {isGeneratingEmbedding ? '생성 중...' : (embeddingStatus?.hasEmbedding ? '재생성' : '임베딩 생성')}
                </button>
              </div>
              {embeddingLoading ? (
                <div style={{ color: '#666', fontSize: '13px' }}>로딩 중...</div>
              ) : embeddingStatus ? (
                <div style={{ fontSize: '13px', color: '#444' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
                    <span>상태:</span>
                    {embeddingStatus.hasEmbedding ? (
                      embeddingStatus.isUpToDate ? (
                        <span style={{ color: '#28a745', fontWeight: 500 }}>최신 ✓</span>
                      ) : (
                        <span style={{ color: '#e36209', fontWeight: 500 }}>업데이트 필요</span>
                      )
                    ) : (
                      <span style={{ color: '#999' }}>임베딩 없음</span>
                    )}
                  </div>
                  {embeddingStatus.hasEmbedding && (
                    <div style={{ color: '#666', fontSize: '12px' }}>
                      청크 수: {embeddingStatus.chunkCount}개
                      {embeddingStatus.lastEmbeddingDate && (
                        <> | 생성일: {new Date(embeddingStatus.lastEmbeddingDate).toLocaleString()}</>
                      )}
                    </div>
                  )}
                </div>
              ) : (
                <div style={{ color: '#999', fontSize: '13px' }}>상태를 불러올 수 없습니다</div>
              )}
            </div>
          </div>

          {/* 우측: 변경 이력 */}
          <div style={{ width: '350px', minWidth: '350px', flexShrink: 0, borderLeft: '1px solid #eee', paddingLeft: '32px', display: 'flex', flexDirection: 'column' }}>
            <SrHistoryList srId={sr.id} />
          </div>
        </div>

        {isModal && (
          <div className="modal-footer">
            <button className="btn btn-secondary" onClick={onClose}>
              닫기
            </button>
            <button className="btn btn-primary" onClick={onEdit}>
              수정
            </button>
          </div>
        )}
      </>
  );

  // 모달 모드: modal-overlay로 감싸서 렌더링
  if (isModal) {
    return (
      <div className="modal-overlay">
        <div className="modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '1400px', width: '95%' }}>
          {detailContent}
        </div>
      </div>
    );
  }

  // 패널 모드: 내용만 렌더링
  return detailContent;
}

export default SrDetail;
