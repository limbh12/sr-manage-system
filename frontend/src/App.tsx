import { useState, useEffect } from 'react';
import { Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { RootState } from './store';
import { useAuth } from './hooks/useAuth';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import SrManagementPage from './pages/SrManagementPage';
import UserManagementPage from './pages/UserManagementPage';
import ProfilePage from './pages/ProfilePage';
import Header from './components/common/Header';
import Sidebar from './components/common/Sidebar';
import Loading from './components/common/Loading';
import './App.css';
import OpenApiSurveyPage from './pages/OpenApiSurveyPage';
import SurveyForm from './components/survey/SurveyForm';
import CommonCodePage from './pages/CommonCodePage';
import WikiPage from './pages/WikiPage';
import NotificationsPage from './pages/NotificationsPage';

/**
 * 메인 App 컴포넌트
 */
function App() {
  const { isAuthenticated, loading, user } = useAuth();
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);
  const location = useLocation();
  const theme = useSelector((state: RootState) => state.theme.mode);

  // 테마 적용
  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
  }, [theme]);

  // 화면 크기에 따른 사이드바 초기 상태 설정 및 리사이즈 이벤트 핸들러
  useEffect(() => {
    const handleResize = () => {
      if (window.innerWidth < 768) {
        setIsSidebarOpen(false);
      } else {
        setIsSidebarOpen(true);
      }
    };

    // 초기 실행
    handleResize();

    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  // 모바일에서 라우트 변경 시 사이드바 닫기
  useEffect(() => {
    if (window.innerWidth < 768) {
      setIsSidebarOpen(false);
    }
  }, [location]);

  const toggleSidebar = () => {
    setIsSidebarOpen(!isSidebarOpen);
  };

  if (loading) {
    return <Loading />;
  }

  return (
    <div className="app-layout">
      {isAuthenticated && <Header onToggleSidebar={toggleSidebar} />}
      <div className="app-content">
        {isAuthenticated && (
          <>
            <Sidebar isOpen={isSidebarOpen} onClose={() => setIsSidebarOpen(false)} />
            {/* 모바일에서 사이드바가 열려있을 때 배경 오버레이 */}
            <div 
              className={`sidebar-overlay ${isSidebarOpen ? 'visible' : ''}`}
              onClick={() => setIsSidebarOpen(false)}
            />
          </>
        )}
        <main className={`main-content ${!isSidebarOpen ? 'expanded' : ''}`}>
          <Routes>
            <Route path="/login" element={!isAuthenticated ? <LoginPage /> : <Navigate to="/" />} />
            
            {/* Protected Routes */}
            <Route path="/" element={isAuthenticated ? <DashboardPage /> : <Navigate to="/login" />} />
            <Route path="/sr" element={isAuthenticated ? <SrManagementPage /> : <Navigate to="/login" />} />
            <Route path="/profile" element={isAuthenticated ? <ProfilePage /> : <Navigate to="/login" />} />
            
            {/* OPEN API 현황조사 라우트 추가 */}
            <Route path="/survey" element={isAuthenticated ? <OpenApiSurveyPage /> : <Navigate to="/login" />} />
            <Route path="/survey/new" element={isAuthenticated ? <SurveyForm /> : <Navigate to="/login" />} />
            <Route path="/survey/:id" element={isAuthenticated ? <SurveyForm /> : <Navigate to="/login" />} />

            {/* Wiki 라우트 추가 */}
            <Route path="/wiki" element={isAuthenticated ? <WikiPage /> : <Navigate to="/login" />} />
            <Route path="/wiki/:id" element={isAuthenticated ? <WikiPage /> : <Navigate to="/login" />} />

            {/* 알림 라우트 추가 */}
            <Route path="/notifications" element={isAuthenticated ? <NotificationsPage /> : <Navigate to="/login" />} />

            {/* Admin Routes */}
            <Route 
              path="/users" 
              element={
                isAuthenticated && user?.role === 'ADMIN' 
                  ? <UserManagementPage /> 
                  : <Navigate to="/" />
              } 
            />
            <Route 
              path="/admin/codes" 
              element={
                isAuthenticated && user?.role === 'ADMIN' 
                  ? <CommonCodePage /> 
                  : <Navigate to="/" />
              } 
            />

            <Route path="*" element={<Navigate to="/" />} />
          </Routes>
        </main>
      </div>
    </div>
  );
}

export default App;
