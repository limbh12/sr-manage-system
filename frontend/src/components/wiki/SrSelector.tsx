import React, { useState, useEffect } from 'react';
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

  useEffect(() => {
    if (searchKeyword.trim().length >= 2) {
      searchSrs();
    } else {
      setSearchResults([]);
      setShowDropdown(false);
    }
  }, [searchKeyword]);

  const searchSrs = async () => {
    try {
      setLoading(true);
      const response = await getSrList({
        page: 0,
        size: 100, // 더 많은 결과를 가져와서 클라이언트에서 필터링
      });
      // 클라이언트 사이드에서 제목으로 필터링
      const filtered = response.content.filter(sr =>
        sr.title.toLowerCase().includes(searchKeyword.toLowerCase())
      ).slice(0, 10);
      setSearchResults(filtered);
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
          type="text"
          className="sr-search-input"
          placeholder="SR 제목으로 검색... (최소 2자)"
          value={searchKeyword}
          onChange={(e) => setSearchKeyword(e.target.value)}
          onFocus={() => searchKeyword.length >= 2 && setShowDropdown(true)}
        />
        {loading && <span className="search-loading">검색 중...</span>}

        {/* 검색 결과 드롭다운 */}
        {showDropdown && searchResults.length > 0 && (
          <div className="sr-dropdown">
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
          </div>
        )}

        {showDropdown && searchKeyword.length >= 2 && searchResults.length === 0 && !loading && (
          <div className="sr-dropdown">
            <div className="sr-dropdown-empty">검색 결과가 없습니다.</div>
          </div>
        )}
      </div>

      <small className="sr-selector-hint">
        이 Wiki 문서와 연계할 SR을 검색하여 추가하세요.
      </small>
    </div>
  );
};

export default SrSelector;
