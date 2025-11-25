import { Sr, SrStatus, Priority } from '../../types';

interface SrDetailProps {
  sr: Sr;
  onClose: () => void;
  onEdit: () => void;
  onStatusChange: (status: SrStatus) => void;
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
 * SR 상세 컴포넌트
 */
function SrDetail({ sr, onClose, onEdit, onStatusChange }: SrDetailProps) {
  const statusOptions: SrStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2 className="modal-title">SR 상세 정보</h2>
          <button className="modal-close" onClick={onClose}>
            ×
          </button>
        </div>

        <div style={{ marginBottom: '16px' }}>
          <strong>ID:</strong> {sr.id}
        </div>

        <div style={{ marginBottom: '16px' }}>
          <strong>제목:</strong> {sr.title}
        </div>

        <div style={{ marginBottom: '16px' }}>
          <strong>설명:</strong>
          <p style={{ marginTop: '8px', whiteSpace: 'pre-wrap' }}>
            {sr.description || '(설명 없음)'}
          </p>
        </div>

        <div style={{ marginBottom: '16px' }}>
          <strong>상태:</strong> {getStatusLabel(sr.status)}
        </div>

        <div style={{ marginBottom: '16px' }}>
          <strong>우선순위:</strong> {getPriorityLabel(sr.priority)}
        </div>

        <div style={{ marginBottom: '16px' }}>
          <strong>요청자:</strong> {sr.requester.username} ({sr.requester.email})
        </div>

        <div style={{ marginBottom: '16px' }}>
          <strong>담당자:</strong>{' '}
          {sr.assignee
            ? `${sr.assignee.username} (${sr.assignee.email})`
            : '(미지정)'}
        </div>

        <div style={{ marginBottom: '16px' }}>
          <strong>생성일:</strong> {new Date(sr.createdAt).toLocaleString()}
        </div>

        <div style={{ marginBottom: '16px' }}>
          <strong>수정일:</strong> {new Date(sr.updatedAt).toLocaleString()}
        </div>

        <div style={{ marginBottom: '16px' }}>
          <strong>상태 변경:</strong>
          <div style={{ marginTop: '8px', display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
            {statusOptions.map((status) => (
              <button
                key={status}
                className={`btn ${sr.status === status ? 'btn-primary' : 'btn-secondary'}`}
                onClick={() => onStatusChange(status)}
                disabled={sr.status === status}
              >
                {getStatusLabel(status)}
              </button>
            ))}
          </div>
        </div>

        <div className="modal-footer">
          <button className="btn btn-secondary" onClick={onClose}>
            닫기
          </button>
          <button className="btn btn-primary" onClick={onEdit}>
            수정
          </button>
        </div>
      </div>
    </div>
  );
}

export default SrDetail;
