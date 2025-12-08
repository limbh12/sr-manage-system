import { useEffect, useState, useRef, useCallback, useLayoutEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { OpenApiSurvey, OpenApiSurveySearch } from '../../types';
import * as surveyService from '../../services/surveyService';
import CsvUploadModal from './CsvUploadModal';

function SurveyList() {
  const navigate = useNavigate();
  const STORAGE_KEY = 'SURVEY_LIST_STATE';

  // Initialize state from sessionStorage
  const [savedState] = useState(() => {
    const saved = sessionStorage.getItem(STORAGE_KEY);
    return saved ? JSON.parse(saved) : null;
  });

  // 수정/등록 완료 플래그 확인
  const [formSubmitted] = useState(() => {
    const flag = sessionStorage.getItem('SURVEY_FORM_SUBMITTED');
    console.log('🔍 SurveyList 초기화 - formSubmitted 플래그:', flag);
    if (flag) {
      sessionStorage.removeItem('SURVEY_FORM_SUBMITTED');
      console.log('✅ formSubmitted = true, 최신 데이터를 로드합니다.');
      return true;
    }
    console.log('❌ formSubmitted = false, 캐시를 사용합니다.');
    return false;
  });

  const [surveys, setSurveys] = useState<OpenApiSurvey[]>(savedState?.surveys || []);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(savedState?.page || 0);
  const [pageSize] = useState(10);
  const [totalElements, setTotalElements] = useState(savedState?.totalElements || 0);
  const [hasMore, setHasMore] = useState(savedState?.hasMore || false);
  const [search, setSearch] = useState<OpenApiSurveySearch>(savedState?.search || {
    keyword: '',
    currentMethod: '',
    desiredMethod: '',
  });
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
  const [selectedId, setSelectedId] = useState<number | null>(savedState?.selectedId || null);
  const observerTarget = useRef<HTMLDivElement>(null);

  const loadSurveys = async (pageToLoad: number, searchParams: OpenApiSurveySearch, isReset: boolean = false, skipLoadingCheck: boolean = false) => {
    if (!skipLoadingCheck && loading) return;
    setLoading(true);
    try {
      const response = await surveyService.getSurveyList(pageToLoad, pageSize, searchParams);

      if (isReset) {
        setSurveys(response.content);
      } else {
        setSurveys(prev => [...prev, ...response.content]);
      }

      setTotalElements(response.totalElements);
      setHasMore(!response.last);
      setPage(pageToLoad);

      return response;
    } catch (error) {
      console.error(error);
      alert('목록을 불러오는데 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  const saveScrollPosition = (selectedId?: number) => {
    const scrollY = window.scrollY;
    const stateToSave = {
      surveys,
      page,
      totalElements,
      hasMore,
      search,
      scrollY,
      selectedId
    };
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(stateToSave));
  };

  // Scroll restoration
  useLayoutEffect(() => {
    if (!formSubmitted && savedState?.scrollY) {
      window.scrollTo(0, savedState.scrollY);
      sessionStorage.removeItem(STORAGE_KEY);
    }
  }, [savedState, formSubmitted]);

  // Disable browser scroll restoration
  useEffect(() => {
    if ('scrollRestoration' in history) {
      history.scrollRestoration = 'manual';
    }
    return () => {
      if ('scrollRestoration' in history) {
        history.scrollRestoration = 'auto';
      }
    };
  }, []);

  // Initial load
  useEffect(() => {
    if (formSubmitted) {
      // 수정/등록 후 돌아온 경우: 필요한 만큼 데이터를 로드
      const loadDataForScroll = async () => {
        const targetPage = savedState?.page || 0;
        const targetId = savedState?.selectedId;

        console.log('수정 완료 후 데이터 로드 시작:', { targetPage, targetId });

        // 페이지 0부터 targetPage까지 순차적으로 로드
        let allContent: any[] = [];
        for (let i = 0; i <= targetPage; i++) {
          console.log(`페이지 ${i} 로드 중...`);
          const response = await surveyService.getSurveyList(i, pageSize, search);
          if (i === 0) {
            allContent = response.content;
          } else {
            allContent = [...allContent, ...response.content];
          }

          // 상태 한 번에 업데이트
          setSurveys(allContent);
          setTotalElements(response.totalElements);
          setHasMore(!response.last);
          setPage(i);

          console.log(`페이지 ${i} 로드 완료, 총 ${allContent.length}개 항목`);
        }

        // selectedId 복원
        if (targetId) {
          setSelectedId(targetId);
          console.log('selectedId 복원:', targetId);

          // DOM이 완전히 렌더링될 때까지 대기 후 스크롤
          setTimeout(() => {
            const selectedRow = document.querySelector(`tr[data-survey-id="${targetId}"]`);
            console.log('스크롤 대상 찾기:', selectedRow ? '성공' : '실패');
            if (selectedRow) {
              selectedRow.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }
          }, 300);
        }

        // 캐시 정리
        sessionStorage.removeItem(STORAGE_KEY);
      };

      loadDataForScroll();
    } else if (!savedState) {
      // 캐시가 없는 경우: 초기 로드
      loadSurveys(0, search, true);
    }
  }, []);

  const handleObserver = useCallback((entries: IntersectionObserverEntry[]) => {
    const target = entries[0];
    if (target.isIntersecting && hasMore && !loading) {
      loadSurveys(page + 1, search, false);
    }
  }, [hasMore, loading, page, search]);

  useEffect(() => {
    const option = {
      root: null,
      rootMargin: "20px",
      threshold: 0
    };
    const observer = new IntersectionObserver(handleObserver, option);
    if (observerTarget.current) observer.observe(observerTarget.current);
    
    return () => {
      if (observerTarget.current) observer.unobserve(observerTarget.current);
    }
  }, [handleObserver]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    loadSurveys(0, search, true);
  };

  const handleReset = () => {
    const resetSearch: OpenApiSurveySearch = { keyword: '', currentMethod: '', desiredMethod: '' };
    setSearch(resetSearch);
    setPage(0);
    loadSurveys(0, resetSearch, true);
  };

  const handleDownloadTemplate = () => {
    const link = document.createElement('a');
    link.href = '/templates/openapi_survey_template.csv';
    link.download = 'openapi_survey_template.csv';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const handleUploadSuccess = () => {
    setPage(0);
    loadSurveys(0, search, true);
  };

  return (
    <div>
      <div className="page-header">
        <h2 className="page-title">OPEN API 현황조사 목록</h2>
        <div style={{ display: 'flex', gap: '8px' }}>
          <button onClick={handleDownloadTemplate} className="btn btn-secondary">
            템플릿 다운로드
          </button>
          <button onClick={() => setIsUploadModalOpen(true)} className="btn btn-secondary">
            일괄 등록
          </button>
          <button onClick={() => navigate('/survey/new')} className="btn btn-primary">
            신규 등록
          </button>
        </div>
      </div>

      {/* 검색 필터 */}
      <div className="card mb-4" style={{ padding: '16px' }}>
        <form onSubmit={handleSearch} className="grid-2" style={{ alignItems: 'end', gap: '16px' }}>
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label className="form-label">검색어</label>
            <input
              type="text"
              className="form-input"
              placeholder="기관명, 부서, 시스템명 검색"
              value={search.keyword}
              onChange={(e) => setSearch({ ...search, keyword: e.target.value })}
            />
          </div>
          <div className="grid-2" style={{ gap: '16px' }}>
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label">현재방식</label>
              <select
                className="form-select"
                value={search.currentMethod}
                onChange={(e) => setSearch({ ...search, currentMethod: e.target.value as any })}
              >
                <option value="">전체</option>
                <option value="CENTRAL">중앙형</option>
                <option value="DISTRIBUTED">분산형</option>
                <option value="NO_RESPONSE">미회신</option>
              </select>
            </div>
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label">희망방식</label>
              <select
                className="form-select"
                value={search.desiredMethod}
                onChange={(e) => setSearch({ ...search, desiredMethod: e.target.value as any })}
              >
                <option value="">전체</option>
                <option value="CENTRAL_IMPROVED">중앙개선형</option>
                <option value="DISTRIBUTED_IMPROVED">분산개선형</option>
                <option value="NO_RESPONSE">미회신</option>
              </select>
            </div>
          </div>
          <div className="flex-end" style={{ gridColumn: '1 / -1', marginTop: '8px' }}>
            <button type="button" onClick={handleReset} className="btn btn-secondary">초기화</button>
            <button type="submit" className="btn btn-primary">검색</button>
          </div>
        </form>
      </div>

      <div className="card table-container">
        <table className="table">
          <thead>
            <tr>
              <th>No</th>
              <th>기관명</th>
              <th>부서</th>
              <th>담당자</th>
              <th>시스템명</th>
              <th>현재방식</th>
              <th>희망방식</th>
              <th>등록일</th>
              <th>관리</th>
            </tr>
          </thead>
          <tbody>
            {surveys.length === 0 && !loading ? (
              <tr>
                <td colSpan={9} style={{ textAlign: 'center', padding: '40px', color: '#666' }}>
                  등록된 데이터가 없습니다.
                </td>
              </tr>
            ) : (
              surveys.map((survey, index) => (
                <tr
                  key={survey.id}
                  data-survey-id={survey.id}
                  className={selectedId === survey.id ? 'bg-highlight' : ''}
                >
                  <td>{totalElements - index}</td>
                  <td>{survey.organization.name}</td>
                  <td>{survey.department}</td>
                  <td>{survey.contactName}</td>
                  <td>{survey.systemName}</td>
                  <td>
                    {survey.currentMethod === 'CENTRAL' ? '중앙형' : 
                      survey.currentMethod === 'DISTRIBUTED' ? '분산형' : 
                      survey.currentMethod === 'NO_RESPONSE' ? '미회신' : survey.currentMethod}
                  </td>
                  <td>
                    {survey.desiredMethod === 'CENTRAL_IMPROVED' ? '중앙개선형' : 
                      survey.desiredMethod === 'DISTRIBUTED_IMPROVED' ? '분산개선형' : 
                      survey.desiredMethod === 'NO_RESPONSE' ? '미회신' : survey.desiredMethod}
                  </td>
                  <td>{new Date(survey.createdAt).toLocaleDateString()}</td>
                  <td>
                    <button 
                      className="btn btn-sm btn-secondary"
                      onClick={() => {
                        saveScrollPosition(survey.id);
                        navigate(`/survey/${survey.id}`);
                      }}
                    >
                      상세
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
        
        {/* Infinite Scroll Sentinel */}
        <div ref={observerTarget} style={{ height: '20px', margin: '10px 0', textAlign: 'center' }}>
          {loading && <div className="loading-spinner" style={{ display: 'inline-block', width: '24px', height: '24px', border: '3px solid #f3f3f3', borderTop: '3px solid #3498db' }}></div>}
        </div>
      </div>

      <CsvUploadModal
        isOpen={isUploadModalOpen}
        onClose={() => setIsUploadModalOpen(false)}
        onSuccess={handleUploadSuccess}
      />
    </div>
  );
}

export default SurveyList;
