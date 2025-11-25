import { useState, FormEvent } from 'react';
import { useAuth } from '../../hooks/useAuth';

/**
 * 로그인 폼 컴포넌트
 */
function LoginForm() {
  const { login, loading, error, resetError } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    resetError();
    await login({ username, password });
  };

  return (
    <form onSubmit={handleSubmit}>
      {error && <div className="login-error">{error}</div>}
      
      <div className="form-group">
        <label htmlFor="username" className="form-label">
          사용자명
        </label>
        <input
          type="text"
          id="username"
          className="form-input"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          placeholder="사용자명을 입력하세요"
          required
          disabled={loading}
        />
      </div>
      
      <div className="form-group">
        <label htmlFor="password" className="form-label">
          비밀번호
        </label>
        <input
          type="password"
          id="password"
          className="form-input"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="비밀번호를 입력하세요"
          required
          disabled={loading}
        />
      </div>
      
      <button type="submit" className="btn btn-primary" style={{ width: '100%' }} disabled={loading}>
        {loading ? '로그인 중...' : '로그인'}
      </button>
    </form>
  );
}

export default LoginForm;
