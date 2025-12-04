import React, { useState, useEffect } from 'react';
import CommonCodeList from '../components/admin/CommonCodeList';
import CommonCodeForm from '../components/admin/CommonCodeForm';
import { commonCodeService } from '../services/commonCodeService';
import { CommonCode } from '../types';

const CommonCodePage: React.FC = () => {
  const [groups, setGroups] = useState<string[]>([]);
  const [selectedGroup, setSelectedGroup] = useState<string>('');
  const [showForm, setShowForm] = useState(false);
  const [editingCode, setEditingCode] = useState<CommonCode | undefined>(undefined);
  const [refreshKey, setRefreshKey] = useState(0);
  const [loading, setLoading] = useState(false);

  const fetchGroups = async () => {
    setLoading(true);
    try {
      const data = await commonCodeService.getCodeGroups();
      setGroups(data);
      // If no group selected and groups exist, select the first one
      if (!selectedGroup && data.length > 0) {
        setSelectedGroup(data[0]);
      }
    } catch (err) {
      console.error('Failed to fetch code groups', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchGroups();
  }, []);

  const handleGroupClick = (group: string) => {
    setSelectedGroup(group);
  };

  const handleAddCode = () => {
    setEditingCode(undefined);
    setShowForm(true);
  };

  const handleAddGroup = () => {
    setSelectedGroup(''); // Clear selection to indicate new group creation might be intended, or just let them type it
    setEditingCode(undefined);
    setShowForm(true);
  };

  const handleEditCode = (code: CommonCode) => {
    setEditingCode(code);
    setShowForm(true);
  };

  const handleFormSuccess = () => {
    setShowForm(false);
    setRefreshKey(prev => prev + 1); // Refresh list
    fetchGroups(); // Refresh groups in case a new one was added
  };

  return (
    <div className="container">
      <div className="page-header">
        <h1 className="page-title">공통코드 관리</h1>
        <button className="btn btn-primary" onClick={handleAddGroup}>
          + 새 그룹/코드 등록
        </button>
      </div>

      <div className="grid-2" style={{ gridTemplateColumns: '250px 1fr', alignItems: 'start' }}>
        {/* Left: Group List */}
        <div className="card" style={{ padding: '0' }}>
          <div style={{ padding: '16px', borderBottom: '1px solid #eee', fontWeight: '500' }}>
            코드 그룹
          </div>
          <div style={{ maxHeight: 'calc(100vh - 200px)', overflowY: 'auto' }}>
            {loading ? (
              <div style={{ padding: '16px', textAlign: 'center' }}>로딩 중...</div>
            ) : (
              <ul style={{ listStyle: 'none', padding: '0', margin: '0' }}>
                {groups.map(group => (
                  <li key={group}>
                    <button
                      style={{
                        width: '100%',
                        textAlign: 'left',
                        padding: '12px 16px',
                        border: 'none',
                        background: selectedGroup === group ? '#e3f2fd' : 'transparent',
                        color: selectedGroup === group ? '#1976d2' : '#333',
                        fontWeight: selectedGroup === group ? '500' : 'normal',
                        cursor: 'pointer',
                        borderLeft: selectedGroup === group ? '3px solid #1976d2' : '3px solid transparent'
                      }}
                      onClick={() => handleGroupClick(group)}
                    >
                      {group}
                    </button>
                  </li>
                ))}
                {groups.length === 0 && (
                  <li style={{ padding: '16px', color: '#757575', textAlign: 'center' }}>
                    등록된 그룹이 없습니다.
                  </li>
                )}
              </ul>
            )}
          </div>
        </div>

        {/* Right: Code List */}
        <div>
          {selectedGroup ? (
            <>
              <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '16px' }}>
                <button className="btn btn-primary" onClick={handleAddCode}>
                  + {selectedGroup}에 코드 추가
                </button>
              </div>
              <CommonCodeList
                key={`${selectedGroup}-${refreshKey}`}
                selectedGroup={selectedGroup}
                onEdit={handleEditCode}
              />
            </>
          ) : (
            <div className="card text-center" style={{ padding: '40px' }}>
              <p className="text-gray">왼쪽에서 코드 그룹을 선택하거나 새로운 그룹을 등록하세요.</p>
            </div>
          )}
        </div>
      </div>

      {showForm && (
        <CommonCodeForm
          initialData={editingCode}
          initialGroup={selectedGroup}
          onClose={() => setShowForm(false)}
          onSuccess={handleFormSuccess}
        />
      )}
    </div>
  );
};

export default CommonCodePage;
