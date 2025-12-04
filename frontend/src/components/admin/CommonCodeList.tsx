import React, { useState, useEffect } from 'react';
import { CommonCode } from '../../types';
import { commonCodeService } from '../../services/commonCodeService';

interface CommonCodeListProps {
  selectedGroup: string;
  onEdit: (code: CommonCode) => void;
}

const CommonCodeList: React.FC<CommonCodeListProps> = ({ selectedGroup, onEdit }) => {
  const [codes, setCodes] = useState<CommonCode[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchCodes = async () => {
    if (!selectedGroup) return;
    
    setLoading(true);
    setError(null);
    try {
      const data = await commonCodeService.getCodesByGroup(selectedGroup);
      setCodes(data);
    } catch (err) {
      setError('코드 목록을 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCodes();
  }, [selectedGroup]);

  // Expose fetchCodes to parent via ref or just rely on parent triggering re-render?
  // Actually, if the parent passes a "refreshTrigger" prop, it might be easier.
  // Or better, just expose a refresh method?
  // For now, let's just re-fetch when selectedGroup changes. 
  // If we add/edit, the parent can force a refresh by toggling a key or something, 
  // but standard way is to lift state up or use a query library.
  // Since we are using simple state, I'll add a refresh method to the component 
  // or just let the parent handle the data fetching?
  // No, let's keep data fetching here for simplicity, but maybe add a refresh prop.
  
  // Let's add a way to delete here.
  const handleDelete = async (id: number) => {
    if (!window.confirm('정말 삭제하시겠습니까?')) return;

    try {
      await commonCodeService.deleteCode(id);
      fetchCodes(); // Refresh list
    } catch (err: any) {
      alert(err.response?.data?.message || '삭제 중 오류가 발생했습니다.');
    }
  };

  // We need to expose a way to refresh the list from outside (e.g. after adding a code).
  // I'll add a `key` prop in the parent to force re-mount or use a refresh trigger.
  // Let's use `useImperativeHandle` or just pass a `refreshTrigger` prop.
  // Let's go with `refreshTrigger` prop.
  
  return (
    <div className="card">
      <div className="card-title">
        {selectedGroup} 코드 목록
        <button 
          className="btn btn-secondary" 
          style={{ float: 'right', fontSize: '12px', padding: '4px 8px' }}
          onClick={fetchCodes}
        >
          새로고침
        </button>
      </div>

      {loading ? (
        <div className="loading-container">
          <div className="loading-spinner"></div>
        </div>
      ) : error ? (
        <div className="text-center text-gray">{error}</div>
      ) : codes.length === 0 ? (
        <div className="text-center text-gray" style={{ padding: '20px' }}>
          등록된 코드가 없습니다.
        </div>
      ) : (
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>코드 값</th>
                <th>코드 명</th>
                <th>정렬 순서</th>
                <th>사용 여부</th>
                <th>설명</th>
                <th>관리</th>
              </tr>
            </thead>
            <tbody>
              {codes.map((code) => (
                <tr key={code.id}>
                  <td>{code.codeValue}</td>
                  <td>{code.codeName}</td>
                  <td>{code.sortOrder}</td>
                  <td>
                    <span className={`badge ${code.isActive ? 'badge-resolved' : 'badge-closed'}`}>
                      {code.isActive ? '사용' : '미사용'}
                    </span>
                  </td>
                  <td>{code.description}</td>
                  <td>
                    <div style={{ display: 'flex', gap: '8px' }}>
                      <button 
                        className="btn btn-secondary" 
                        style={{ padding: '4px 8px', fontSize: '12px' }}
                        onClick={() => onEdit(code)}
                      >
                        수정
                      </button>
                      <button 
                        className="btn btn-danger" 
                        style={{ padding: '4px 8px', fontSize: '12px' }}
                        onClick={() => handleDelete(code.id)}
                      >
                        삭제
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default CommonCodeList;
