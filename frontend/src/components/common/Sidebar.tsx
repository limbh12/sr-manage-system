import { NavLink } from 'react-router-dom';

/**
 * 사이드바 컴포넌트
 */
function Sidebar() {
  return (
    <aside className="sidebar">
      <nav>
        <ul className="sidebar-nav">
          <li className="sidebar-nav-item">
            <NavLink
              to="/dashboard"
              className={({ isActive }) =>
                `sidebar-nav-link ${isActive ? 'active' : ''}`
              }
            >
              📊 대시보드
            </NavLink>
          </li>
          <li className="sidebar-nav-item">
            <NavLink
              to="/sr"
              className={({ isActive }) =>
                `sidebar-nav-link ${isActive ? 'active' : ''}`
              }
            >
              📋 SR 관리
            </NavLink>
          </li>
        </ul>
      </nav>
    </aside>
  );
}

export default Sidebar;
