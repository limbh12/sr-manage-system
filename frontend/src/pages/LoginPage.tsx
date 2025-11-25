import { useState } from 'react';
import { Link } from 'react-router-dom';
import LoginForm from '../components/auth/LoginForm';
import RegisterForm from '../components/auth/RegisterForm';

/**
 * 로그인 페이지
 */
function LoginPage() {
  const [isRegisterMode, setIsRegisterMode] = useState(false);

  return (
    <div className="login-page">
      <div className="login-container">
        <h2 className="login-title">
          {isRegisterMode ? '회원가입' : 'SR Management System'}
        </h2>
        
        {isRegisterMode ? <RegisterForm /> : <LoginForm />}
        
        <div style={{ marginTop: '24px', textAlign: 'center' }}>
          {isRegisterMode ? (
            <p>
              이미 계정이 있으신가요?{' '}
              <Link
                to="#"
                onClick={(e) => {
                  e.preventDefault();
                  setIsRegisterMode(false);
                }}
                style={{ color: '#1976d2' }}
              >
                로그인
              </Link>
            </p>
          ) : (
            <p>
              계정이 없으신가요?{' '}
              <Link
                to="#"
                onClick={(e) => {
                  e.preventDefault();
                  setIsRegisterMode(true);
                }}
                style={{ color: '#1976d2' }}
              >
                회원가입
              </Link>
            </p>
          )}
        </div>
      </div>
    </div>
  );
}

export default LoginPage;
