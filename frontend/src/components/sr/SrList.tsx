import { useEffect, useState } from 'react';
import { Sr, SrStatus, Priority } from '../../types';
import { commonCodeService } from '../../services/commonCodeService';

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

interface SrListProps {
  srList: Sr[];
  onSelectSr: (sr: Sr) => void;
  onDeleteSr: (id: number) => void;
  onRestoreSr?: (id: number) => void;
  totalElements: number;
  page: number;
  size?: number;
  isAdmin?: boolean;
}

/**
 * 상태 뱃지 클래스 반환
 */
const getStatusBadgeClass = (status: SrStatus): string => {
  switch (status) {
    case 'OPEN':
      return 'badge badge-open';
    case 'IN_PROGRESS':
      return 'badge badge-in-progress';
    case 'RESOLVED':
      return 'badge badge-resolved';
    case 'CLOSED':
      return 'badge badge-closed';
    default:
      return 'badge';
  }
};

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
 * SR 목록 컴포넌트
 */
function SrList({ srList, onSelectSr, onDeleteSr, onRestoreSr, totalElements, page: _page, size: _size = 10, isAdmin = false }: SrListProps) {
  const [categoryMap, setCategoryMap] = useState<Map<string, string>>(new Map());
  const [requestTypeMap, setRequestTypeMap] = useState<Map<string, string>>(new Map());

  useEffect(() => {
    // 분류 코드 로드
    commonCodeService.getActiveCodesByGroup('SR_CATEGORY').then(codes => {
      const map = new Map<string, string>();
      codes.forEach(code => map.set(code.codeValue, code.codeName));
      setCategoryMap(map);
    }).catch(console.error);

    // 요청구분 코드 로드
    commonCodeService.getActiveCodesByGroup('SR_REQUEST_TYPE').then(codes => {
      const map = new Map<string, string>();
      codes.forEach(code => map.set(code.codeValue, code.codeName));
      setRequestTypeMap(map);
    }).catch(console.error);
  }, []);

  return (
    <div className="table-container">
      <table className="table">
        <thead>
          <tr>
            <th>No</th>
            <th>ID</th>
            <th>분류</th>
            <th>요청구분</th>
            <th>제목</th>
            <th>요청자</th>
            <th>상태</th>
            <th>우선순위</th>
            <th>담당자</th>
            <th>처리예정일자</th>
            <th>등록일(접수일)</th>
            <th>작업</th>
          </tr>
        </thead>
        <tbody>
          {srList.length === 0 ? (
            <tr>
              <td colSpan={12} style={{ textAlign: 'center', padding: '40px', color: '#666' }}>
                등록된 SR이 없습니다.
              </td>
            </tr>
          ) : (
            srList.map((sr, index) => (
              <tr key={sr.id} style={{ opacity: sr.deleted ? 0.6 : 1 }}>
                <td>{totalElements - index}</td>
                <td>{sr.srId || '-'}</td>
                <td>{sr.category ? (categoryMap.get(sr.category) || sr.category) : '-'}</td>
                <td>{sr.requestType ? (requestTypeMap.get(sr.requestType) || sr.requestType) : '-'}</td>
                <td>
                  <a
                    href="#"
                    onClick={(e) => {
                      e.preventDefault();
                      onSelectSr(sr);
                    }}
                    style={{
                      color: '#1976d2',
                      textDecoration: sr.deleted ? 'line-through' : 'none'
                    }}
                  >
                    {sr.title}
                  </a>
                  {sr.deleted && <span style={{ marginLeft: '8px', color: '#999', fontSize: '12px' }}>(삭제됨)</span>}
                </td>
                <td>{sr.applicantName || sr.requester.name || sr.requester.username}</td>
                <td>
                  <span className={getStatusBadgeClass(sr.status)}>
                    {getStatusLabel(sr.status)}
                  </span>
                </td>
                <td>
                  <span className={getPriorityBadgeClass(sr.priority)}>
                    {getPriorityLabel(sr.priority)}
                  </span>
                </td>
                <td>{sr.assignee ? (sr.assignee.name || sr.assignee.username) : '-'}</td>
                <td>
                  {sr.expectedCompletionDate ? (
                    <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                      <span>{new Date(sr.expectedCompletionDate).toLocaleDateString()}</span>
                      {isDueTomorrow(sr.expectedCompletionDate) && (
                        <span className="badge badge-critical" style={{ fontSize: '10px', padding: '2px 6px' }}>
                          D-1
                        </span>
                      )}
                    </div>
                  ) : (
                    '-'
                  )}
                </td>
                <td>{new Date(sr.createdAt).toLocaleDateString()}</td>
                <td>
                  {sr.deleted && isAdmin && onRestoreSr ? (
                    <button
                      className="btn btn-primary"
                      onClick={() => onRestoreSr(sr.id)}
                      style={{ marginRight: '8px' }}
                    >
                      복구
                    </button>
                  ) : null}
                  {!sr.deleted && (
                    <button
                      className="btn btn-danger"
                      onClick={() => onDeleteSr(sr.id)}
                    >
                      삭제
                    </button>
                  )}
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}

export default SrList;
