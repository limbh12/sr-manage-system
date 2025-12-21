import React, { useState, useEffect } from 'react';
import { wikiFileApi, wikiCategoryApi, EnhancedPdfConversionResponse } from '../../services/wikiService';
import type { WikiCategory, WikiDocument } from '../../types/wiki';
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
  const [selectedCategoryId, setSelectedCategoryId] = useState<number | undefined>(undefined);
  const [categories, setCategories] = useState<WikiCategory[]>([]);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [conversionStatus, setConversionStatus] = useState<string>('');
  const [isDragging, setIsDragging] = useState(false);
  // AI 구조 보정 옵션 (D-3)
  const [enableAiEnhancement, setEnableAiEnhancement] = useState(true);
  const [aiEnhancementResult, setAiEnhancementResult] = useState<{
    tablesFound?: number;
    formulasFound?: number;
    aiEnhanced?: boolean;
  } | null>(null);

  useEffect(() => {
    if (isOpen) {
      loadCategories();
    }
  }, [isOpen]);

  const loadCategories = async () => {
    try {
      const response = await wikiCategoryApi.getAll();
      setCategories(response.data);
    } catch (err) {
      console.error('카테고리 로딩 실패:', err);
    }
  };

  const validateAndSetFile = (file: File) => {
    if (file.type !== 'application/pdf') {
      setError('PDF 파일만 업로드 가능합니다.');
      return false;
    }
    if (file.size > 20 * 1024 * 1024) { // 20MB
      setError('파일 크기는 20MB를 초과할 수 없습니다.');
      return false;
    }
    setSelectedFile(file);
    setError(null);
    return true;
  };

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      validateAndSetFile(file);
    }
  };

  const handleDragOver = (event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    event.stopPropagation();
    setIsDragging(true);
  };

  const handleDragLeave = (event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    event.stopPropagation();
    setIsDragging(false);
  };

  const handleDrop = (event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    event.stopPropagation();
    setIsDragging(false);

    const file = event.dataTransfer.files?.[0];
    if (file) {
      validateAndSetFile(file);
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
    setAiEnhancementResult(null);

    try {
      // 업로드 및 변환 API 호출
      setUploadProgress(30);
      setConversionStatus('PDF 업로드 중...');

      // AI 구조 보정 옵션에 따라 다른 API 호출
      let documentId: number;
      let statusMessage = '마크다운 변환 완료!';

      if (enableAiEnhancement) {
        setUploadProgress(40);
        setConversionStatus('PDF 텍스트 추출 중...');

        const enhancedResponse = await wikiFileApi.uploadPdfWithAiEnhancement(
          selectedFile,
          selectedCategoryId,
          enableAiEnhancement
        );

        setUploadProgress(70);
        setConversionStatus('AI 구조 보정 적용 중 (표/수식 인식)...');

        const enhancedData: EnhancedPdfConversionResponse = enhancedResponse.data;

        // AI 보정 결과 저장
        if (enhancedData.aiEnhanced) {
          setAiEnhancementResult({
            aiEnhanced: enhancedData.aiEnhanced,
            tablesFound: enhancedData.tablesFound,
            formulasFound: enhancedData.formulasFound
          });
          statusMessage = `마크다운 변환 완료! (표 ${enhancedData.tablesFound || 0}개, 수식 ${enhancedData.formulasFound || 0}개 인식)`;
        }

        documentId = enhancedData.document.id;
      } else {
        const basicResponse = await wikiFileApi.uploadPdf(selectedFile, selectedCategoryId);
        const basicData: WikiDocument = basicResponse.data;
        documentId = basicData.id;
      }

      setUploadProgress(90);
      setConversionStatus(statusMessage);

      setTimeout(() => {
        setUploadProgress(100);
        onUploadSuccess(documentId);
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
    setSelectedCategoryId(undefined);
    setUploading(false);
    setUploadProgress(0);
    setError(null);
    setConversionStatus('');
    setEnableAiEnhancement(true);
    setAiEnhancementResult(null);
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
              {/* 카테고리 선택 */}
              <div className="pdf-category-selector">
                <label htmlFor="category-select" className="pdf-category-label">
                  카테고리 선택 (선택사항)
                </label>
                <select
                  id="category-select"
                  className="pdf-category-select"
                  value={selectedCategoryId || ''}
                  onChange={(e) => setSelectedCategoryId(e.target.value ? Number(e.target.value) : undefined)}
                >
                  <option value="">카테고리 없음</option>
                  {categories.map((category) => (
                    <option key={category.id} value={category.id}>
                      {category.parentId ? `└ ${category.name}` : category.name}
                    </option>
                  ))}
                </select>
              </div>

              {/* AI 구조 보정 옵션 */}
              <div className="pdf-ai-enhancement-option">
                <label className="pdf-ai-enhancement-checkbox">
                  <input
                    type="checkbox"
                    checked={enableAiEnhancement}
                    onChange={(e) => setEnableAiEnhancement(e.target.checked)}
                  />
                  <span className="pdf-ai-enhancement-text">
                    AI 구조 보정 적용
                  </span>
                </label>
                <p className="pdf-ai-enhancement-hint">
                  표(Table)와 수식(LaTeX)을 자동 인식하여 마크다운으로 변환합니다
                </p>
              </div>

              {/* 파일 드롭존 */}
              <div
                className={`pdf-upload-dropzone ${isDragging ? 'dragging' : ''}`}
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDrop}
              >
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

              {/* AI 보정 결과 표시 */}
              {aiEnhancementResult && aiEnhancementResult.aiEnhanced && (
                <div className="pdf-ai-enhancement-result">
                  <div className="pdf-ai-enhancement-result-title">AI 구조 보정 결과</div>
                  <div>
                    <span className="pdf-ai-enhancement-result-item">
                      표(Table): <span className="pdf-ai-enhancement-result-badge">{aiEnhancementResult.tablesFound || 0}개</span>
                    </span>
                    <span className="pdf-ai-enhancement-result-item">
                      수식(LaTeX): <span className="pdf-ai-enhancement-result-badge">{aiEnhancementResult.formulasFound || 0}개</span>
                    </span>
                  </div>
                </div>
              )}

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
