import { Routes, Route, Navigate } from 'react-router-dom';
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
import SurveyDetail from './components/survey/SurveyDetail';

/**
 * 메인 App 컴포넌트
 */
function App() {
  const { isAuthenticated, loading, user } = useAuth();

  if (loading) {
    return <Loading />;
  }

  return (
    <div className="app-layout">
      {isAuthenticated && <Header />}
      <div className="app-content">
        {isAuthenticated && <Sidebar />}
        <main className="main-content">
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

            {/* Admin Routes */}
            <Route 
              path="/users" 
              element={
                isAuthenticated && user?.role === 'ADMIN' 
                  ? <UserManagementPage /> 
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
