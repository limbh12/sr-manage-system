import { useState, FormEvent, useEffect } from 'react';
import { Sr, SrCreateRequest, SrUpdateRequest, Priority } from '../../types';

interface SrFormProps {
  sr?: Sr | null;
  onSubmit: (data: SrCreateRequest | SrUpdateRequest) => void;
  onCancel: () => void;
  loading?: boolean;
}

/**
 * SR 생성/수정 폼 컴포넌트
 */
function SrForm({ sr, onSubmit, onCancel, loading = false }: SrFormProps) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState<Priority>('MEDIUM');

  const isEditMode = !!sr;

  useEffect(() => {
    if (sr) {
      setTitle(sr.title);
      setDescription(sr.description || '');
      setPriority(sr.priority);
    }
  }, [sr]);

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    
    if (isEditMode) {
      const updateData: SrUpdateRequest = {
        title,
        description,
        priority,
      };
      onSubmit(updateData);
    } else {
      const createData: SrCreateRequest = {
        title,
        description,
        priority,
      };
      onSubmit(createData);
    }
  };

  return (
    <div className="modal-overlay" onClick={onCancel}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2 className="modal-title">{isEditMode ? 'SR 수정' : 'SR 등록'}</h2>
          <button className="modal-close" onClick={onCancel}>
            ×
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="title" className="form-label">
              제목 *
            </label>
            <input
              type="text"
              id="title"
              className="form-input"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="SR 제목을 입력하세요"
              required
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="description" className="form-label">
              설명
            </label>
            <textarea
              id="description"
              className="form-input"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="SR 설명을 입력하세요"
              rows={5}
              disabled={loading}
              style={{ resize: 'vertical' }}
            />
          </div>

          <div className="form-group">
            <label htmlFor="priority" className="form-label">
              우선순위
            </label>
            <select
              id="priority"
              className="form-select"
              value={priority}
              onChange={(e) => setPriority(e.target.value as Priority)}
              disabled={loading}
            >
              <option value="LOW">낮음</option>
              <option value="MEDIUM">보통</option>
              <option value="HIGH">높음</option>
              <option value="CRITICAL">긴급</option>
            </select>
          </div>

          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onCancel} disabled={loading}>
              취소
            </button>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? '저장 중...' : isEditMode ? '수정' : '등록'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default SrForm;
