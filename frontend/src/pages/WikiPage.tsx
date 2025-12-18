import React, { useState, useEffect } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import WikiCategoryTree from '../components/wiki/WikiCategoryTree';
import WikiEditor from '../components/wiki/WikiEditor';
import WikiViewer from '../components/wiki/WikiViewer';
import {
  wikiDocumentApi,
  wikiCategoryApi,
} from '../services/wikiService';
import type { WikiDocument, WikiCategory, WikiDocumentRequest } from '../types/wiki';
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
      loadDocumentsByCategory(selectedCategoryId);
    }
  }, [selectedCategoryId]);

  const loadCategories = async () => {
    try {
      const response = await wikiCategoryApi.getRoot();
      setCategories(response.data);
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

  const handleCreateDocument = () => {
    setIsCreating(true);
    setIsEditing(true);
    setCurrentDocument(null);
    setEditTitle('');
    setEditContent('');
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
      categoryId: selectedCategoryId,
    };

    try {
      setLoading(true);
      if (isCreating) {
        const response = await wikiDocumentApi.create(request);
        alert('문서가 생성되었습니다.');
        navigate(`/wiki/${response.data.id}`);
        setIsCreating(false);
      } else if (currentDocument) {
        await wikiDocumentApi.update(currentDocument.id, request);
        alert('문서가 수정되었습니다.');
        await loadDocument(currentDocument.id);
      }
      setIsEditing(false);
      if (selectedCategoryId) {
        loadDocumentsByCategory(selectedCategoryId);
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
      navigate('/wiki');
      if (selectedCategoryId) {
        loadDocumentsByCategory(selectedCategoryId);
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
    } catch (error) {
      console.error('검색 실패:', error);
      alert('검색에 실패했습니다.');
    }
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
        />

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
              {currentDocument && (
                <>
                  <button className="btn-secondary" onClick={() => setIsEditing(true)}>
                    편집
                  </button>
                  <button className="btn-danger" onClick={handleDeleteDocument}>
                    삭제
                  </button>
                </>
              )}
            </>
          )}

          {(isEditing || isCreating) && (
            <>
              <button className="btn-primary" onClick={handleSaveDocument} disabled={loading}>
                {loading ? '저장 중...' : '저장'}
              </button>
              <button
                className="btn-secondary"
                onClick={() => {
                  setIsEditing(false);
                  setIsCreating(false);
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
            <WikiViewer content={currentDocument.content} />
          </div>
        ) : (
          <div className="wiki-empty">
            <p>문서를 선택하거나 새 문서를 작성해주세요.</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default WikiPage;
