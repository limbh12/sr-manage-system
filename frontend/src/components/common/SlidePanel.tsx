import React, { useEffect } from 'react';
import './SlidePanel.css';

interface SlidePanelProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  children: React.ReactNode;
  width?: string;
}

const SlidePanel: React.FC<SlidePanelProps> = ({
  isOpen,
  onClose,
  title,
  children,
  width = '50%'
}) => {
  // ESC 키로 패널 닫기
  useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };

    document.addEventListener('keydown', handleEscape);
    return () => document.removeEventListener('keydown', handleEscape);
  }, [isOpen, onClose]);

  // body 스크롤 방지
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
    }

    return () => {
      document.body.style.overflow = '';
    };
  }, [isOpen]);

  if (!isOpen) return null;

  return (
    <>
      {/* 배경 오버레이 */}
      <div className="slide-panel-overlay" onClick={onClose} />

      {/* 슬라이드 패널 */}
      <div className="slide-panel" style={{ width }}>
        {/* 헤더 */}
        <div className="slide-panel-header">
          <h2>{title}</h2>
          <button className="slide-panel-close" onClick={onClose} title="닫기 (ESC)">
            ×
          </button>
        </div>

        {/* 내용 */}
        <div className="slide-panel-content">
          {children}
        </div>
      </div>
    </>
  );
};

export default SlidePanel;
