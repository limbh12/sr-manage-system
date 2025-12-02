import { useState, useEffect } from 'react';
import { useAuth } from '../hooks/useAuth';
import * as userService from '../services/userService';
import { UserUpdateRequest } from '../types';

function ProfilePage() {
  const { user, checkAuth } = useAuth();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null);

  useEffect(() => {
    if (user) {
      setName(user.name);
      setEmail(user.email);
    }
  }, [user]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setMessage(null);

    try {
      const updateData: UserUpdateRequest = {
        name,
        email,
      };
      
      await userService.updateMyProfile(updateData);
      await checkAuth(); // 사용자 정보 갱신
      
      setMessage({ type: 'success', text: '내 정보가 수정되었습니다.' });
    } catch (err) {
      console.error('Failed to update profile', err);
      setMessage({ type: 'error', text: '정보 수정에 실패했습니다.' });
    } finally {
      setLoading(false);
    }
  };

  if (!user) return null;

  return (
    <div>
      <div className="page-header">
        <h2 className="page-title">내 정보 수정</h2>
      </div>

      <div className="card" style={{ maxWidth: '600px', margin: '0 auto' }}>
        {message && (
          <div className={`alert ${message.type === 'success' ? 'alert-success' : 'alert-error'}`} 
               style={{ 
                 padding: '12px', 
                 marginBottom: '16px', 
                 borderRadius: '4px',
                 backgroundColor: message.type === 'success' ? '#e8f5e9' : '#ffebee',
                 color: message.type === 'success' ? '#2e7d32' : '#c62828',
                 border: `1px solid ${message.type === 'success' ? '#c8e6c9' : '#ffcdd2'}`
               }}>
            {message.text}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">사용자 ID</label>
            <input
              type="text"
              className="form-input"
              value={user.username}
              disabled
              style={{ backgroundColor: '#f5f5f5' }}
            />
          </div>

          <div className="form-group">
            <label className="form-label">권한</label>
            <input
              type="text"
              className="form-input"
              value={user.role === 'ADMIN' ? '관리자' : '일반 사용자'}
              disabled
              style={{ backgroundColor: '#f5f5f5' }}
            />
          </div>

          <div className="form-group">
            <label className="form-label">이름</label>
            <input
              type="text"
              className="form-input"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">이메일</label>
            <input
              type="email"
              className="form-input"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>

          <div style={{ marginTop: '24px', display: 'flex', justifyContent: 'flex-end' }}>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? '저장 중...' : '저장'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default ProfilePage;
