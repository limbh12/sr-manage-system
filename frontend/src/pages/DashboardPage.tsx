import { useEffect } from 'react';
import { useSr } from '../hooks/useSr';
import { useAuth } from '../hooks/useAuth';
import Loading from '../components/common/Loading';

/**
 * 대시보드 페이지
 */
function DashboardPage() {
  const { user } = useAuth();
  const { srList, totalElements, loading, fetchSrList } = useSr();

  useEffect(() => {
    fetchSrList({ size: 5 });
  }, [fetchSrList]);

  // 상태별 SR 수 계산
  const statusCounts = {
    OPEN: srList.filter((sr) => sr.status === 'OPEN').length,
    IN_PROGRESS: srList.filter((sr) => sr.status === 'IN_PROGRESS').length,
    RESOLVED: srList.filter((sr) => sr.status === 'RESOLVED').length,
    CLOSED: srList.filter((sr) => sr.status === 'CLOSED').length,
  };

  // 우선순위별 SR 수 계산
  const priorityCounts = {
    CRITICAL: srList.filter((sr) => sr.priority === 'CRITICAL').length,
    HIGH: srList.filter((sr) => sr.priority === 'HIGH').length,
    MEDIUM: srList.filter((sr) => sr.priority === 'MEDIUM').length,
    LOW: srList.filter((sr) => sr.priority === 'LOW').length,
  };

  if (loading) {
    return <Loading />;
  }

  return (
    <div>
      <h2 style={{ marginBottom: '24px' }}>
        안녕하세요, {user?.username}님! 👋
      </h2>

      {/* 요약 카드 */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '16px', marginBottom: '24px' }}>
        <div className="card" style={{ textAlign: 'center' }}>
          <h3 style={{ color: '#666', marginBottom: '8px' }}>전체 SR</h3>
          <p style={{ fontSize: '2rem', fontWeight: 'bold', color: '#1976d2' }}>
            {totalElements}
          </p>
        </div>
        <div className="card" style={{ textAlign: 'center' }}>
          <h3 style={{ color: '#666', marginBottom: '8px' }}>신규</h3>
          <p style={{ fontSize: '2rem', fontWeight: 'bold', color: '#2196f3' }}>
            {statusCounts.OPEN}
          </p>
        </div>
        <div className="card" style={{ textAlign: 'center' }}>
          <h3 style={{ color: '#666', marginBottom: '8px' }}>처리중</h3>
          <p style={{ fontSize: '2rem', fontWeight: 'bold', color: '#ff9800' }}>
            {statusCounts.IN_PROGRESS}
          </p>
        </div>
        <div className="card" style={{ textAlign: 'center' }}>
          <h3 style={{ color: '#666', marginBottom: '8px' }}>해결됨</h3>
          <p style={{ fontSize: '2rem', fontWeight: 'bold', color: '#4caf50' }}>
            {statusCounts.RESOLVED}
          </p>
        </div>
      </div>

      {/* 우선순위별 현황 */}
      <div className="card">
        <h3 className="card-title">우선순위별 현황</h3>
        <div style={{ display: 'flex', gap: '24px', flexWrap: 'wrap' }}>
          <div>
            <span className="badge badge-critical">긴급</span>
            <span style={{ marginLeft: '8px', fontWeight: 'bold' }}>
              {priorityCounts.CRITICAL}건
            </span>
          </div>
          <div>
            <span className="badge badge-high">높음</span>
            <span style={{ marginLeft: '8px', fontWeight: 'bold' }}>
              {priorityCounts.HIGH}건
            </span>
          </div>
          <div>
            <span className="badge badge-medium">보통</span>
            <span style={{ marginLeft: '8px', fontWeight: 'bold' }}>
              {priorityCounts.MEDIUM}건
            </span>
          </div>
          <div>
            <span className="badge badge-low">낮음</span>
            <span style={{ marginLeft: '8px', fontWeight: 'bold' }}>
              {priorityCounts.LOW}건
            </span>
          </div>
        </div>
      </div>

      {/* 최근 SR */}
      <div className="card">
        <h3 className="card-title">최근 등록된 SR</h3>
        {srList.length === 0 ? (
          <p style={{ color: '#666' }}>등록된 SR이 없습니다.</p>
        ) : (
          <div className="table-container">
            <table className="table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>제목</th>
                  <th>상태</th>
                  <th>우선순위</th>
                  <th>등록일(접수일)</th>
                </tr>
              </thead>
              <tbody>
                {srList.slice(0, 5).map((sr) => (
                  <tr key={sr.id}>
                    <td>{sr.id}</td>
                    <td>{sr.title}</td>
                    <td>
                      <span
                        className={`badge badge-${sr.status.toLowerCase().replace('_', '-')}`}
                      >
                        {sr.status === 'OPEN' && '신규'}
                        {sr.status === 'IN_PROGRESS' && '처리중'}
                        {sr.status === 'RESOLVED' && '해결됨'}
                        {sr.status === 'CLOSED' && '종료'}
                      </span>
                    </td>
                    <td>
                      <span className={`badge badge-${sr.priority.toLowerCase()}`}>
                        {sr.priority === 'LOW' && '낮음'}
                        {sr.priority === 'MEDIUM' && '보통'}
                        {sr.priority === 'HIGH' && '높음'}
                        {sr.priority === 'CRITICAL' && '긴급'}
                      </span>
                    </td>
                    <td>{new Date(sr.createdAt).toLocaleDateString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

export default DashboardPage;
