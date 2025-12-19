import React, { useState, useEffect } from 'react';
import { wikiVersionApi } from '../../services/wikiService';
import type { WikiVersion } from '../../types/wiki';
import './VersionHistoryModal.css';

interface VersionHistoryModalProps {
  isOpen: boolean;
  onClose: () => void;
  documentId: number;
  onRollback: () => void;
}

const VersionHistoryModal: React.FC<VersionHistoryModalProps> = ({
  isOpen,
  onClose,
  documentId,
  onRollback
}) => {
  const [versions, setVersions] = useState<WikiVersion[]>([]);
  const [selectedVersion, setSelectedVersion] = useState<WikiVersion | null>(null);
  const [compareVersion, setCompareVersion] = useState<WikiVersion | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen && documentId) {
      loadVersions();
    }
  }, [isOpen, documentId]);

  const loadVersions = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await wikiVersionApi.getAll(documentId);
      setVersions(response.data);
    } catch (err) {
      console.error('버전 목록 로딩 실패:', err);
      setError('버전 목록을 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleRollback = async (version: number) => {
    if (!window.confirm(`버전 ${version}으로 롤백하시겠습니까?`)) {
      return;
    }

    try {
      await wikiVersionApi.rollback(documentId, version);
      alert('롤백이 완료되었습니다.');
      onRollback();
      onClose();
    } catch (err) {
      console.error('롤백 실패:', err);
      alert('롤백에 실패했습니다.');
    }
  };

  const handleViewVersion = async (version: number) => {
    try {
      const response = await wikiVersionApi.get(documentId, version);
      setSelectedVersion(response.data);
      setCompareVersion(null);
    } catch (err) {
      console.error('버전 조회 실패:', err);
      alert('버전을 불러오는데 실패했습니다.');
    }
  };

  const handleCompare = async (version: number) => {
    if (!selectedVersion) {
      alert('먼저 비교할 버전을 선택해주세요.');
      return;
    }

    try {
      const response = await wikiVersionApi.get(documentId, version);
      setCompareVersion(response.data);
    } catch (err) {
      console.error('버전 조회 실패:', err);
      alert('버전을 불러오는데 실패했습니다.');
    }
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleString('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (!isOpen) return null;

  return (
    <div className="version-modal-overlay" onClick={onClose}>
      <div className="version-modal" onClick={(e) => e.stopPropagation()}>
        <div className="version-modal-header">
          <h2>📜 버전 이력</h2>
          <button className="version-modal-close" onClick={onClose}>×</button>
        </div>

        <div className="version-modal-body">
          {loading && <div className="version-loading">로딩 중...</div>}
          {error && <div className="version-error">{error}</div>}

          {!loading && !error && (
            <div className="version-content">
              {/* 버전 목록 */}
              <div className="version-list-section">
                <h3>버전 목록</h3>
                <div className="version-list">
                  {versions.map((version) => (
                    <div
                      key={version.id}
                      className={`version-item ${selectedVersion?.version === version.version ? 'selected' : ''}`}
                    >
                      <div className="version-info">
                        <div className="version-number">
                          <strong>버전 {version.version}</strong>
                          {version.version === versions[0]?.version && (
                            <span className="version-badge">최신</span>
                          )}
                        </div>
                        <div className="version-meta">
                          <span className="version-author">{version.createdByName}</span>
                          <span className="version-date">{formatDate(version.createdAt)}</span>
                        </div>
                        {version.changeSummary && (
                          <div className="version-description">{version.changeSummary}</div>
                        )}
                      </div>
                      <div className="version-actions">
                        <button
                          className="btn-view"
                          onClick={() => handleViewVersion(version.version)}
                        >
                          보기
                        </button>
                        {selectedVersion && selectedVersion.version !== version.version && (
                          <button
                            className="btn-compare"
                            onClick={() => handleCompare(version.version)}
                          >
                            비교
                          </button>
                        )}
                        {version.version !== versions[0]?.version && (
                          <button
                            className="btn-rollback"
                            onClick={() => handleRollback(version.version)}
                          >
                            롤백
                          </button>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* 버전 내용 미리보기 */}
              {selectedVersion && (
                <div className="version-preview-section">
                  <h3>버전 {selectedVersion.version} 내용</h3>
                  <div className="version-preview">
                    <pre>{selectedVersion.content}</pre>
                  </div>

                  {compareVersion && (
                    <>
                      <h3>버전 {compareVersion.version} 내용</h3>
                      <div className="version-preview">
                        <pre>{compareVersion.content}</pre>
                      </div>
                    </>
                  )}
                </div>
              )}
            </div>
          )}
        </div>

        <div className="version-modal-footer">
          <button className="btn-close" onClick={onClose}>
            닫기
          </button>
        </div>
      </div>
    </div>
  );
};

export default VersionHistoryModal;
