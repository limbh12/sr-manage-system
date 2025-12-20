import React, { useState, useEffect } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import WikiCategoryTree from '../components/wiki/WikiCategoryTree';
import WikiCategoryModal from '../components/wiki/WikiCategoryModal';
import WikiEditor from '../components/wiki/WikiEditor';
import WikiViewer from '../components/wiki/WikiViewer';
import SrSelector from '../components/wiki/SrSelector';
import SrDetailPanel from '../components/sr/SrDetailPanel';
import PdfUploadModal from '../components/wiki/PdfUploadModal';
import VersionHistoryModal from '../components/wiki/VersionHistoryModal';
import AiSearchBox from '../components/wiki/AiSearchBox';
import aiSearchService from '../services/aiSearchService';
import {
  wikiDocumentApi,
  wikiCategoryApi,
} from '../services/wikiService';
import type { WikiDocument, WikiCategory, WikiDocumentRequest, WikiCategoryRequest, SrInfo } from '../types/wiki';
import type { EmbeddingStatusResponse, EmbeddingProgressEvent } from '../types/aiSearch';
import './WikiPage.css';

const WikiPage: React.FC = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const [searchParams] = useSearchParams();
  const categoryIdParam = searchParams.get('categoryId');

  const [categories, setCategories] = useState<WikiCategory[]>([]);
  const [selectedCategoryId, setSelectedCategoryId] = useState<number | undefined>();
  const [documents, setDocuments] = useState<WikiDocument[]>([]);
  const [currentDocument, setCurrentDocument] = useState<WikiDocument | null>(null);
  const [isEditing, setIsEditing] = useState(false);
  const [isCreating, setIsCreating] = useState(false);
  const [editContent, setEditContent] = useState('');
  const [editTitle, setEditTitle] = useState('');
  const [searchKeyword, setSearchKeyword] = useState('');
  const [loading, setLoading] = useState(false);
  const [showCategoryModal, setShowCategoryModal] = useState(false);
  const [editingCategory, setEditingCategory] = useState<WikiCategory | null>(null);
  const [parentCategoryId, setParentCategoryId] = useState<number | undefined>();
  const [showAllDocuments, setShowAllDocuments] = useState(false);
  const [editCategoryId, setEditCategoryId] = useState<number | undefined>();
  const [flatCategories, setFlatCategories] = useState<WikiCategory[]>([]);
  const [editSrs, setEditSrs] = useState<SrInfo[]>([]);
  const [selectedSrId, setSelectedSrId] = useState<number | null>(null);
  const [showPdfUpload, setShowPdfUpload] = useState(false);
  const [showVersionHistory, setShowVersionHistory] = useState(false);
  const [generateToc, setGenerateToc] = useState(false); // 목차 자동 생성 옵션
  const [embeddingStatus, setEmbeddingStatus] = useState<EmbeddingStatusResponse | null>(null);
  const [isGeneratingEmbedding, setIsGeneratingEmbedding] = useState(false);
  const [embeddingProgress, setEmbeddingProgress] = useState<EmbeddingProgressEvent | null>(null);

  // 카테고리 로드
  useEffect(() => {
    loadCategories();
  }, []);

  // URL 파라미터에서 카테고리 설정
  useEffect(() => {
    if (categoryIdParam) {
      setSelectedCategoryId(parseInt(categoryIdParam));
    }
  }, [categoryIdParam]);

  // 문서 ID가 있으면 문서 로드
  useEffect(() => {
    if (id) {
      loadDocument(parseInt(id));
    } else {
      setCurrentDocument(null);
      setIsEditing(false);
      setIsCreating(false);
    }
  }, [id]);

  // 카테고리 선택 시 문서 목록 로드
  useEffect(() => {
    if (selectedCategoryId) {
      setShowAllDocuments(false);
      loadDocumentsByCategory(selectedCategoryId);
    }
  }, [selectedCategoryId]);

  // 전체 문서 보기
  useEffect(() => {
    if (showAllDocuments) {
      loadAllDocuments();
    }
  }, [showAllDocuments]);

  // 문서 변경 시 임베딩 상태 조회 및 진행률 구독
  useEffect(() => {
    if (currentDocument && !isEditing) {
      loadEmbeddingStatus(currentDocument.id);
      // 진행 중인 임베딩이 있는지 확인하고 구독
      checkAndSubscribeProgress(currentDocument.id);
    } else {
      setEmbeddingStatus(null);
      setEmbeddingProgress(null);
    }
  }, [currentDocument, isEditing]);

  // 카테고리 트리를 평면 리스트로 변환 (드롭다운용)
  const flattenCategories = (cats: WikiCategory[], level = 0, isLast: boolean[] = []): WikiCategory[] => {
    const result: WikiCategory[] = [];
    cats.forEach((cat, index) => {
      const isLastItem = index === cats.length - 1;

      // 트리 구조 시각화
      let prefix = '';
      if (level > 0) {
        // 상위 레벨의 연결선 표시
        for (let i = 0; i < level - 1; i++) {
          prefix += isLast[i] ? '    ' : '│   ';
        }
        // 현재 항목 연결선
        prefix += isLastItem ? '└── ' : '├── ';
      }

      result.push({ ...cat, name: prefix + cat.name });

      if (cat.children && cat.children.length > 0) {
        result.push(...flattenCategories(cat.children, level + 1, [...isLast, isLastItem]));
      }
    });
    return result;
  };

  const loadCategories = async () => {
    try {
      const response = await wikiCategoryApi.getRoot();
      setCategories(response.data);
      // 평면화된 카테고리 목록도 생성
      setFlatCategories(flattenCategories(response.data));
    } catch (error) {
      console.error('카테고리 로드 실패:', error);
      alert('카테고리를 불러오는데 실패했습니다.');
    }
  };

  const loadDocument = async (documentId: number) => {
    try {
      setLoading(true);
      const response = await wikiDocumentApi.get(documentId);
      setCurrentDocument(response.data);
      setEditTitle(response.data.title);
      setEditContent(response.data.content);
      setEditCategoryId(response.data.categoryId);
      setEditSrs(response.data.srs || []);
    } catch (error) {
      console.error('문서 로드 실패:', error);
      alert('문서를 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const loadDocumentsByCategory = async (categoryId: number) => {
    try {
      const response = await wikiDocumentApi.getByCategory(categoryId, 0, 100);
      setDocuments(response.data.content);
    } catch (error) {
      console.error('문서 목록 로드 실패:', error);
    }
  };

  const loadAllDocuments = async () => {
    try {
      const response = await wikiDocumentApi.getAll(0, 100);
      setDocuments(response.data.content);
    } catch (error) {
      console.error('전체 문서 로드 실패:', error);
      alert('문서 목록을 불러오는데 실패했습니다.');
    }
  };

  const loadEmbeddingStatus = async (documentId: number) => {
    try {
      const status = await aiSearchService.getEmbeddingStatus(documentId);
      setEmbeddingStatus(status);
    } catch (error) {
      console.error('임베딩 상태 조회 실패:', error);
      setEmbeddingStatus(null);
    }
  };

  // 진행 중인 임베딩이 있는지 확인하고 SSE 구독
  const checkAndSubscribeProgress = async (documentId: number) => {
    try {
      const progress = await aiSearchService.getCurrentProgress(documentId);
      if (progress && (progress.status === 'STARTED' || progress.status === 'IN_PROGRESS')) {
        setEmbeddingProgress(progress);
        setIsGeneratingEmbedding(true);
        subscribeToProgress(documentId);
      } else {
        setEmbeddingProgress(null);
        setIsGeneratingEmbedding(false);
      }
    } catch (error) {
      console.error('진행률 조회 실패:', error);
    }
  };

  // 폴링으로 진행률 구독
  const subscribeToProgress = (documentId: number) => {
    const unsubscribe = aiSearchService.subscribeProgress(
      documentId,
      (event) => {
        setEmbeddingProgress(event);
        if (event.status === 'COMPLETED') {
          setIsGeneratingEmbedding(false);
          loadEmbeddingStatus(documentId);
        } else if (event.status === 'FAILED') {
          setIsGeneratingEmbedding(false);
          alert(`임베딩 생성 실패: ${event.message}`);
        }
      },
      () => {
        // 완료 시 진행률 상태 초기화 (약간의 딜레이 후)
        setTimeout(() => setEmbeddingProgress(null), 3000);
      },
      (error) => {
        // 에러 발생 시
        console.error('임베딩 생성 오류:', error);
        setIsGeneratingEmbedding(false);
        alert(error.message || '임베딩 생성 중 오류가 발생했습니다.');
      }
    );
    return unsubscribe;
  };

  const handleGenerateEmbedding = async () => {
    if (!currentDocument) return;

    if (!confirm('이 문서의 AI 검색용 임베딩을 생성하시겠습니까?\n(문서 길이에 따라 시간이 소요될 수 있습니다)')) {
      return;
    }

    try {
      setIsGeneratingEmbedding(true);
      // 비동기 임베딩 생성 시작
      await aiSearchService.generateEmbeddingsAsync(currentDocument.id);
      // SSE 구독 시작
      subscribeToProgress(currentDocument.id);
    } catch (error: any) {
      console.error('임베딩 생성 시작 실패:', error);
      alert(error.response?.data || '임베딩 생성 시작에 실패했습니다.');
      setIsGeneratingEmbedding(false);
    }
  };

  // 진행률 표시 포맷팅
  const formatTime = (ms?: number) => {
    if (!ms || ms <= 0) return '-';
    if (ms < 1000) return `${ms}ms`;
    const seconds = Math.floor(ms / 1000);
    if (seconds < 60) return `${seconds}초`;
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${minutes}분 ${remainingSeconds}초`;
  };

  const handleCreateDocument = () => {
    setIsCreating(true);
    setIsEditing(true);
    setCurrentDocument(null);
    setEditTitle('');
    setEditContent('');
    setEditCategoryId(selectedCategoryId);
    setEditSrs([]);
    navigate('/wiki');
  };

  const handleSaveDocument = async () => {
    if (!editTitle.trim()) {
      alert('제목을 입력해주세요.');
      return;
    }

    const request: WikiDocumentRequest = {
      title: editTitle,
      content: editContent,
      categoryId: editCategoryId,
      srIds: editSrs.map(sr => sr.id),
      generateToc: generateToc, // 목차 자동 생성 옵션 추가
    };

    try {
      setLoading(true);
      if (isCreating) {
        const response = await wikiDocumentApi.create(request);
        alert('문서가 생성되었습니다.');
        // 카테고리 트리 갱신 (문서 개수 업데이트)
        await loadCategories();
        navigate(`/wiki/${response.data.id}`);
        setIsCreating(false);
      } else if (currentDocument) {
        const oldCategoryId = currentDocument.categoryId;
        await wikiDocumentApi.update(currentDocument.id, request);
        alert('문서가 수정되었습니다.');
        // 카테고리가 변경되었으면 트리 갱신
        if (oldCategoryId !== editCategoryId) {
          await loadCategories();
        }
        await loadDocument(currentDocument.id);
      }
      setIsEditing(false);
      if (selectedCategoryId) {
        loadDocumentsByCategory(selectedCategoryId);
      } else if (showAllDocuments) {
        loadAllDocuments();
      }
    } catch (error) {
      console.error('문서 저장 실패:', error);
      alert('문서 저장에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteDocument = async () => {
    if (!currentDocument) return;

    if (!window.confirm('정말로 이 문서를 삭제하시겠습니까?')) {
      return;
    }

    try {
      await wikiDocumentApi.delete(currentDocument.id);
      alert('문서가 삭제되었습니다.');
      // 카테고리 트리 갱신 (문서 개수 업데이트)
      await loadCategories();
      navigate('/wiki');
      if (selectedCategoryId) {
        loadDocumentsByCategory(selectedCategoryId);
      } else if (showAllDocuments) {
        loadAllDocuments();
      }
    } catch (error) {
      console.error('문서 삭제 실패:', error);
      alert('문서 삭제에 실패했습니다.');
    }
  };

  const handleSearch = async () => {
    if (!searchKeyword.trim()) return;

    try {
      const response = await wikiDocumentApi.search(searchKeyword);
      setDocuments(response.data.content);
      setSelectedCategoryId(undefined);
      setShowAllDocuments(false);
    } catch (error) {
      console.error('검색 실패:', error);
      alert('검색에 실패했습니다.');
    }
  };

  // 카테고리 관리 핸들러
  const handleCreateCategory = (parentId?: number) => {
    setEditingCategory(null);
    setParentCategoryId(parentId);
    setShowCategoryModal(true);
  };

  const handleEditCategory = (category: WikiCategory) => {
    setEditingCategory(category);
    setParentCategoryId(category.parentId);
    setShowCategoryModal(true);
  };

  const handleDeleteCategory = async (categoryId: number) => {
    try {
      await wikiCategoryApi.delete(categoryId);
      alert('카테고리가 삭제되었습니다.');
      await loadCategories();
    } catch (error) {
      console.error('카테고리 삭제 실패:', error);
      alert('카테고리 삭제에 실패했습니다. 하위 카테고리나 문서가 있는지 확인해주세요.');
    }
  };

  const handleSubmitCategory = async (data: WikiCategoryRequest) => {
    try {
      if (editingCategory) {
        await wikiCategoryApi.update(editingCategory.id, data);
        alert('카테고리가 수정되었습니다.');
      } else {
        await wikiCategoryApi.create(data);
        alert('카테고리가 생성되었습니다.');
      }
      await loadCategories();
    } catch (error) {
      console.error('카테고리 저장 실패:', error);
      throw error;
    }
  };

  const handleShowAllDocuments = () => {
    setSelectedCategoryId(undefined);
    setShowAllDocuments(true);
    setSearchKeyword('');
  };

  return (
    <div className="wiki-page">
      <div className="wiki-sidebar">
        <div className="wiki-search">
          <input
            type="text"
            placeholder="문서 검색..."
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
          />
          <button onClick={handleSearch}>검색</button>
        </div>

        <WikiCategoryTree
          categories={categories}
          selectedCategoryId={selectedCategoryId}
          onCategorySelect={setSelectedCategoryId}
          onCategoryCreate={handleCreateCategory}
          onCategoryEdit={handleEditCategory}
          onCategoryDelete={handleDeleteCategory}
        />

        <div className="document-list">
          <button
            className={`btn-show-all ${showAllDocuments ? 'active' : ''}`}
            onClick={handleShowAllDocuments}
          >
            📄 전체 문서 보기
          </button>
        </div>

        {documents.length > 0 && (
          <div className="document-list">
            <h4>문서 목록</h4>
            {documents.map((doc) => (
              <div
                key={doc.id}
                className={`document-item ${currentDocument?.id === doc.id ? 'active' : ''}`}
                onClick={() => navigate(`/wiki/${doc.id}`)}
              >
                <div className="document-title">{doc.title}</div>
                <div className="document-meta">
                  조회수: {doc.viewCount} | {new Date(doc.updatedAt).toLocaleDateString()}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="wiki-content">
        <div className="wiki-toolbar">
          {!isEditing && !isCreating && (
            <>
              <button className="btn-primary" onClick={handleCreateDocument}>
                + 새 문서
              </button>
              <button className="btn-secondary" onClick={() => setShowPdfUpload(true)}>
                📄 PDF 업로드
              </button>
              {currentDocument && (
                <>
                  <button className="btn-secondary" onClick={() => setIsEditing(true)}>
                    편집
                  </button>
                  <button className="btn-secondary" onClick={() => setShowVersionHistory(true)}>
                    📜 버전 이력
                  </button>
                  <button
                    className="btn-secondary"
                    onClick={handleGenerateEmbedding}
                    disabled={isGeneratingEmbedding}
                  >
                    {isGeneratingEmbedding ? '⏳ 생성 중...' : '🤖 AI 임베딩 생성'}
                  </button>

                  {/* 진행률 표시 */}
                  {embeddingProgress && (embeddingProgress.status === 'STARTED' || embeddingProgress.status === 'IN_PROGRESS' || embeddingProgress.status === 'COMPLETED') && (
                    <div
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: '12px',
                        marginLeft: '12px',
                        padding: '8px 16px',
                        backgroundColor: embeddingProgress.status === 'COMPLETED' ? '#d4edda' : '#e3f2fd',
                        borderRadius: '8px',
                        border: `1px solid ${embeddingProgress.status === 'COMPLETED' ? '#28a745' : '#2196f3'}`,
                      }}
                    >
                      {embeddingProgress.status !== 'COMPLETED' && (
                        <div
                          style={{
                            width: '20px',
                            height: '20px',
                            border: '3px solid #e0e0e0',
                            borderTop: '3px solid #2196f3',
                            borderRadius: '50%',
                            animation: 'spin 1s linear infinite',
                          }}
                        />
                      )}
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                          <span style={{ fontWeight: '600', fontSize: '13px' }}>
                            {embeddingProgress.status === 'COMPLETED'
                              ? '✅ 임베딩 생성 완료'
                              : `⏳ ${embeddingProgress.currentChunk}/${embeddingProgress.totalChunks} 청크 처리 중`}
                          </span>
                          <span style={{
                            padding: '2px 8px',
                            backgroundColor: embeddingProgress.status === 'COMPLETED' ? '#28a745' : '#2196f3',
                            color: 'white',
                            borderRadius: '12px',
                            fontSize: '12px',
                            fontWeight: '700',
                          }}>
                            {embeddingProgress.progressPercent}%
                          </span>
                        </div>
                        {embeddingProgress.status !== 'COMPLETED' && (
                          <div style={{ display: 'flex', gap: '12px', fontSize: '11px', color: '#666' }}>
                            <span>경과: {formatTime(embeddingProgress.elapsedTimeMs)}</span>
                            <span>예상 남은 시간: {formatTime(embeddingProgress.estimatedRemainingMs)}</span>
                          </div>
                        )}
                        {/* 진행률 바 */}
                        <div style={{
                          width: '200px',
                          height: '6px',
                          backgroundColor: '#e0e0e0',
                          borderRadius: '3px',
                          overflow: 'hidden',
                        }}>
                          <div style={{
                            width: `${embeddingProgress.progressPercent}%`,
                            height: '100%',
                            backgroundColor: embeddingProgress.status === 'COMPLETED' ? '#28a745' : '#2196f3',
                            transition: 'width 0.3s ease',
                          }} />
                        </div>
                      </div>
                    </div>
                  )}

                  {/* 임베딩 상태 배지 (진행 중이 아닐 때만 표시) */}
                  {!embeddingProgress && embeddingStatus && (
                    <span
                      style={{
                        padding: '6px 12px',
                        borderRadius: '4px',
                        fontSize: '13px',
                        fontWeight: '600',
                        marginLeft: '8px',
                        backgroundColor: embeddingStatus.hasEmbedding
                          ? (embeddingStatus.isUpToDate ? '#d4edda' : '#fff3cd')
                          : '#f8d7da',
                        color: embeddingStatus.hasEmbedding
                          ? (embeddingStatus.isUpToDate ? '#155724' : '#856404')
                          : '#721c24',
                      }}
                    >
                      {embeddingStatus.hasEmbedding
                        ? (embeddingStatus.isUpToDate
                          ? `✅ AI 검색 준비됨 (${embeddingStatus.chunkCount}개 청크)`
                          : '⚠️ 임베딩 재생성 필요')
                        : '❌ 임베딩 없음'}
                    </span>
                  )}
                  <button className="btn-danger" onClick={handleDeleteDocument}>
                    삭제
                  </button>
                </>
              )}
            </>
          )}

          {(isEditing || isCreating) && (
            <>
              <label className="toc-checkbox-label" style={{ marginRight: '16px' }}>
                <input
                  type="checkbox"
                  checked={generateToc}
                  onChange={(e) => setGenerateToc(e.target.checked)}
                  style={{ marginRight: '6px' }}
                />
                📑 목차 자동 생성
              </label>
              <button className="btn-primary" onClick={handleSaveDocument} disabled={loading}>
                {loading ? '저장 중...' : '저장'}
              </button>
              <button
                className="btn-secondary"
                onClick={() => {
                  setIsEditing(false);
                  setIsCreating(false);
                  setGenerateToc(false); // 취소 시 목차 옵션 초기화
                  if (currentDocument) {
                    setEditTitle(currentDocument.title);
                    setEditContent(currentDocument.content);
                  }
                }}
              >
                취소
              </button>
            </>
          )}
        </div>

        {(isEditing || isCreating) ? (
          <div className="wiki-editor-container">
            <input
              type="text"
              className="document-title-input"
              placeholder="문서 제목"
              value={editTitle}
              onChange={(e) => setEditTitle(e.target.value)}
            />
            <div className="document-category-select">
              <label htmlFor="category-select">카테고리</label>
              <select
                id="category-select"
                value={editCategoryId || ''}
                onChange={(e) => setEditCategoryId(e.target.value ? parseInt(e.target.value) : undefined)}
              >
                <option value="">카테고리 없음</option>
                {flatCategories.map((cat) => (
                  <option key={cat.id} value={cat.id}>
                    {cat.name}
                  </option>
                ))}
              </select>
            </div>
            <SrSelector
              selectedSrs={editSrs}
              onChange={setEditSrs}
            />
            <WikiEditor
              initialValue={editContent}
              onChange={setEditContent}
              documentId={currentDocument?.id}
            />
          </div>
        ) : currentDocument ? (
          <div className="wiki-document">
            <h1>{currentDocument.title}</h1>
            <div className="document-info">
              <span>작성자: {currentDocument.createdByName}</span>
              <span>작성일: {new Date(currentDocument.createdAt).toLocaleString()}</span>
              <span>수정일: {new Date(currentDocument.updatedAt).toLocaleString()}</span>
              <span>조회수: {currentDocument.viewCount}</span>
            </div>

            {/* 연계된 SR 목록 */}
            {currentDocument.srs && currentDocument.srs.length > 0 && (
              <div className="linked-srs">
                <h3>연계된 SR</h3>
                <div className="linked-sr-list">
                  {currentDocument.srs.map(sr => (
                    <div
                      key={sr.id}
                      className="linked-sr-item"
                      onClick={() => setSelectedSrId(sr.id)}
                    >
                      <span className={`sr-status-badge status-${sr.status.toLowerCase().replace('_', '-')}`}>
                        {sr.status === 'OPEN' ? '접수' :
                         sr.status === 'IN_PROGRESS' ? '진행중' :
                         sr.status === 'RESOLVED' ? '해결' : '종료'}
                      </span>
                      <span className="linked-sr-title">{sr.title}</span>
                      <span className="link-arrow">→</span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            <WikiViewer content={currentDocument.content} files={currentDocument.files} />
          </div>
        ) : (
          <div className="wiki-empty">
            <AiSearchBox onDocumentClick={(documentId) => navigate(`/wiki/${documentId}`)} />
            <p style={{ marginTop: '20px', color: 'var(--text-secondary)' }}>
              또는 문서를 선택하거나 새 문서를 작성해주세요.
            </p>
          </div>
        )}
      </div>

      <WikiCategoryModal
        isOpen={showCategoryModal}
        onClose={() => setShowCategoryModal(false)}
        onSubmit={handleSubmitCategory}
        category={editingCategory}
        parentId={parentCategoryId}
      />

      {/* SR 상세 슬라이드 패널 */}
      <SrDetailPanel
        srId={selectedSrId}
        onClose={() => setSelectedSrId(null)}
      />

      {/* PDF 업로드 모달 */}
      <PdfUploadModal
        isOpen={showPdfUpload}
        onClose={() => setShowPdfUpload(false)}
        onUploadSuccess={(documentId) => {
          setShowPdfUpload(false);
          navigate(`/wiki/${documentId}`);
        }}
      />

      {/* 버전 이력 모달 */}
      {currentDocument && (
        <VersionHistoryModal
          isOpen={showVersionHistory}
          onClose={() => setShowVersionHistory(false)}
          documentId={currentDocument.id}
          onRollback={() => {
            // 롤백 후 문서 다시 로드
            if (currentDocument) {
              loadDocument(currentDocument.id);
            }
          }}
        />
      )}
    </div>
  );
};

export default WikiPage;
