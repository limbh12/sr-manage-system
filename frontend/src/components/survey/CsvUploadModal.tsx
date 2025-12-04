import React, { useState, useCallback } from 'react';
import { surveyService, BulkUploadResult } from '../../services/surveyService';

interface CsvUploadModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

const CsvUploadModal: React.FC<CsvUploadModalProps> = ({ isOpen, onClose, onSuccess }) => {
  const [isDragging, setIsDragging] = useState(false);
  const [file, setFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [result, setResult] = useState<BulkUploadResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  }, []);

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
  }, []);

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    const droppedFile = e.dataTransfer.files[0];
    if (droppedFile && droppedFile.type === 'text/csv') {
      setFile(droppedFile);
      setError(null);
      setResult(null);
    } else {
      setError('CSV 파일만 업로드 가능합니다.');
    }
  }, []);

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = e.target.files?.[0];
    if (selectedFile) {
      setFile(selectedFile);
      setError(null);
      setResult(null);
    }
  };

  const handleUpload = async () => {
    if (!file) return;

    setUploading(true);
    setError(null);
    try {
      const result = await surveyService.uploadSurveyCsv(file);
      setResult(result);
      if (result.successCount > 0) {
        onSuccess();
      }
    } catch (err: any) {
      setError(err.response?.data?.message || '파일 업로드 중 오류가 발생했습니다.');
    } finally {
      setUploading(false);
    }
  };

  const handleClose = () => {
    setFile(null);
    setResult(null);
    setError(null);
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div className="modal-overlay">
      <div className="modal" style={{ maxWidth: '600px' }}>
        <div className="modal-header">
          <h2 className="modal-title">현황조사 일괄 등록 (CSV)</h2>
          <button onClick={handleClose} className="modal-close">
            ✕
          </button>
        </div>

        {!result ? (
          <div className="space-y-4">
            <div
              onDragOver={handleDragOver}
              onDragLeave={handleDragLeave}
              onDrop={handleDrop}
              style={{
                border: `2px dashed ${isDragging ? '#2196f3' : '#ccc'}`,
                borderRadius: '8px',
                padding: '32px',
                textAlign: 'center',
                backgroundColor: isDragging ? '#e3f2fd' : '#fafafa',
                transition: 'all 0.2s',
                cursor: 'pointer'
              }}
            >
              <div style={{ marginBottom: '16px' }}>
                <svg style={{ width: '48px', height: '48px', color: '#9e9e9e', margin: '0 auto' }} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
                </svg>
              </div>
              <p style={{ color: '#666', marginBottom: '8px' }}>
                CSV 파일을 이곳에 드래그하거나 클릭하여 선택하세요
              </p>
              <input
                type="file"
                accept=".csv"
                onChange={handleFileSelect}
                style={{ display: 'none' }}
                id="file-upload"
              />
              <label
                htmlFor="file-upload"
                className="btn btn-secondary"
                style={{ display: 'inline-block', marginTop: '8px' }}
              >
                파일 선택
              </label>
              {file && (
                <p style={{ marginTop: '12px', color: '#1976d2', fontWeight: 500 }}>
                  선택된 파일: {file.name}
                </p>
              )}
            </div>

            {error && (
              <div style={{ 
                backgroundColor: '#ffebee', 
                border: '1px solid #ffcdd2', 
                color: '#c62828', 
                padding: '12px', 
                borderRadius: '4px',
                marginTop: '16px'
              }}>
                {error}
              </div>
            )}

            <div className="modal-footer">
              <button
                onClick={handleClose}
                className="btn btn-secondary"
              >
                취소
              </button>
              <button
                onClick={handleUpload}
                disabled={!file || uploading}
                className="btn btn-primary"
                style={{ opacity: (!file || uploading) ? 0.5 : 1 }}
              >
                {uploading ? '업로드 중...' : '업로드'}
              </button>
            </div>
            
            <div style={{ marginTop: '16px', fontSize: '13px', color: '#757575' }}>
              <p>※ 템플릿 파일 형식(CSV)을 준수해야 합니다.</p>
              <p>※ 첫 번째 열은 반드시 '기관명'이어야 하며, 시스템에 등록된 기관명과 일치해야 합니다.</p>
            </div>
          </div>
        ) : (
          <div className="space-y-4">
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '16px', textAlign: 'center', marginBottom: '24px' }}>
              <div style={{ backgroundColor: '#f5f5f5', padding: '16px', borderRadius: '8px' }}>
                <div style={{ fontSize: '13px', color: '#757575' }}>총 건수</div>
                <div style={{ fontSize: '24px', fontWeight: 'bold' }}>{result.totalCount}</div>
              </div>
              <div style={{ backgroundColor: '#e8f5e9', padding: '16px', borderRadius: '8px' }}>
                <div style={{ fontSize: '13px', color: '#2e7d32' }}>성공</div>
                <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#2e7d32' }}>{result.successCount}</div>
              </div>
              <div style={{ backgroundColor: '#ffebee', padding: '16px', borderRadius: '8px' }}>
                <div style={{ fontSize: '13px', color: '#c62828' }}>실패</div>
                <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#c62828' }}>{result.failureCount}</div>
              </div>
            </div>

            {result.failures.length > 0 && (
              <div>
                <h3 style={{ fontSize: '16px', fontWeight: 600, marginBottom: '8px', color: '#c62828' }}>실패 상세 내역</h3>
                <div style={{ 
                  backgroundColor: '#fafafa', 
                  border: '1px solid #eee', 
                  borderRadius: '8px', 
                  maxHeight: '240px', 
                  overflowY: 'auto' 
                }}>
                  <table className="table" style={{ fontSize: '13px' }}>
                    <thead>
                      <tr>
                        <th style={{ width: '60px' }}>행</th>
                        <th>데이터(기관명)</th>
                        <th>사유</th>
                      </tr>
                    </thead>
                    <tbody>
                      {result.failures.map((fail, idx) => (
                        <tr key={idx}>
                          <td>{fail.rowNumber}</td>
                          <td>{fail.data}</td>
                          <td style={{ color: '#c62828' }}>{fail.reason}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            <div className="modal-footer">
              <button
                onClick={handleClose}
                className="btn btn-primary"
              >
                확인
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default CsvUploadModal;
