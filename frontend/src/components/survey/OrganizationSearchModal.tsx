import { useState, useEffect } from 'react';
import * as surveyService from '../../services/surveyService';

interface Organization {
  code: string;
  name: string;
}

interface OrganizationSearchModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSelect: (org: Organization) => void;
}

function OrganizationSearchModal({ isOpen, onClose, onSelect }: OrganizationSearchModalProps) {
  const [keyword, setKeyword] = useState('');
  const [results, setResults] = useState<Organization[]>([]);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);

  useEffect(() => {
    if (!isOpen) return;
    const handleEsc = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', handleEsc);
    return () => {
      window.removeEventListener('keydown', handleEsc);
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const handleSearch = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!keyword.trim()) return;

    setLoading(true);
    try {
      const data = await surveyService.searchOrganizations(keyword);
      setResults(data);
      setSearched(true);
    } catch (error) {
      console.error(error);
      alert('검색 중 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal" style={{ maxWidth: '600px' }}>
        <div className="modal-header">
          <h3 className="modal-title">기관 검색 (행정표준코드)</h3>
          <button onClick={onClose} className="modal-close">&times;</button>
        </div>
        
        <div className="modal-body">
          <form onSubmit={handleSearch} className="flex gap-2 mb-4" style={{ display: 'flex', gap: '8px', marginBottom: '16px' }}>
            <input
              type="text"
              className="form-input"
              placeholder="기관명 또는 기관코드 입력 (예: 서울특별시)"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              autoFocus
            />
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? '검색 중...' : '검색'}
            </button>
          </form>

          <div className="search-results" style={{ maxHeight: '300px', overflowY: 'auto', border: '1px solid #eee', borderRadius: '4px' }}>
            {loading ? (
              <div className="text-center p-4" style={{ padding: '16px', textAlign: 'center' }}>검색 중...</div>
            ) : results.length > 0 ? (
              <table className="table">
                <thead>
                  <tr>
                    <th>기관코드</th>
                    <th>기관명</th>
                    <th>선택</th>
                  </tr>
                </thead>
                <tbody>
                  {results.map((org) => (
                    <tr key={org.code}>
                      <td>{org.code}</td>
                      <td>{org.name}</td>
                      <td>
                        <button
                          type="button"
                          className="btn btn-secondary"
                          style={{ padding: '4px 8px', fontSize: '12px' }}
                          onClick={() => onSelect(org)}
                        >
                          선택
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : searched ? (
              <div className="text-center p-4 text-gray" style={{ padding: '16px', textAlign: 'center', color: '#666' }}>
                검색 결과가 없습니다.
              </div>
            ) : (
              <div className="text-center p-4 text-gray" style={{ padding: '16px', textAlign: 'center', color: '#666' }}>
                기관명 또는 코드를 입력하여 검색하세요.
              </div>
            )}
          </div>
          
          <div className="mt-4 text-sm text-gray" style={{ marginTop: '16px', fontSize: '12px', color: '#666' }}>
            * 행정표준코드관리시스템(code.go.kr) 기준 데이터입니다.
          </div>
        </div>

        <div className="modal-footer">
          <button onClick={onClose} className="btn btn-secondary">닫기</button>
        </div>
      </div>
    </div>
  );
}

export default OrganizationSearchModal;
