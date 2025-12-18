import React, { useState, useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import { getSrList } from '../../services/srService';
import type { Sr } from '../../types';
import type { SrInfo } from '../../types/wiki';
import './SrSelector.css';

interface SrSelectorProps {
  selectedSrs: SrInfo[];
  onChange: (srs: SrInfo[]) => void;
}

const SrSelector: React.FC<SrSelectorProps> = ({ selectedSrs, onChange }) => {
  const [searchKeyword, setSearchKeyword] = useState('');
  const [searchResults, setSearchResults] = useState<Sr[]>([]);
  const [showDropdown, setShowDropdown] = useState(false);
  const [loading, setLoading] = useState(false);
  const [dropdownPosition, setDropdownPosition] = useState({ top: 0, left: 0, width: 0 });
  const [hasMoreResults, setHasMoreResults] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (searchKeyword.trim().length >= 3) {
      searchSrs();
    } else {
      setSearchResults([]);
      setShowDropdown(false);
      setHasMoreResults(false);
    }
  }, [searchKeyword]);

  // 드롭다운 외부 클릭 시 닫기
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (showDropdown && inputRef.current && !inputRef.current.contains(event.target as Node)) {
        // 드롭다운 자체 클릭은 제외 (Portal로 렌더링되므로 별도 체크)
        const dropdownElements = document.querySelectorAll('.sr-dropdown-portal');
        let clickedInside = false;
        dropdownElements.forEach(el => {
          if (el.contains(event.target as Node)) {
            clickedInside = true;
          }
        });
        if (!clickedInside) {
          setShowDropdown(false);
        }
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [showDropdown]);

  const updateDropdownPosition = () => {
    if (inputRef.current) {
      const rect = inputRef.current.getBoundingClientRect();
      setDropdownPosition({
        top: rect.bottom + window.scrollY + 4,
        left: rect.left + window.scrollX,
        width: rect.width
      });
    }
  };

  const searchSrs = async () => {
    try {
      setLoading(true);
      const response = await getSrList({
        page: 0,
        size: 500, // 더 많은 결과를 가져와서 클라이언트에서 필터링 (백엔드 기본값 10 오버라이드)
      });

      console.log('API에서 가져온 전체 SR 수:', response.content.length);
      console.log('검색어:', searchKeyword);

      // 클라이언트 사이드에서 제목으로 필터링
      const allFiltered = response.content.filter(sr =>
        sr.title.toLowerCase().includes(searchKeyword.toLowerCase())
      );

      console.log('필터링된 SR 수:', allFiltered.length);
      console.log('필터링된 SR 목록:', allFiltered.map(sr => sr.title));

      // 최대 30개만 표시
      const filtered = allFiltered.slice(0, 30);
      setSearchResults(filtered);
      setHasMoreResults(allFiltered.length > 30);

      updateDropdownPosition();
      setShowDropdown(true);
    } catch (error) {
      console.error('SR 검색 실패:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSelectSr = (sr: Sr) => {
    // 이미 선택된 SR인지 확인
    if (selectedSrs.some(s => s.id === sr.id)) {
      return;
    }

    const srInfo: SrInfo = {
      id: sr.id,
      title: sr.title,
      status: sr.status,
    };

    onChange([...selectedSrs, srInfo]);
    setSearchKeyword('');
    setSearchResults([]);
    setShowDropdown(false);
  };

  const handleRemoveSr = (srId: number) => {
    onChange(selectedSrs.filter(s => s.id !== srId));
  };

  const getStatusBadgeClass = (status: string) => {
    switch (status) {
      case 'OPEN': return 'status-open';
      case 'IN_PROGRESS': return 'status-in-progress';
      case 'RESOLVED': return 'status-resolved';
      case 'CLOSED': return 'status-closed';
      default: return '';
    }
  };

  const getStatusText = (status: string) => {
    switch (status) {
      case 'OPEN': return '접수';
      case 'IN_PROGRESS': return '진행중';
      case 'RESOLVED': return '해결';
      case 'CLOSED': return '종료';
      default: return status;
    }
  };

  return (
    <div className="sr-selector">
      <label>연계 SR (선택사항)</label>

      {/* 선택된 SR 목록 */}
      {selectedSrs.length > 0 && (
        <div className="selected-srs">
          {selectedSrs.map(sr => (
            <div key={sr.id} className="selected-sr-item">
              <span className={`sr-status-badge ${getStatusBadgeClass(sr.status)}`}>
                {getStatusText(sr.status)}
              </span>
              <span className="sr-title">{sr.title}</span>
              <button
                type="button"
                className="btn-remove-sr"
                onClick={() => handleRemoveSr(sr.id)}
                title="제거"
              >
                ×
              </button>
            </div>
          ))}
        </div>
      )}

      {/* SR 검색 입력 */}
      <div className="sr-search-container">
        <input
          ref={inputRef}
          type="text"
          className="sr-search-input"
          placeholder="SR 제목으로 검색... (최소 3자)"
          value={searchKeyword}
          onChange={(e) => setSearchKeyword(e.target.value)}
          onFocus={() => {
            if (searchKeyword.length >= 3) {
              updateDropdownPosition();
              setShowDropdown(true);
            }
          }}
        />
        {loading && <span className="search-loading">검색 중...</span>}
      </div>

      {/* 검색 결과 드롭다운 (Portal로 body에 렌더링) */}
      {showDropdown && searchResults.length > 0 && createPortal(
        <div
          className="sr-dropdown sr-dropdown-portal"
          style={{
            position: 'fixed',
            top: `${dropdownPosition.top}px`,
            left: `${dropdownPosition.left}px`,
            width: `${dropdownPosition.width}px`,
            maxWidth: '600px'
          }}
        >
          {searchResults.map(sr => (
            <div
              key={sr.id}
              className={`sr-dropdown-item ${selectedSrs.some(s => s.id === sr.id) ? 'disabled' : ''}`}
              onClick={() => !selectedSrs.some(s => s.id === sr.id) && handleSelectSr(sr)}
            >
              <span className={`sr-status-badge ${getStatusBadgeClass(sr.status)}`}>
                {getStatusText(sr.status)}
              </span>
              <span className="sr-dropdown-title">{sr.title}</span>
              {selectedSrs.some(s => s.id === sr.id) && (
                <span className="already-selected">이미 선택됨</span>
              )}
            </div>
          ))}
          {hasMoreResults && (
            <div className="sr-dropdown-more-hint">
              더 많은 결과가 있습니다. 더 구체적인 검색어를 입력하세요.
            </div>
          )}
        </div>,
        document.body
      )}

      {showDropdown && searchKeyword.length >= 3 && searchResults.length === 0 && !loading && createPortal(
        <div
          className="sr-dropdown sr-dropdown-portal"
          style={{
            position: 'fixed',
            top: `${dropdownPosition.top}px`,
            left: `${dropdownPosition.left}px`,
            width: `${dropdownPosition.width}px`,
            maxWidth: '600px'
          }}
        >
          <div className="sr-dropdown-empty">검색 결과가 없습니다.</div>
        </div>,
        document.body
      )}

      <small className="sr-selector-hint">
        이 Wiki 문서와 연계할 SR을 검색하여 추가하세요.
      </small>
    </div>
  );
};

export default SrSelector;
