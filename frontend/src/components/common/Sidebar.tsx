import { NavLink } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';

interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
}

/**
 * 사이드바 컴포넌트
 */
function Sidebar({ isOpen, onClose }: SidebarProps) {
  const { user } = useAuth();

  const handleLinkClick = () => {
    if (window.innerWidth < 768) {
      onClose();
    }
  };

  return (
    <aside className={`sidebar ${isOpen ? 'open' : 'closed'}`}>
      <nav className="sidebar-nav">
        <NavLink 
          to="/" 
          className={({ isActive }) => `sidebar-nav-link ${isActive ? 'active' : ''}`}
          onClick={handleLinkClick}
        >
          📊 대시보드
        </NavLink>
        <NavLink 
          to="/sr" 
          className={({ isActive }) => `sidebar-nav-link ${isActive ? 'active' : ''}`}
          onClick={handleLinkClick}
        >
          📋 SR 관리
        </NavLink>
        <NavLink
          to="/survey"
          className={({ isActive }) =>
            `sidebar-nav-link ${isActive ? 'active' : ''}`
          }
          onClick={handleLinkClick}
        >
          📋 OPEN API 현황조사
        </NavLink>
        {user?.role === 'ADMIN' && (
          <NavLink 
            to="/users" 
            className={({ isActive }) => `sidebar-nav-link ${isActive ? 'active' : ''}`}
            onClick={handleLinkClick}
          >
            👥 사용자 관리
          </NavLink>
        )}
      </nav>
    </aside>
  );
}

export default Sidebar;
