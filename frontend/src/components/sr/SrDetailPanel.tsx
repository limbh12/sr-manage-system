import React, { useEffect, useState } from 'react';
import SlidePanel from '../common/SlidePanel';
import WikiDetailPanel from '../wiki/WikiDetailPanel';
import { getSrById } from '../../services/srService';
import type { Sr } from '../../types';
import SrDetail from './SrDetail';

interface SrDetailPanelProps {
  srId: number | null;
  onClose: () => void;
}

const SrDetailPanel: React.FC<SrDetailPanelProps> = ({ srId, onClose }) => {
  const [sr, setSr] = useState<Sr | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedWikiId, setSelectedWikiId] = useState<number | null>(null);

  useEffect(() => {
    if (srId) {
      loadSr();
    }
  }, [srId]);

  const loadSr = async () => {
    if (!srId) return;

    try {
      setLoading(true);
      setError(null);
      const data = await getSrById(srId);
      setSr(data);
    } catch (err) {
      console.error('SR 조회 실패:', err);
      setError('SR을 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleEdit = () => {
    // 패널에서는 수정 불가 (원래 페이지로 이동)
    alert('편집하려면 SR 관리 페이지로 이동하세요.');
  };

  const handleStatusChange = () => {
    // 패널에서는 상태 변경 불가
    alert('상태를 변경하려면 SR 관리 페이지로 이동하세요.');
  };

  return (
    <>
    <SlidePanel
      isOpen={srId !== null}
      onClose={onClose}
      title={sr ? `SR #${sr.id}: ${sr.title}` : 'SR 상세'}
      width="60%"
    >
      {loading && (
        <div style={{ textAlign: 'center', padding: '40px', color: '#586069' }}>
          로딩 중...
        </div>
      )}

      {error && (
        <div style={{
          padding: '20px',
          color: '#d73a49',
          background: '#ffeef0',
          borderRadius: '4px',
          border: '1px solid #fdb8c0'
        }}>
          {error}
        </div>
      )}

      {!loading && !error && sr && (
        <SrDetail
          sr={sr}
          onClose={onClose}
          onEdit={handleEdit}
          onStatusChange={handleStatusChange}
          onWikiClick={setSelectedWikiId}
          isModal={false}
        />
      )}
    </SlidePanel>

      {/* Wiki 상세 슬라이드 패널 (SR 패널 위에 표시) */}
      <WikiDetailPanel
        documentId={selectedWikiId}
        onClose={() => setSelectedWikiId(null)}
        onSrClick={() => {
          setSelectedWikiId(null); // Wiki 패널 닫기
          // SR 패널이 이미 열려 있으므로 자동으로 새 SR로 교체됨
        }}
      />
    </>
  );
};

export default SrDetailPanel;
