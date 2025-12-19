import React from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeHighlight from 'rehype-highlight';
import rehypeRaw from 'rehype-raw';
import rehypeSlug from 'rehype-slug';
import 'highlight.js/styles/github.css';
import './WikiViewer.css';
import type { WikiFile } from '../../types/wiki';
import PdfViewer from './PdfViewer';

interface WikiViewerProps {
  content: string;
  files?: WikiFile[];
}

const WikiViewer: React.FC<WikiViewerProps> = ({ content, files }) => {
  // PDF 파일 찾기 (원본 PDF)
  const pdfFile = files?.find(file =>
    file.type === 'DOCUMENT' &&
    (file.originalFileName.toLowerCase().endsWith('.pdf') ||
     file.fileType === 'application/pdf')
  );

  return (
    <div className="wiki-viewer markdown-body">
      {/* PDF 원본 뷰어 */}
      {pdfFile && (
        <PdfViewer
          fileId={pdfFile.id}
          fileName={pdfFile.originalFileName}
        />
      )}

      {/* 마크다운 콘텐츠 */}
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        rehypePlugins={[rehypeSlug, rehypeHighlight, rehypeRaw]}
        components={{
          // 링크 처리: 앵커 링크는 같은 페이지, 외부 링크는 새 탭
          a: ({ href, children }) => {
            // 앵커 링크 (#로 시작) 또는 상대 경로는 같은 페이지에서 열기
            if (href?.startsWith('#') || href?.startsWith('/')) {
              return <a href={href}>{children}</a>;
            }
            // 외부 링크는 새 탭에서 열기
            return (
              <a href={href} target="_blank" rel="noopener noreferrer">
                {children}
              </a>
            );
          },
        }}
      >
        {content}
      </ReactMarkdown>
    </div>
  );
};

export default WikiViewer;
