import { useEffect, useState } from 'react';
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

  // 수정/생성 모달 상태
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [saving, setSaving] = useState(false);

  const fetchUsers = async (pageNumber: number) => {
    setLoading(true);
    setError(null);
    try {
      const response: PageResponse<User> = await userService.getUsers(pageNumber, pageSize);
      setUsers(response.content);
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

  const handlePageChange = (newPage: number) => {
    if (newPage >= 0 && newPage < totalPages) {
      fetchUsers(newPage);
    }
  };

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
        await fetchUsers(page);
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
      await fetchUsers(page);
      handleCloseModal();
    } catch (err) {
      console.error('Failed to save user', err);
      alert('사용자 정보 저장에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  };

  if (loading && users.length === 0) {
    return <Loading />;
  }

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
                  <td>{totalElements - (page * pageSize) - index}</td>
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
              <tr>
                <td colSpan={7} style={{ textAlign: 'center', padding: '40px', color: '#666' }}>
                  등록된 사용자가 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {totalPages > 1 && (
        <div className="pagination">
          <button
            className="pagination-btn"
            onClick={() => handlePageChange(page - 1)}
            disabled={page === 0 || loading}
          >
            이전
          </button>
          <span style={{ margin: '0 12px' }}>
            {page + 1} / {totalPages}
          </span>
          <button
            className="pagination-btn"
            onClick={() => handlePageChange(page + 1)}
            disabled={page === totalPages - 1 || loading}
          >
            다음
          </button>
        </div>
      )}

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
