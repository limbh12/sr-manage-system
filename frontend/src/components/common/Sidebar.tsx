import { NavLink } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';

/**
 * 사이드바 컴포넌트
 */
function Sidebar() {
  const { user } = useAuth();

  return (
    <aside className="sidebar">
      <nav className="sidebar-nav">
        <NavLink to="/" className={({ isActive }) => `sidebar-nav-link ${isActive ? 'active' : ''}`}>
          📊 대시보드
        </NavLink>
        <NavLink to="/sr" className={({ isActive }) => `sidebar-nav-link ${isActive ? 'active' : ''}`}>
          📋 SR 관리
        </NavLink>
        <NavLink
          to="/survey"
          className={({ isActive }) =>
            `sidebar-nav-link ${isActive ? 'active' : ''}`
          }
        >
          📋 OPEN API 현황조사
        </NavLink>
        {user?.role === 'ADMIN' && (
          <NavLink to="/users" className={({ isActive }) => `sidebar-nav-link ${isActive ? 'active' : ''}`}>
            👥 사용자 관리
          </NavLink>
        )}
      </nav>
    </aside>
  );
}

export default Sidebar;
