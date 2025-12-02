import { Link } from 'react-router-dom';
import RegisterForm from '../components/auth/RegisterForm';

function RegisterPage() {
  return (
    <div className="login-container">
      <div className="login-card">
        <h1 className="login-title">회원가입</h1>
        <p className="login-subtitle">SR 관리 시스템에 오신 것을 환영합니다</p>
        
        <RegisterForm />
        
        <div className="login-footer">
          이미 계정이 있으신가요? <Link to="/login">로그인</Link>
        </div>
      </div>
    </div>
  );
}

export default RegisterPage;
