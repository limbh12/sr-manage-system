import React, { useState, useEffect } from 'react';
import { Document, Page, pdfjs } from 'react-pdf';
import 'react-pdf/dist/Page/AnnotationLayer.css';
import 'react-pdf/dist/Page/TextLayer.css';

// PDF.js worker 설정 - vite-plugin-static-copy 사용
pdfjs.GlobalWorkerOptions.workerSrc = new URL(
  'pdfjs-dist/build/pdf.worker.min.mjs',
  import.meta.url
).toString();

interface PdfViewerProps {
  fileId: number;
  fileName: string;
}

const PdfViewer: React.FC<PdfViewerProps> = ({ fileId, fileName }) => {
  const [numPages, setNumPages] = useState<number>(0);
  const [currentPage, setCurrentPage] = useState<number>(1);
  const [scale, setScale] = useState<number>(1.0);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [pdfData, setPdfData] = useState<string | null>(null);

  const pdfUrl = `/api/wiki/files/${fileId}`;

  // JWT 토큰으로 PDF를 Blob으로 fetch
  useEffect(() => {
    const fetchPdf = async () => {
      try {
        console.log('PDF fetch 시작:', pdfUrl);
        const token = localStorage.getItem('accessToken');
        console.log('토큰 존재:', !!token);

        const response = await fetch(pdfUrl, {
          headers: {
            Authorization: token ? `Bearer ${token}` : '',
          },
        });

        console.log('응답 상태:', response.status, response.statusText);
        console.log('응답 헤더:', {
          contentType: response.headers.get('content-type'),
          contentLength: response.headers.get('content-length'),
        });

        if (!response.ok) {
          const errorText = await response.text();
          console.error('응답 에러 내용:', errorText);
          throw new Error(`PDF 로드 실패: ${response.status} ${response.statusText}`);
        }

        const blob = await response.blob();
        console.log('Blob 생성 완료:', blob.size, 'bytes, type:', blob.type);

        const url = URL.createObjectURL(blob);
        console.log('Object URL 생성:', url);
        setPdfData(url);
      } catch (err) {
        console.error('PDF fetch 실패:', err);
        setError(`PDF 파일을 불러올 수 없습니다: ${err instanceof Error ? err.message : '알 수 없는 오류'}`);
        setIsLoading(false);
      }
    };

    fetchPdf();

    // cleanup
    return () => {
      if (pdfData) {
        console.log('Object URL 해제:', pdfData);
        URL.revokeObjectURL(pdfData);
      }
    };
  }, [fileId]);

  const onDocumentLoadSuccess = ({ numPages }: { numPages: number }) => {
    setNumPages(numPages);
    setIsLoading(false);
    setError(null);
  };

  const onDocumentLoadError = (error: Error) => {
    console.error('PDF Document 로드 실패 상세:', {
      message: error.message,
      name: error.name,
      stack: error.stack,
      error: error
    });
    setError(`PDF 파일을 불러올 수 없습니다: ${error.message}`);
    setIsLoading(false);
  };

  const goToPrevPage = () => {
    setCurrentPage((prev) => Math.max(prev - 1, 1));
  };

  const goToNextPage = () => {
    setCurrentPage((prev) => Math.min(prev + 1, numPages));
  };

  const zoomIn = () => {
    setScale((prev) => Math.min(prev + 0.2, 3.0));
  };

  const zoomOut = () => {
    setScale((prev) => Math.max(prev - 0.2, 0.5));
  };

  const resetZoom = () => {
    setScale(1.0);
  };

  const downloadPdf = () => {
    window.open(pdfUrl, '_blank');
  };

  return (
    <div className="pdf-viewer-container" style={styles.container}>
      {/* 헤더 */}
      <div style={styles.header}>
        <div style={styles.headerLeft}>
          <h3 style={styles.title}>📄 {fileName}</h3>
        </div>
        <div style={styles.headerRight}>
          <button onClick={downloadPdf} style={styles.downloadButton}>
            다운로드
          </button>
        </div>
      </div>

      {/* 툴바 */}
      <div style={styles.toolbar}>
        <div style={styles.toolbarLeft}>
          <button
            onClick={goToPrevPage}
            disabled={currentPage <= 1}
            style={{
              ...styles.toolbarButton,
              ...(currentPage <= 1 ? styles.toolbarButtonDisabled : {}),
            }}
          >
            ← 이전
          </button>
          <span style={styles.pageInfo}>
            {currentPage} / {numPages}
          </span>
          <button
            onClick={goToNextPage}
            disabled={currentPage >= numPages}
            style={{
              ...styles.toolbarButton,
              ...(currentPage >= numPages ? styles.toolbarButtonDisabled : {}),
            }}
          >
            다음 →
          </button>
        </div>

        <div style={styles.toolbarRight}>
          <button onClick={zoomOut} style={styles.toolbarButton}>
            -
          </button>
          <span style={styles.zoomInfo}>{Math.round(scale * 100)}%</span>
          <button onClick={zoomIn} style={styles.toolbarButton}>
            +
          </button>
          <button onClick={resetZoom} style={styles.toolbarButton}>
            초기화
          </button>
        </div>
      </div>

      {/* PDF 문서 */}
      <div style={styles.documentContainer}>
        {isLoading && <div style={styles.loading}>PDF 로딩 중...</div>}
        {error && <div style={styles.error}>{error}</div>}

        {pdfData && (
          <Document
            file={pdfData}
            onLoadSuccess={onDocumentLoadSuccess}
            onLoadError={onDocumentLoadError}
            loading={<div style={styles.loading}>PDF 로딩 중...</div>}
            error={<div style={styles.error}>PDF 파일을 불러올 수 없습니다.</div>}
          >
            <Page
              pageNumber={currentPage}
              scale={scale}
              renderTextLayer={true}
              renderAnnotationLayer={true}
              loading={<div style={styles.loading}>페이지 로딩 중...</div>}
            />
          </Document>
        )}
      </div>
    </div>
  );
};

const styles: { [key: string]: React.CSSProperties } = {
  container: {
    border: '1px solid var(--border-color, #e0e0e0)',
    borderRadius: '8px',
    backgroundColor: 'var(--bg-primary, #ffffff)',
    overflow: 'hidden',
    marginBottom: '20px',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '16px 20px',
    borderBottom: '1px solid var(--border-color, #e0e0e0)',
    backgroundColor: 'var(--bg-secondary, #f8f9fa)',
  },
  headerLeft: {
    flex: 1,
  },
  headerRight: {
    display: 'flex',
    gap: '8px',
  },
  title: {
    margin: 0,
    fontSize: '16px',
    fontWeight: 600,
    color: 'var(--text-primary, #212529)',
  },
  downloadButton: {
    padding: '8px 16px',
    backgroundColor: '#007bff',
    color: '#ffffff',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
    fontSize: '14px',
    fontWeight: 500,
  },
  toolbar: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '12px 20px',
    borderBottom: '1px solid var(--border-color, #e0e0e0)',
    backgroundColor: 'var(--bg-secondary, #f8f9fa)',
  },
  toolbarLeft: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
  },
  toolbarRight: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
  },
  toolbarButton: {
    padding: '6px 12px',
    backgroundColor: '#ffffff',
    border: '1px solid var(--border-color, #dee2e6)',
    borderRadius: '4px',
    cursor: 'pointer',
    fontSize: '14px',
    color: 'var(--text-primary, #212529)',
  },
  toolbarButtonDisabled: {
    opacity: 0.5,
    cursor: 'not-allowed',
  },
  pageInfo: {
    fontSize: '14px',
    fontWeight: 500,
    color: 'var(--text-primary, #212529)',
    minWidth: '80px',
    textAlign: 'center',
  },
  zoomInfo: {
    fontSize: '14px',
    fontWeight: 500,
    color: 'var(--text-primary, #212529)',
    minWidth: '60px',
    textAlign: 'center',
  },
  documentContainer: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'flex-start',
    padding: '20px',
    backgroundColor: 'var(--bg-tertiary, #f1f3f5)',
    minHeight: '600px',
    maxHeight: '800px',
    overflowY: 'auto',
  },
  loading: {
    padding: '40px',
    textAlign: 'center',
    fontSize: '16px',
    color: 'var(--text-secondary, #6c757d)',
  },
  error: {
    padding: '40px',
    textAlign: 'center',
    fontSize: '16px',
    color: '#dc3545',
  },
};

export default PdfViewer;
