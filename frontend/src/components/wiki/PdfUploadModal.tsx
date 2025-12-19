import React, { useState } from 'react';
import { wikiFileApi } from '../../services/wikiService';
import './PdfUploadModal.css';

interface PdfUploadModalProps {
  isOpen: boolean;
  onClose: () => void;
  onUploadSuccess: (documentId: number) => void;
}

const PdfUploadModal: React.FC<PdfUploadModalProps> = ({
  isOpen,
  onClose,
  onUploadSuccess
}) => {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [conversionStatus, setConversionStatus] = useState<string>('');

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      if (file.type !== 'application/pdf') {
        setError('PDF 파일만 업로드 가능합니다.');
        return;
      }
      if (file.size > 20 * 1024 * 1024) { // 20MB
        setError('파일 크기는 20MB를 초과할 수 없습니다.');
        return;
      }
      setSelectedFile(file);
      setError(null);
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      setError('파일을 선택해주세요.');
      return;
    }

    setUploading(true);
    setUploadProgress(0);
    setConversionStatus('업로드 중...');
    setError(null);

    try {
      // 업로드 및 변환 API 호출
      setUploadProgress(30);
      setConversionStatus('PDF 업로드 중...');

      const response = await wikiFileApi.uploadPdf(selectedFile);

      setUploadProgress(60);
      setConversionStatus('PDF 텍스트 추출 중...');

      setUploadProgress(90);
      setConversionStatus('마크다운 변환 완료!');

      setTimeout(() => {
        setUploadProgress(100);
        onUploadSuccess(response.data.id);
        handleClose();
      }, 500);

    } catch (err) {
      console.error('PDF 업로드 실패:', err);
      setError('PDF 업로드에 실패했습니다. 다시 시도해주세요.');
      setUploading(false);
    }
  };

  const handleClose = () => {
    setSelectedFile(null);
    setUploading(false);
    setUploadProgress(0);
    setError(null);
    setConversionStatus('');
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div className="pdf-upload-modal-overlay" onClick={handleClose}>
      <div className="pdf-upload-modal" onClick={(e) => e.stopPropagation()}>
        <div className="pdf-upload-modal-header">
          <h2>PDF 업로드 및 변환</h2>
          <button className="pdf-upload-modal-close" onClick={handleClose}>×</button>
        </div>

        <div className="pdf-upload-modal-body">
          {!uploading ? (
            <>
              <div className="pdf-upload-dropzone">
                <input
                  type="file"
                  id="pdf-file-input"
                  accept="application/pdf"
                  onChange={handleFileSelect}
                  style={{ display: 'none' }}
                />
                <label htmlFor="pdf-file-input" className="pdf-upload-dropzone-label">
                  <div className="pdf-upload-icon">📄</div>
                  <p>PDF 파일을 선택하거나 여기에 드래그하세요</p>
                  <p className="pdf-upload-hint">최대 20MB, PDF 파일만 지원</p>
                </label>
              </div>

              {selectedFile && (
                <div className="pdf-upload-selected-file">
                  <div className="pdf-file-info">
                    <span className="pdf-file-icon">📄</span>
                    <div className="pdf-file-details">
                      <div className="pdf-file-name">{selectedFile.name}</div>
                      <div className="pdf-file-size">
                        {(selectedFile.size / 1024 / 1024).toFixed(2)} MB
                      </div>
                    </div>
                  </div>
                  <button
                    className="pdf-file-remove"
                    onClick={() => setSelectedFile(null)}
                  >
                    ×
                  </button>
                </div>
              )}

              {error && (
                <div className="pdf-upload-error">
                  {error}
                </div>
              )}
            </>
          ) : (
            <div className="pdf-upload-progress">
              <div className="pdf-progress-status">{conversionStatus}</div>
              <div className="pdf-progress-bar">
                <div
                  className="pdf-progress-bar-fill"
                  style={{ width: `${uploadProgress}%` }}
                />
              </div>
              <div className="pdf-progress-percentage">{uploadProgress}%</div>
              <div className="pdf-conversion-info">
                <p>PDF를 마크다운 형식으로 변환하고 있습니다.</p>
                <p>변환이 완료되면 자동으로 Wiki 문서가 생성됩니다.</p>
              </div>
            </div>
          )}
        </div>

        <div className="pdf-upload-modal-footer">
          <button
            className="pdf-upload-button-cancel"
            onClick={handleClose}
            disabled={uploading}
          >
            취소
          </button>
          <button
            className="pdf-upload-button-upload"
            onClick={handleUpload}
            disabled={!selectedFile || uploading}
          >
            {uploading ? '변환 중...' : '업로드 및 변환'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default PdfUploadModal;
