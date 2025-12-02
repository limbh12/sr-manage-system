import { useState, useEffect, FormEvent } from 'react';
import { User, UserUpdateRequest, UserCreateRequest, Role } from '../../types';

interface UserEditModalProps {
  user?: User | null; // null이면 생성 모드
  onClose: () => void;
  onSave: (id: number | null, data: UserUpdateRequest | UserCreateRequest) => void;
  loading?: boolean;
}

function UserEditModal({ user, onClose, onSave, loading = false }: UserEditModalProps) {
  const isEditMode = !!user;
  
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [role, setRole] = useState<Role>('USER');

  useEffect(() => {
    if (user) {
      setUsername(user.username);
      setName(user.name || '');
      setEmail(user.email);
      setRole(user.role);
    } else {
      // 초기화
      setUsername('');
      setPassword('');
      setName('');
      setEmail('');
      setRole('USER');
    }
  }, [user]);

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

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    
    if (isEditMode && user) {
      onSave(user.id, { name, email, role });
    } else {
      onSave(null, { username, password, name, email, role });
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2 className="modal-title">{isEditMode ? '사용자 정보 수정' : '사용자 등록'}</h2>
          <button className="modal-close" onClick={onClose}>
            ×
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">사용자ID</label>
            <input
              type="text"
              className="form-input"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              disabled={isEditMode}
              required
              style={isEditMode ? { backgroundColor: '#f5f5f5' } : {}}
            />
          </div>

          {!isEditMode && (
            <div className="form-group">
              <label className="form-label">비밀번호</label>
              <input
                type="password"
                className="form-input"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                placeholder="초기 비밀번호"
              />
            </div>
          )}

          <div className="form-group">
            <label htmlFor="name" className="form-label">
              사용자명
            </label>
            <input
              type="text"
              id="name"
              className="form-input"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="email" className="form-label">
              이메일
            </label>
            <input
              type="email"
              id="email"
              className="form-input"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="role" className="form-label">
              권한
            </label>
            <select
              id="role"
              className="form-select"
              value={role}
              onChange={(e) => setRole(e.target.value as Role)}
              disabled={loading}
            >
              <option value="USER">일반 사용자 (USER)</option>
              <option value="ADMIN">관리자 (ADMIN)</option>
            </select>
          </div>

          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose} disabled={loading}>
              취소
            </button>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? '저장 중...' : (isEditMode ? '수정' : '등록')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default UserEditModal;
