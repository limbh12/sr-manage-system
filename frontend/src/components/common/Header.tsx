import { Link } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';

/**
 * 헤더 컴포넌트
 */
function Header() {
  const { user, logout } = useAuth();

  return (
    <header className="header">
      <h1>SR Management System</h1>
      <div className="header-actions">
        {user && (
          <>
            <div className="header-user">
              <Link to="/profile" style={{ color: 'inherit', textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span>{user.username}</span>
                <span className="badge badge-open">{user.role}</span>
              </Link>
            </div>
            <button className="btn btn-secondary" onClick={logout}>
              로그아웃
            </button>
          </>
        )}
      </div>
    </header>
  );
}

export default Header;
