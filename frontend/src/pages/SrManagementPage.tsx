import { useEffect, useState, useRef, useCallback } from 'react';
import { useSr } from '../hooks/useSr';
import SrList from '../components/sr/SrList';
import SrDetail from '../components/sr/SrDetail';
import SrForm from '../components/sr/SrForm';
import Loading from '../components/common/Loading';
import { Sr, SrCreateRequest, SrUpdateRequest, SrStatus, Priority } from '../types';
import { USE_MOCK } from '../config';

/**
 * SR 관리 페이지
 */
function SrManagementPage() {
  const {
    srList,
    currentSr,
    totalElements,
    totalPages,
    currentPage,
    loading,
    fetchSrList,
    createSr,
    updateSr,
    deleteSr,
    updateSrStatus,
    selectSr,
  } = useSr();

  const [showForm, setShowForm] = useState(false);
  const [showDetail, setShowDetail] = useState(false);
  const [editMode, setEditMode] = useState(false);
  const [statusFilter, setStatusFilter] = useState<SrStatus | ''>('');
  const [priorityFilter, setPriorityFilter] = useState<Priority | ''>('');
  const [searchQuery, setSearchQuery] = useState('');
  const observerTarget = useRef<HTMLDivElement>(null);

  // 초기 로딩
  useEffect(() => {
    fetchSrList({
      page: 0,
      status: statusFilter || undefined,
      priority: priorityFilter || undefined,
      search: searchQuery || undefined,
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleObserver = useCallback((entries: IntersectionObserverEntry[]) => {
    const target = entries[0];
    if (target.isIntersecting && !loading && currentPage < totalPages - 1) {
      fetchSrList({
        page: currentPage + 1,
        status: statusFilter || undefined,
        priority: priorityFilter || undefined,
        search: searchQuery || undefined,
      });
    }
  }, [loading, currentPage, totalPages, fetchSrList, statusFilter, priorityFilter, searchQuery]);

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

  const handleSelectSr = (sr: Sr) => {
    selectSr(sr);
    setShowDetail(true);
    setEditMode(false);
  };

  const handleCloseDetail = () => {
    setShowDetail(false);
    selectSr(null);
  };

  const handleEdit = () => {
    setEditMode(true);
    setShowDetail(false);
    setShowForm(true);
  };

  const handleCreate = () => {
    selectSr(null);
    setEditMode(false);
    setShowForm(true);
  };

  const handleSubmit = async (data: SrCreateRequest | SrUpdateRequest) => {
    let success = false;
    
    if (editMode && currentSr) {
      success = await updateSr(currentSr.id, data as SrUpdateRequest);
    } else {
      success = await createSr(data as SrCreateRequest);
    }
    
    if (success) {
      setShowForm(false);
      selectSr(null);
      // 목록 갱신 (첫 페이지부터 다시)
      handleSearch();
    }
  };

  const handleDelete = async (id: number) => {
    if (window.confirm('정말로 이 SR을 삭제하시겠습니까?')) {
      const success = await deleteSr(id);
      if (success) {
        // 목록 갱신 (현재 상태 유지하되, 삭제된 항목이 빠지므로 리로드 필요할 수 있음)
        // 간단하게 첫 페이지부터 다시 로드
        handleSearch();
      }
    }
  };

  const handleStatusChange = async (status: SrStatus) => {
    if (currentSr) {
      const success = await updateSrStatus(currentSr.id, { status });
      if (success) {
        // 목록 갱신
        handleSearch();
      }
    }
  };

  const handleSearch = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    fetchSrList({
      page: 0,
      status: statusFilter || undefined,
      priority: priorityFilter || undefined,
      search: searchQuery || undefined,
    });
  };

  const handleReset = () => {
    setStatusFilter('');
    setPriorityFilter('');
    setSearchQuery('');
    fetchSrList({
      page: 0,
      status: undefined,
      priority: undefined,
      search: undefined,
    });
  };

  return (
    <div>
      <div className="page-header">
        <h2 className="page-title">SR 관리</h2>
        <div style={{ display: 'flex', gap: '8px' }}>
          {USE_MOCK && (
            <button 
              className="btn btn-danger" 
              onClick={() => {
                if (window.confirm('로컬 스토리지의 모든 데이터(SR 목록, 로그인 정보 등)를 초기화하시겠습니까?\n페이지가 새로고침됩니다.')) {
                  localStorage.clear();
                  window.location.reload();
                }
              }}
            >
              데이터 초기화
            </button>
          )}
          <button className="btn btn-primary" onClick={handleCreate}>
            + SR 등록
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
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="제목 또는 설명 검색"
            />
          </div>
          
          <div className="grid-2" style={{ gap: '16px' }}>
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label">상태</label>
              <select
                className="form-select"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value as SrStatus | '')}
              >
                <option value="">전체</option>
                <option value="OPEN">신규</option>
                <option value="IN_PROGRESS">처리중</option>
                <option value="RESOLVED">해결됨</option>
                <option value="CLOSED">종료</option>
              </select>
            </div>
            
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label">우선순위</label>
              <select
                className="form-select"
                value={priorityFilter}
                onChange={(e) => setPriorityFilter(e.target.value as Priority | '')}
              >
                <option value="">전체</option>
                <option value="LOW">낮음</option>
                <option value="MEDIUM">보통</option>
                <option value="HIGH">높음</option>
                <option value="CRITICAL">긴급</option>
              </select>
            </div>
          </div>

          <div className="flex-end" style={{ gridColumn: '1 / -1', marginTop: '8px' }}>
            <button type="button" onClick={handleReset} className="btn btn-secondary">초기화</button>
            <button type="submit" className="btn btn-primary">검색</button>
          </div>
        </form>
      </div>

      {/* SR 목록 */}
      <div className="card">
        <SrList
          srList={srList}
          onSelectSr={handleSelectSr}
          onDeleteSr={handleDelete}
          totalElements={totalElements}
          page={currentPage}
        />
        
        {/* Infinite Scroll Sentinel */}
        <div ref={observerTarget} style={{ height: '20px', margin: '10px 0', textAlign: 'center' }}>
          {loading && <div className="loading-spinner" style={{ display: 'inline-block', width: '24px', height: '24px', border: '3px solid #f3f3f3', borderTop: '3px solid #3498db' }}></div>}
        </div>
      </div>

      {/* SR 상세 모달 */}
      {showDetail && currentSr && (
        <SrDetail
          sr={currentSr}
          onClose={handleCloseDetail}
          onEdit={handleEdit}
          onStatusChange={handleStatusChange}
        />
      )}

      {/* SR 폼 모달 */}
      {showForm && (
        <SrForm
          sr={editMode ? currentSr : null}
          onSubmit={handleSubmit}
          onCancel={() => {
            setShowForm(false);
            selectSr(null);
          }}
          loading={loading}
        />
      )}
    </div>
  );
}

export default SrManagementPage;
