import { Link } from 'react-router-dom';
import LoginForm from '../components/auth/LoginForm';

/**
 * 로그인 페이지
 */
function LoginPage() {
  return (
    <div className="login-page">
      <div className="login-container">
        <h2 className="login-title">
          SR Management System
        </h2>
        
        <LoginForm />
        
      </div>
    </div>
  );
}

export default LoginPage;
