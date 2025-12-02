import { useEffect, useState } from 'react';
import { useSr } from '../hooks/useSr';
import SrList from '../components/sr/SrList';
import SrDetail from '../components/sr/SrDetail';
import SrForm from '../components/sr/SrForm';
import Loading from '../components/common/Loading';
import { Sr, SrCreateRequest, SrUpdateRequest, SrStatus, Priority } from '../types';

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

  // 초기 로딩 및 페이지 변경 시에만 실행
  useEffect(() => {
    fetchSrList({
      page: currentPage,
      status: statusFilter || undefined,
      priority: priorityFilter || undefined,
      search: searchQuery || undefined,
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [fetchSrList, currentPage]);

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
      // 목록 갱신
      handleSearch();
    }
  };

  const handleDelete = async (id: number) => {
    if (window.confirm('정말로 이 SR을 삭제하시겠습니까?')) {
      const success = await deleteSr(id);
      if (success) {
        // 목록 갱신
        fetchSrList({
          page: currentPage,
          status: statusFilter || undefined,
          priority: priorityFilter || undefined,
          search: searchQuery || undefined,
        });
      }
    }
  };

  const handleStatusChange = async (status: SrStatus) => {
    if (currentSr) {
      const success = await updateSrStatus(currentSr.id, { status });
      if (success) {
        // 목록 갱신
        fetchSrList({
          page: currentPage,
          status: statusFilter || undefined,
          priority: priorityFilter || undefined,
          search: searchQuery || undefined,
        });
      }
    }
  };

  const handlePageChange = (page: number) => {
    fetchSrList({
      page,
      status: statusFilter || undefined,
      priority: priorityFilter || undefined,
      search: searchQuery || undefined,
    });
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
        {loading ? (
          <Loading />
        ) : (
          <SrList
            srList={srList}
            onSelectSr={handleSelectSr}
            onDeleteSr={handleDelete}
            totalElements={totalElements}
            page={currentPage}
          />
        )}

        {/* 페이지네이션 */}
        {totalPages > 1 && (
          <div className="pagination">
            <button
              className="pagination-btn"
              onClick={() => handlePageChange(currentPage - 1)}
              disabled={currentPage === 0}
            >
              이전
            </button>
            {Array.from({ length: totalPages }, (_, i) => (
              <button
                key={i}
                className={`pagination-btn ${currentPage === i ? 'active' : ''}`}
                onClick={() => handlePageChange(i)}
              >
                {i + 1}
              </button>
            ))}
            <button
              className="pagination-btn"
              onClick={() => handlePageChange(currentPage + 1)}
              disabled={currentPage === totalPages - 1}
            >
              다음
            </button>
          </div>
        )}
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
