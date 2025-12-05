import { useState, useEffect } from 'react';
import { OpenApiSurvey, OpenApiSurveySearch } from '../../types';
import * as surveyService from '../../services/surveyService';

interface SurveySearchModalProps {
  onSelect: (survey: OpenApiSurvey) => void;
  onClose: () => void;
  initialKeyword?: string;
}

function SurveySearchModal({ onSelect, onClose, initialKeyword = '' }: SurveySearchModalProps) {
  const [surveys, setSurveys] = useState<OpenApiSurvey[]>([]);
  const [loading, setLoading] = useState(false);
  const [keyword, setKeyword] = useState(initialKeyword);

  const searchSurveys = async () => {
    setLoading(true);
    try {
      const searchParams: OpenApiSurveySearch = { keyword };
      const response = await surveyService.getSurveyList(0, 100, searchParams);
      setSurveys(response.content);
    } catch (error) {
      console.error(error);
      alert('목록을 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    setKeyword(initialKeyword);
  }, [initialKeyword]);

  useEffect(() => {
    searchSurveys();
  }, []);

  useEffect(() => {
    const handleEsc = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', handleEsc);
    return () => {
      window.removeEventListener('keydown', handleEsc);
    };
  }, [onClose]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    searchSurveys();
  };

  return (
    <div className="modal-overlay" style={{ zIndex: 1300 }}>
      <div className="modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '800px' }}>
        <div className="modal-header">
          <h2 className="modal-title">OPEN API 현황조사 검색</h2>
          <button className="modal-close" onClick={onClose}>×</button>
        </div>

        <form onSubmit={handleSearch} style={{ display: 'flex', gap: '8px', marginBottom: '16px' }}>
          <input
            type="text"
            className="form-input"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="기관명, 시스템명, 부서명 검색"
          />
          <button type="submit" className="btn btn-primary" style={{ whiteSpace: 'nowrap' }}>검색</button>
        </form>

        <div style={{ maxHeight: '400px', overflowY: 'auto' }}>
          {loading ? (
            <div className="loading-container" style={{ minHeight: '100px' }}>
              <div className="loading-spinner"></div>
            </div>
          ) : (
            <table className="table">
              <thead>
                <tr>
                  <th>기관명</th>
                  <th>부서</th>
                  <th>시스템명</th>
                  <th>담당자</th>
                  <th>선택</th>
                </tr>
              </thead>
              <tbody>
                {surveys.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="text-center" style={{ padding: '24px', color: '#666', textAlign: 'center' }}>
                      검색 결과가 없습니다.
                    </td>
                  </tr>
                ) : (
                  surveys.map((survey) => (
                                      <tr 
                    key={survey.id} 
                    onClick={() => onSelect(survey)}
                    style={{ cursor: 'pointer' }}
                  >
                    <td>{survey.organization?.name || '-'}</td>
                    <td>{survey.department || '-'}</td>
                    <td>{survey.systemName || '-'}</td>
                    <td>{survey.contactName}</td>
                    <td>
                      <button
                        className="btn btn-sm btn-primary"
                        onClick={(e) => {
                          e.stopPropagation();
                          onSelect(survey);
                        }}
                      >
                        선택
                      </button>
                    </td>
                  </tr>
                  ))
                )}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}

export default SurveySearchModal;
