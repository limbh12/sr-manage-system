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
              <span>{user.username}</span>
              <span className="badge badge-open">{user.role}</span>
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
