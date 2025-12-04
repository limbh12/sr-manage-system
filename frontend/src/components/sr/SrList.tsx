import { Sr, SrStatus, Priority } from '../../types';

interface SrListProps {
  srList: Sr[];
  onSelectSr: (sr: Sr) => void;
  onDeleteSr: (id: number) => void;
  totalElements: number;
  page: number;
  size?: number;
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
function SrList({ srList, onSelectSr, onDeleteSr, totalElements, page, size = 10 }: SrListProps) {
  return (
    <div className="table-container">
      <table className="table">
        <thead>
          <tr>
            <th>No</th>
            <th>ID</th>
            <th>제목</th>
            <th>상태</th>
            <th>우선순위</th>
            <th>접수자</th>
            <th>담당자</th>
            <th>등록일(접수일)</th>
            <th>작업</th>
          </tr>
        </thead>
        <tbody>
          {srList.length === 0 ? (
            <tr>
              <td colSpan={9} style={{ textAlign: 'center', padding: '40px', color: '#666' }}>
                등록된 SR이 없습니다.
              </td>
            </tr>
          ) : (
            srList.map((sr, index) => (
              <tr key={sr.id}>
                <td>{totalElements - index}</td>
                <td>{sr.srId || '-'}</td>
                <td>
                  <a
                    href="#"
                    onClick={(e) => {
                      e.preventDefault();
                      onSelectSr(sr);
                    }}
                    style={{ color: '#1976d2', textDecoration: 'none' }}
                  >
                    {sr.title}
                  </a>
                </td>
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
                <td>{sr.requester.name || sr.requester.username}</td>
                <td>{sr.assignee ? (sr.assignee.name || sr.assignee.username) : '-'}</td>
                <td>{new Date(sr.createdAt).toLocaleDateString()}</td>
                <td>
                  <button
                    className="btn btn-secondary"
                    onClick={() => onSelectSr(sr)}
                    style={{ marginRight: '8px' }}
                  >
                    상세
                  </button>
                  <button
                    className="btn btn-danger"
                    onClick={() => onDeleteSr(sr.id)}
                  >
                    삭제
                  </button>
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
