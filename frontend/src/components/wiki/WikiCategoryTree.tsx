import React, { useState } from 'react';
import type { WikiCategory } from '../../types/wiki';
import './WikiCategoryTree.css';

interface WikiCategoryTreeProps {
  categories: WikiCategory[];
  selectedCategoryId?: number;
  onCategorySelect: (categoryId: number) => void;
  onCategoryCreate?: (parentId?: number) => void;
  onCategoryEdit?: (category: WikiCategory) => void;
  onCategoryDelete?: (categoryId: number) => void;
}

const WikiCategoryTree: React.FC<WikiCategoryTreeProps> = ({
  categories,
  selectedCategoryId,
  onCategorySelect,
  onCategoryCreate,
  onCategoryEdit,
  onCategoryDelete,
}) => {
  const [expandedCategories, setExpandedCategories] = useState<Set<number>>(new Set());

  // 하위 카테고리가 있는 카테고리 ID만 수집 (펼칠 수 있는 카테고리)
  const collectExpandableCategoryIds = (cats: WikiCategory[]): number[] => {
    const ids: number[] = [];
    const collect = (categoryList: WikiCategory[]) => {
      for (const cat of categoryList) {
        if (cat.children && cat.children.length > 0) {
          ids.push(cat.id);
          collect(cat.children);
        }
      }
    };
    collect(cats);
    return ids;
  };

  const toggleExpand = (categoryId: number) => {
    setExpandedCategories((prev) => {
      const newSet = new Set(prev);
      if (newSet.has(categoryId)) {
        newSet.delete(categoryId);
      } else {
        newSet.add(categoryId);
      }
      return newSet;
    });
  };

  // 전체 펼치기
  const expandAll = () => {
    const allExpandableIds = collectExpandableCategoryIds(categories);
    setExpandedCategories(new Set(allExpandableIds));
  };

  // 전체 접기
  const collapseAll = () => {
    setExpandedCategories(new Set());
  };

  // 펼칠 수 있는 카테고리가 있는지 확인
  const hasExpandableCategories = collectExpandableCategoryIds(categories).length > 0;

  const renderCategory = (category: WikiCategory, level: number = 0) => {
    const hasChildren = category.children && category.children.length > 0;
    const isExpanded = expandedCategories.has(category.id);
    const isSelected = selectedCategoryId === category.id;

    return (
      <div key={category.id} className="category-item" style={{ paddingLeft: `${level * 20}px` }}>
        <div className={`category-header ${isSelected ? 'selected' : ''}`}>
          {hasChildren && (
            <button
              className="category-toggle"
              onClick={() => toggleExpand(category.id)}
              aria-label={isExpanded ? 'Collapse' : 'Expand'}
            >
              {isExpanded ? '▼' : '▶'}
            </button>
          )}
          {!hasChildren && <span className="category-spacer" />}

          <div className="category-info" onClick={() => onCategorySelect(category.id)}>
            <span className="category-name">{category.name}</span>
            <span className="category-count">({category.documentCount})</span>
          </div>

          {(onCategoryEdit || onCategoryDelete || onCategoryCreate) && (
            <div className="category-actions">
              {onCategoryCreate && (
                <button
                  className="category-action-btn"
                  onClick={(e) => {
                    e.stopPropagation();
                    onCategoryCreate(category.id);
                  }}
                  title="하위 카테고리 추가"
                >
                  +
                </button>
              )}
              {onCategoryEdit && (
                <button
                  className="category-action-btn"
                  onClick={(e) => {
                    e.stopPropagation();
                    onCategoryEdit(category);
                  }}
                  title="카테고리 수정"
                >
                  ✎
                </button>
              )}
              {onCategoryDelete && (
                <button
                  className="category-action-btn delete"
                  onClick={(e) => {
                    e.stopPropagation();
                    if (window.confirm(`"${category.name}" 카테고리를 삭제하시겠습니까?`)) {
                      onCategoryDelete(category.id);
                    }
                  }}
                  title="카테고리 삭제"
                >
                  ×
                </button>
              )}
            </div>
          )}
        </div>

        {hasChildren && isExpanded && (
          <div className="category-children">
            {category.children!.map((child) => renderCategory(child, level + 1))}
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="wiki-category-tree">
      <div className="category-tree-header">
        <h3>카테고리</h3>
        <div className="category-header-actions">
          {hasExpandableCategories && (
            <div className="expand-collapse-buttons">
              <button
                className="btn-expand-all"
                onClick={expandAll}
                title="전체 펼치기"
              >
                ⊞
              </button>
              <button
                className="btn-collapse-all"
                onClick={collapseAll}
                title="전체 접기"
              >
                ⊟
              </button>
            </div>
          )}
          {onCategoryCreate && (
            <button
              className="btn-create-category"
              onClick={() => onCategoryCreate()}
              title="최상위 카테고리 추가"
            >
              + 새 카테고리
            </button>
          )}
        </div>
      </div>
      <div className="category-tree-content">
        {categories.length === 0 ? (
          <div className="category-empty">카테고리가 없습니다.</div>
        ) : (
          categories.map((category) => renderCategory(category, 0))
        )}
      </div>
    </div>
  );
};

export default WikiCategoryTree;
