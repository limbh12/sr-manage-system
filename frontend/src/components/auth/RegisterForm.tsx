import { useState, FormEvent } from 'react';
import { useAuth } from '../../hooks/useAuth';

/**
 * 회원가입 폼 컴포넌트
 */
function RegisterForm() {
  const { register, loading, error, resetError } = useAuth();
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [localError, setLocalError] = useState('');

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    resetError();
    setLocalError('');

    if (password !== confirmPassword) {
      setLocalError('비밀번호가 일치하지 않습니다.');
      return;
    }

    if (password.length < 6) {
      setLocalError('비밀번호는 6자 이상이어야 합니다.');
      return;
    }

    await register({ username, email, password });
  };

  return (
    <form onSubmit={handleSubmit}>
      {(error || localError) && (
        <div className="login-error">{error || localError}</div>
      )}
      
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
        <label htmlFor="email" className="form-label">
          이메일
        </label>
        <input
          type="email"
          id="email"
          className="form-input"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="이메일을 입력하세요"
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
      
      <div className="form-group">
        <label htmlFor="confirmPassword" className="form-label">
          비밀번호 확인
        </label>
        <input
          type="password"
          id="confirmPassword"
          className="form-input"
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
          placeholder="비밀번호를 다시 입력하세요"
          required
          disabled={loading}
        />
      </div>
      
      <button type="submit" className="btn btn-primary" style={{ width: '100%' }} disabled={loading}>
        {loading ? '가입 중...' : '회원가입'}
      </button>
    </form>
  );
}

export default RegisterForm;
