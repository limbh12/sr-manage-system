import { Link } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';

interface HeaderProps {
  onToggleSidebar: () => void;
}

/**
 * 헤더 컴포넌트
 */
function Header({ onToggleSidebar }: HeaderProps) {
  const { user, logout } = useAuth();

  return (
    <header className="header">
      <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
        <button 
          className="btn-icon" 
          onClick={onToggleSidebar}
          aria-label="메뉴 토글"
          style={{ background: 'none', border: 'none', color: 'white', cursor: 'pointer', fontSize: '1.2rem', padding: '4px' }}
        >
          ☰
        </button>
        <h1 className="header-title">SR Management System</h1>
      </div>
      <div className="header-actions">
        {user && (
          <>
            <div className="header-user">
              <Link to="/profile" style={{ color: 'inherit', textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span className="user-name">{user.username}</span>
                <span className="badge badge-open">{user.role}</span>
              </Link>
            </div>
            <button className="btn btn-secondary header-logout-btn" onClick={logout}>
              로그아웃
            </button>
          </>
        )}
      </div>
    </header>
  );
}

export default Header;
