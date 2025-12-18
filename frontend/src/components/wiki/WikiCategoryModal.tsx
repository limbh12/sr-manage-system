import React, { useState, useEffect } from 'react';
import type { WikiCategory, WikiCategoryRequest } from '../../types/wiki';
import './WikiCategoryModal.css';

interface WikiCategoryModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: WikiCategoryRequest) => Promise<void>;
  category?: WikiCategory | null;
  parentId?: number;
}

const WikiCategoryModal: React.FC<WikiCategoryModalProps> = ({
  isOpen,
  onClose,
  onSubmit,
  category,
  parentId,
}) => {
  const [name, setName] = useState('');
  const [sortOrder, setSortOrder] = useState(0);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (category) {
      setName(category.name);
      setSortOrder(category.sortOrder || 0);
    } else {
      setName('');
      setSortOrder(0);
    }
  }, [category, isOpen]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!name.trim()) {
      alert('카테고리 이름을 입력해주세요.');
      return;
    }

    setLoading(true);
    try {
      await onSubmit({
        name: name.trim(),
        sortOrder,
        parentId,
      });
      onClose();
    } catch (error) {
      console.error('카테고리 저장 실패:', error);
      alert('카테고리 저장에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>{category ? '카테고리 수정' : '새 카테고리'}</h2>
          <button className="modal-close-btn" onClick={onClose}>
            ×
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="category-name">카테고리 이름 *</label>
            <input
              id="category-name"
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="카테고리 이름을 입력하세요"
              maxLength={100}
              required
              autoFocus
            />
          </div>

          <div className="form-group">
            <label htmlFor="category-sort">정렬 순서</label>
            <input
              id="category-sort"
              type="number"
              value={sortOrder}
              onChange={(e) => setSortOrder(parseInt(e.target.value) || 0)}
              placeholder="0"
            />
            <small>숫자가 작을수록 먼저 표시됩니다</small>
          </div>

          <div className="modal-actions">
            <button
              type="button"
              className="btn-secondary"
              onClick={onClose}
              disabled={loading}
            >
              취소
            </button>
            <button
              type="submit"
              className="btn-primary"
              disabled={loading}
            >
              {loading ? '저장 중...' : '저장'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default WikiCategoryModal;
