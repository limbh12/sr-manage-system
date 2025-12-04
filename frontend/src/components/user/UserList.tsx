import { useEffect, useState, useRef, useCallback } from 'react';
import { User, PageResponse, UserUpdateRequest, UserCreateRequest } from '../../types';
import * as userService from '../../services/userService';
import Loading from '../common/Loading';
import UserEditModal from './UserEditModal';

/**
 * 사용자 목록 컴포넌트
 */
function UserList() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const pageSize = 10;
  const observerTarget = useRef<HTMLDivElement>(null);

  // 수정/생성 모달 상태
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [saving, setSaving] = useState(false);

  const fetchUsers = async (pageNumber: number) => {
    if (loading) return;
    setLoading(true);
    setError(null);
    try {
      const response: PageResponse<User> = await userService.getUsers(pageNumber, pageSize);
      
      if (pageNumber === 0) {
        setUsers(response.content);
      } else {
        setUsers(prev => [...prev, ...response.content]);
      }
      
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
      setPage(response.number);
    } catch (err) {
      console.error('Failed to fetch users', err);
      setError('사용자 목록을 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers(0);
  }, []);

  const handleObserver = useCallback((entries: IntersectionObserverEntry[]) => {
    const target = entries[0];
    if (target.isIntersecting && !loading && page < totalPages - 1) {
      fetchUsers(page + 1);
    }
  }, [loading, page, totalPages]);

  useEffect(() => {
    const option = {
      root: null,
      rootMargin: "20px",
      threshold: 0
    };
    const observer = new IntersectionObserver(handleObserver, option);
    if (observerTarget.current) observer.observe(observerTarget.current);
    
    return () => {
      if (observerTarget.current) observer.unobserve(observerTarget.current);
    }
  }, [handleObserver]);

  const handleCreateClick = () => {
    setEditingUser(null);
    setIsModalOpen(true);
  };

  const handleEditClick = (user: User) => {
    setEditingUser(user);
    setIsModalOpen(true);
  };

  const handleDeleteClick = async (id: number) => {
    if (window.confirm('정말로 이 사용자를 삭제(탈퇴) 처리하시겠습니까?')) {
      try {
        await userService.deleteUser(id);
        await fetchUsers(0); // 목록 새로고침 (처음부터)
        alert('사용자가 삭제되었습니다.');
      } catch (err) {
        console.error('Failed to delete user', err);
        alert('사용자 삭제에 실패했습니다.');
      }
    }
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setEditingUser(null);
  };

  const handleSaveUser = async (id: number | null, data: UserUpdateRequest | UserCreateRequest) => {
    setSaving(true);
    try {
      if (id) {
        // 수정
        await userService.updateUser(id, data as UserUpdateRequest);
        alert('사용자 정보가 수정되었습니다.');
      } else {
        // 생성
        await userService.createUser(data as UserCreateRequest);
        alert('사용자가 등록되었습니다.');
      }
      
      // 목록 새로고침
      await fetchUsers(0);
      handleCloseModal();
    } catch (err) {
      console.error('Failed to save user', err);
      alert('사용자 정보 저장에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="card">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
        <h2 className="card-title" style={{ margin: 0 }}>사용자 관리</h2>
        <button className="btn btn-primary" onClick={handleCreateClick}>
          + 사용자 등록
        </button>
      </div>
      
      {error && <div className="login-error">{error}</div>}

      <div className="table-container">
        <table className="table">
          <thead>
            <tr>
              <th>No</th>
              <th>사용자ID</th>
              <th>사용자명</th>
              <th>이메일</th>
              <th>권한</th>
              <th>가입일</th>
              <th>관리</th>
            </tr>
          </thead>
          <tbody>
            {users.length > 0 ? (
              users.map((user, index) => (
                <tr key={user.id}>
                  <td>{totalElements - index}</td>
                  <td>{user.username}</td>
                  <td>{user.name}</td>
                  <td>{user.email}</td>
                  <td>
                    <span className={`badge ${user.role === 'ADMIN' ? 'badge-critical' : 'badge-open'}`}>
                      {user.role}
                    </span>
                  </td>
                  <td>{new Date(user.createdAt).toLocaleDateString()}</td>
                  <td>
                    <button 
                      className="btn btn-secondary" 
                      style={{ padding: '4px 8px', fontSize: '12px', marginRight: '4px' }}
                      onClick={() => handleEditClick(user)}
                    >
                      수정
                    </button>
                    <button 
                      className="btn btn-danger" 
                      style={{ padding: '4px 8px', fontSize: '12px' }}
                      onClick={() => handleDeleteClick(user.id)}
                    >
                      삭제
                    </button>
                  </td>
                </tr>
              ))
            ) : (
              !loading && (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', padding: '40px', color: '#666' }}>
                    등록된 사용자가 없습니다.
                  </td>
                </tr>
              )
            )}
          </tbody>
        </table>
        
        {/* Infinite Scroll Sentinel */}
        <div ref={observerTarget} style={{ height: '20px', margin: '10px 0', textAlign: 'center' }}>
          {loading && <div className="loading-spinner" style={{ display: 'inline-block', width: '24px', height: '24px', border: '3px solid #f3f3f3', borderTop: '3px solid #3498db' }}></div>}
        </div>
      </div>

      {isModalOpen && (
        <UserEditModal
          user={editingUser}
          onClose={handleCloseModal}
          onSave={handleSaveUser}
          loading={saving}
        />
      )}
    </div>
  );
}

export default UserList;
