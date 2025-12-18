import { useEffect, useState, useRef, useCallback } from 'react';
import { useSr } from '../hooks/useSr';
import { useAuth } from '../hooks/useAuth';
import SrList from '../components/sr/SrList';
import SrDetail from '../components/sr/SrDetail';
import SrDetailPanel from '../components/sr/SrDetailPanel';
import SrForm from '../components/sr/SrForm';
import WikiDetailPanel from '../components/wiki/WikiDetailPanel';
import { Sr, SrCreateRequest, SrUpdateRequest, SrStatus, Priority, CommonCode, User } from '../types';
import { USE_MOCK } from '../config';
import { commonCodeService } from '../services/commonCodeService';
import * as userService from '../services/userService';

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
    restoreSr,
    updateSrStatus,
    selectSr,
  } = useSr();

  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';

  const [showForm, setShowForm] = useState(false);
  const [showDetail, setShowDetail] = useState(false);
  const [editMode, setEditMode] = useState(false);
  const [statusFilter, setStatusFilter] = useState<SrStatus | ''>('');
  const [priorityFilter, setPriorityFilter] = useState<Priority | ''>('');
  const [categoryFilter, setCategoryFilter] = useState('');
  const [requestTypeFilter, setRequestTypeFilter] = useState('');
  const [assigneeFilter, setAssigneeFilter] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [includeDeleted, setIncludeDeleted] = useState(false);
  const observerTarget = useRef<HTMLDivElement>(null);

  // 옵션 데이터
  const [categoryOptions, setCategoryOptions] = useState<CommonCode[]>([]);
  const [requestTypeOptions, setRequestTypeOptions] = useState<CommonCode[]>([]);
  const [userOptions, setUserOptions] = useState<User[]>([]);

  // Wiki 패널
  const [selectedWikiId, setSelectedWikiId] = useState<number | null>(null);
  // Wiki에서 SR 클릭 시 슬라이드 패널로 표시하기 위한 state
  const [wikiLinkedSrId, setWikiLinkedSrId] = useState<number | null>(null);

  // 디버깅용: user와 isAdmin 확인
  useEffect(() => {
    console.log('User:', user);
    console.log('Is Admin:', isAdmin);
  }, [user, isAdmin]);

  // 옵션 데이터 로드
  useEffect(() => {
    commonCodeService.getActiveCodesByGroup('SR_CATEGORY').then(setCategoryOptions).catch(console.error);
    commonCodeService.getActiveCodesByGroup('SR_REQUEST_TYPE').then(setRequestTypeOptions).catch(console.error);
    userService.getUserOptions().then(setUserOptions).catch(console.error);
  }, []);

  // 초기 로딩
  useEffect(() => {
    fetchSrList({
      page: 0,
      status: statusFilter || undefined,
      priority: priorityFilter || undefined,
      search: searchQuery || undefined,
      includeDeleted: isAdmin ? includeDeleted : undefined,
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
        category: categoryFilter || undefined,
        requestType: requestTypeFilter || undefined,
        assigneeId: assigneeFilter ? Number(assigneeFilter) : undefined,
        search: searchQuery || undefined,
        includeDeleted: isAdmin ? includeDeleted : undefined,
      });
    }
  }, [loading, currentPage, totalPages, fetchSrList, statusFilter, priorityFilter, categoryFilter, requestTypeFilter, assigneeFilter, searchQuery, isAdmin, includeDeleted]);

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

  const handleRestore = async (id: number) => {
    if (window.confirm('이 SR을 복구하시겠습니까?')) {
      const success = await restoreSr(id);
      if (success) {
        // 목록 갱신
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
      category: categoryFilter || undefined,
      requestType: requestTypeFilter || undefined,
      assigneeId: assigneeFilter ? Number(assigneeFilter) : undefined,
      search: searchQuery || undefined,
      includeDeleted: isAdmin ? includeDeleted : undefined,
    });
  };

  const handleReset = () => {
    setStatusFilter('');
    setPriorityFilter('');
    setCategoryFilter('');
    setRequestTypeFilter('');
    setAssigneeFilter('');
    setSearchQuery('');
    setIncludeDeleted(false);
    fetchSrList({
      page: 0,
      status: undefined,
      priority: undefined,
      category: undefined,
      requestType: undefined,
      assigneeId: undefined,
      search: undefined,
      includeDeleted: undefined,
    });
  };

  return (
    <div>
      <div className="page-header">
        <div>
          <h2 className="page-title">SR 관리</h2>
          {user && (
            <div style={{ fontSize: '12px', color: '#666', marginTop: '4px' }}>
              현재 사용자: {user.name} ({user.username}) - 권한: {user.role}
            </div>
          )}
        </div>
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
        <form onSubmit={handleSearch} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {/* 모든 필터를 한 줄로 배치 */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 2fr 1fr 1fr 1fr', gap: '12px', alignItems: 'end' }}>
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label">분류</label>
              <select
                className="form-select"
                value={categoryFilter}
                onChange={(e) => setCategoryFilter(e.target.value)}
              >
                <option value="">전체</option>
                {categoryOptions.map((option) => (
                  <option key={option.id} value={option.codeValue}>
                    {option.codeName}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label">요청구분</label>
              <select
                className="form-select"
                value={requestTypeFilter}
                onChange={(e) => setRequestTypeFilter(e.target.value)}
              >
                <option value="">전체</option>
                {requestTypeOptions.map((option) => (
                  <option key={option.id} value={option.codeValue}>
                    {option.codeName}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label">검색어</label>
              <input
                type="text"
                className="form-input"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="SR ID, 제목, 설명, 요청자명, 전화번호 검색"
              />
            </div>

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

            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label">담당자</label>
              <select
                className="form-select"
                value={assigneeFilter}
                onChange={(e) => setAssigneeFilter(e.target.value)}
              >
                <option value="">전체</option>
                {userOptions.map((u) => (
                  <option key={u.id} value={u.id}>
                    {u.name} ({u.email})
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* 검색 설명 및 버튼 영역 */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div style={{ fontSize: '11px', color: '#666' }}>
              * SR ID, 제목, 설명(요청사항), 요청자명, 전화번호로 검색 가능 (분류/요청구분/담당자는 select로 선택)
            </div>
            <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
              {/* 삭제된 항목 포함 체크박스 (관리자 전용) */}
              {isAdmin && (
                <label style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  cursor: 'pointer',
                  marginBottom: 0
                }}>
                  <input
                    type="checkbox"
                    checked={includeDeleted}
                    onChange={(e) => {
                      const checked = e.target.checked;
                      setIncludeDeleted(checked);
                      // 체크박스 변경 시 즉시 목록 갱신
                      fetchSrList({
                        page: 0,
                        status: statusFilter || undefined,
                        priority: priorityFilter || undefined,
                        search: searchQuery || undefined,
                        includeDeleted: checked,
                      });
                    }}
                    style={{ cursor: 'pointer' }}
                  />
                  <span>삭제된 항목 포함</span>
                </label>
              )}
              <button type="button" onClick={handleReset} className="btn btn-secondary">초기화</button>
              <button type="submit" className="btn btn-primary">검색</button>
            </div>
          </div>
        </form>
      </div>

      {/* SR 목록 */}
      <div className="card">
        <SrList
          srList={srList}
          onSelectSr={handleSelectSr}
          onDeleteSr={handleDelete}
          onRestoreSr={handleRestore}
          totalElements={totalElements}
          page={currentPage}
          isAdmin={isAdmin}
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
          onWikiClick={setSelectedWikiId}
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

      {/* Wiki 상세 슬라이드 패널 */}
      <WikiDetailPanel
        documentId={selectedWikiId}
        onClose={() => setSelectedWikiId(null)}
        onSrClick={(srId) => {
          setSelectedWikiId(null);  // Wiki 패널 닫기
          setShowDetail(false);     // 기존 SR 모달 닫기
          selectSr(null);           // 선택된 SR 초기화
          setWikiLinkedSrId(srId);  // SR 슬라이드 패널 열기
        }}
      />

      {/* Wiki에서 클릭한 SR 슬라이드 패널 */}
      <SrDetailPanel
        srId={wikiLinkedSrId}
        onClose={() => setWikiLinkedSrId(null)}
      />
    </div>
  );
}

export default SrManagementPage;
