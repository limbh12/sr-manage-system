import React, { useState, useEffect } from 'react';
import { CommonCode } from '../../types';
import { commonCodeService } from '../../services/commonCodeService';

interface CommonCodeFormProps {
  initialData?: CommonCode;
  initialGroup?: string;
  onClose: () => void;
  onSuccess: () => void;
}

const CommonCodeForm: React.FC<CommonCodeFormProps> = ({
  initialData,
  initialGroup,
  onClose,
  onSuccess,
}) => {
  const [formData, setFormData] = useState<Partial<CommonCode>>({
    codeGroup: initialGroup || '',
    codeValue: '',
    codeName: '',
    sortOrder: 0,
    isActive: true,
    description: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (initialData) {
      setFormData(initialData);
    } else if (initialGroup) {
      setFormData(prev => ({ ...prev, codeGroup: initialGroup }));
    }
  }, [initialData, initialGroup]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value, type } = e.target;
    
    if (type === 'checkbox') {
      const checked = (e.target as HTMLInputElement).checked;
      setFormData(prev => ({ ...prev, [name]: checked }));
    } else if (name === 'sortOrder') {
      setFormData(prev => ({ ...prev, [name]: parseInt(value) || 0 }));
    } else {
      setFormData(prev => ({ ...prev, [name]: value }));
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.codeGroup || !formData.codeValue || !formData.codeName) {
      setError('필수 항목을 입력해주세요.');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      if (initialData?.id) {
        await commonCodeService.updateCode(initialData.id, formData);
      } else {
        await commonCodeService.createCode(formData);
      }
      onSuccess();
    } catch (err: any) {
      setError(err.response?.data?.message || '코드 저장 중 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal">
        <div className="modal-header">
          <h2 className="modal-title">
            {initialData ? '공통코드 수정' : '공통코드 등록'}
          </h2>
          <button className="modal-close" onClick={onClose}>&times;</button>
        </div>

        <form onSubmit={handleSubmit}>
          {error && <div className="login-error">{error}</div>}

          <div className="form-group">
            <label className="form-label">코드 그룹 *</label>
            <input
              type="text"
              name="codeGroup"
              className="form-input"
              value={formData.codeGroup}
              onChange={handleChange}
              disabled={!!initialData} // 그룹은 수정 불가
              placeholder="예: SR_CATEGORY"
            />
          </div>

          <div className="form-group">
            <label className="form-label">코드 값 *</label>
            <input
              type="text"
              name="codeValue"
              className="form-input"
              value={formData.codeValue}
              onChange={handleChange}
              placeholder="예: OPEN_API"
            />
          </div>

          <div className="form-group">
            <label className="form-label">코드 명 *</label>
            <input
              type="text"
              name="codeName"
              className="form-input"
              value={formData.codeName}
              onChange={handleChange}
              placeholder="예: OPEN API"
            />
          </div>

          <div className="form-group">
            <label className="form-label">정렬 순서</label>
            <input
              type="number"
              name="sortOrder"
              className="form-input"
              value={formData.sortOrder}
              onChange={handleChange}
            />
          </div>

          <div className="form-group">
            <label className="form-label">설명</label>
            <textarea
              name="description"
              className="form-input"
              value={formData.description || ''}
              onChange={handleChange}
              rows={3}
            />
          </div>

          <div className="form-group">
            <label className="form-label">
              <input
                type="checkbox"
                name="isActive"
                checked={formData.isActive}
                onChange={handleChange}
                style={{ marginRight: '8px' }}
              />
              사용 여부
            </label>
          </div>

          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>
              취소
            </button>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? '저장 중...' : '저장'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default CommonCodeForm;
