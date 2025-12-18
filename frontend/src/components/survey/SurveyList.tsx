import { useEffect, useState, useRef, useCallback, useLayoutEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { OpenApiSurvey, OpenApiSurveySearch } from '../../types';
import * as surveyService from '../../services/surveyService';
import CsvUploadModal from './CsvUploadModal';

function SurveyList() {
  const navigate = useNavigate();
  const STORAGE_KEY = 'SURVEY_LIST_STATE';
  // Initialize state from sessionStorage (ignore if too old)
  const CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes
  const [savedState] = useState(() => {
    const saved = sessionStorage.getItem(STORAGE_KEY);
    if (!saved) return null;
    try {
      const parsed = JSON.parse(saved);
      if (parsed.savedAt && typeof parsed.savedAt === 'number') {
        const age = Date.now() - parsed.savedAt;
        if (age > CACHE_TTL_MS) {
          sessionStorage.removeItem(STORAGE_KEY);
          return null;
        }
      }
      return parsed;
    } catch (e) {
      console.error('Failed to parse saved survey state', e);
      sessionStorage.removeItem(STORAGE_KEY);
      return null;
    }
  });

  // 수정/등록 완료 플래그 확인
  const [formSubmitted] = useState(() => {
    const flag = sessionStorage.getItem('SURVEY_FORM_SUBMITTED');
    if (flag) {
      sessionStorage.removeItem('SURVEY_FORM_SUBMITTED');
      return true;
    }
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
      // timestamp for TTL-based invalidation
      savedAt: Date.now(),
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

  const location = useLocation();

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

        // 페이지 0부터 targetPage까지 순차적으로 로드
        let allContent: any[] = [];
        for (let i = 0; i <= targetPage; i++) {
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

          
        }

        // selectedId 복원
        if (targetId) {
          setSelectedId(targetId);

          // DOM이 완전히 렌더링될 때까지 대기 후 스크롤
          setTimeout(() => {
            const selectedRow = document.querySelector(`tr[data-survey-id="${targetId}"]`);
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

    // If we have a savedState but formSubmitted is false, validate cache in background
    // (This ensures external changes are detected and UI updated even when cached state exists)
    if (savedState && !formSubmitted) {
      const validateCache = async () => {
        try {
          const fresh = await surveyService.getSurveyList(0, pageSize, search);
          // If totalElements or first item differs, refresh displayed list
          const cachedFirstId = (savedState.surveys && savedState.surveys[0] && savedState.surveys[0].id) || null;
          const freshFirstId = (fresh.content && fresh.content[0] && fresh.content[0].id) || null;
          if (fresh.totalElements !== (savedState.totalElements || 0) || cachedFirstId !== freshFirstId) {
            setSurveys(fresh.content);
            setTotalElements(fresh.totalElements);
            setHasMore(!fresh.last);
            setPage(0);
            // clear saved cache to avoid stale restores
            sessionStorage.removeItem(STORAGE_KEY);
          } else {
            // cache is up-to-date
          }
        } catch (err) {
          console.error('캐시 검증 중 오류', err);
        }
      };

      validateCache();
    }
  }, []);

  // Support navigation with state: navigate('/survey', { state: { formSubmitted: true, selectedId } })
  useEffect(() => {
    try {
      // 이미 sessionStorage 기반으로 formSubmitted가 감지되어 로드가 진행중이면
      // 라우트 상태에 의한 추가 로드를 방지합니다 (중복 로드 차단).
      if (formSubmitted) return;

      const state = (location && (location as any).state) || null;
      if (state && state.formSubmitted) {
        const targetPage = savedState?.page || 0;
        const targetId = state.selectedId || savedState?.selectedId;

        const loadDataForScrollFromLocation = async () => {
          
          let allContent: any[] = [];
          for (let i = 0; i <= targetPage; i++) {
            const response = await surveyService.getSurveyList(i, pageSize, search);
            if (i === 0) {
              allContent = response.content;
            } else {
              allContent = [...allContent, ...response.content];
            }

            setSurveys(allContent);
            setTotalElements(response.totalElements);
            setHasMore(!response.last);
            setPage(i);
          }

          if (targetId) {
            setSelectedId(targetId);
            setTimeout(() => {
              const selectedRow = document.querySelector(`tr[data-survey-id="${targetId}"]`);
              if (selectedRow) selectedRow.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }, 300);
          }

          // clear saved cache and clear navigation state so subsequent mounts are clean
          sessionStorage.removeItem(STORAGE_KEY);
          // replace history state to avoid re-triggering
          navigate(location.pathname, { replace: true, state: {} });
        };

        loadDataForScrollFromLocation();
      }
    } catch (err) {
      console.error('라우트 상태 기반 로드 중 오류', err);
    }
  // location.key changes on navigation; run when location changes
  }, [location.key, formSubmitted]);

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

  const handleExportToCSV = () => {
    if (surveys.length === 0) {
      alert('내보낼 데이터가 없습니다.');
      return;
    }

    // CSV 헤더 (템플릿과 동일한 순서)
    const headers = [
      '기관명', '부서', '담당자명', '연락처', '이메일', '수신파일명', '수신일자',
      '시스템명', '운영상태', '현행방식', '희망방식', '분산형희망사유',
      '유지관리운영', '유지관리장소', '유지관리주소', '유지관리비고',
      '운영환경', '서버위치',
      'WEB서버OS', 'WEB서버OS종류', 'WEB서버OS버전', 'WEB서버종류', 'WEB서버종류기타', 'WEB서버버전',
      'WAS서버OS', 'WAS서버OS종류', 'WAS서버OS버전', 'WAS서버종류', 'WAS서버종류기타', 'WAS서버버전',
      'DB서버OS', 'DB서버OS종류', 'DB서버OS버전', 'DB서버종류', 'DB서버종류기타', 'DB서버버전',
      '개발언어', '개발언어기타', '개발언어버전', '개발프레임워크', '개발프레임워크기타', '개발프레임워크버전',
      '기타요청사항', '비고'
    ];

    // CSV 데이터 생성
    const rows = surveys.map(survey => [
      survey.organization?.name || '',
      survey.department || '',
      survey.contactName || '',
      survey.contactPhone || '',
      survey.contactEmail || '',
      survey.receivedFileName || '',
      survey.receivedDate || '',
      survey.systemName || '',
      survey.operationStatus || 'OPERATING',  // 운영상태 추가
      survey.currentMethod || '',
      survey.desiredMethod || '',
      survey.reasonForDistributed || '',
      survey.maintenanceOperation || '',
      survey.maintenanceLocation || '',
      survey.maintenanceAddress || '',
      survey.maintenanceNote || '',
      survey.operationEnv || '',
      survey.serverLocation || '',
      survey.webServerOs || '',
      survey.webServerOsType || '',
      survey.webServerOsVersion || '',
      survey.webServerType || '',
      survey.webServerTypeOther || '',
      survey.webServerVersion || '',
      survey.wasServerOs || '',
      survey.wasServerOsType || '',
      survey.wasServerOsVersion || '',
      survey.wasServerType || '',
      survey.wasServerTypeOther || '',
      survey.wasServerVersion || '',
      survey.dbServerOs || '',
      survey.dbServerOsType || '',
      survey.dbServerOsVersion || '',
      survey.dbServerType || '',
      survey.dbServerTypeOther || '',
      survey.dbServerVersion || '',
      survey.devLanguage || '',
      survey.devLanguageOther || '',
      survey.devLanguageVersion || '',
      survey.devFramework || '',
      survey.devFrameworkOther || '',
      survey.devFrameworkVersion || '',
      survey.otherRequests || '',
      survey.note || ''
    ]);

    // CSV 문자열 생성 (BOM 추가로 한글 깨짐 방지)
    const csvContent = '\uFEFF' + [
      headers.join(','),
      ...rows.map(row => row.map(cell => {
        // 쉼표, 따옴표, 줄바꿈이 포함된 경우 따옴표로 감싸기
        const cellStr = String(cell);
        if (cellStr.includes(',') || cellStr.includes('"') || cellStr.includes('\n')) {
          return `"${cellStr.replace(/"/g, '""')}"`;
        }
        return cellStr;
      }).join(','))
    ].join('\n');

    // Blob 생성 및 다운로드
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    const timestamp = new Date().toISOString().split('T')[0];

    link.href = url;
    link.download = `openapi_survey_result_${timestamp}.csv`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
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
          <button onClick={handleExportToCSV} className="btn btn-secondary">
            엑셀 내보내기
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
              placeholder="기관명, 부서, 담당자, 시스템명 검색"
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
