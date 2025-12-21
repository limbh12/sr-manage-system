/**
 * 간단한 토스트 메시지 유틸리티
 * 화면 상단에 일시적인 알림 메시지를 표시합니다.
 */

export type ToastType = 'success' | 'error' | 'warning' | 'info';

interface ToastOptions {
  message: string;
  type?: ToastType;
  duration?: number; // milliseconds
}

const TOAST_CONTAINER_ID = 'toast-container';

/**
 * 토스트 컨테이너가 없으면 생성
 */
function getOrCreateContainer(): HTMLElement {
  let container = document.getElementById(TOAST_CONTAINER_ID);
  if (!container) {
    container = document.createElement('div');
    container.id = TOAST_CONTAINER_ID;
    container.style.cssText = `
      position: fixed;
      top: 20px;
      left: 50%;
      transform: translateX(-50%);
      z-index: 10000;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 10px;
      pointer-events: none;
    `;
    document.body.appendChild(container);
  }
  return container;
}

/**
 * 토스트 타입에 따른 스타일 반환
 */
function getTypeStyles(type: ToastType): string {
  const baseStyles = `
    padding: 12px 24px;
    border-radius: 8px;
    font-size: 14px;
    font-weight: 500;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    pointer-events: auto;
    animation: slideDown 0.3s ease-out;
  `;

  const typeStyles: Record<ToastType, string> = {
    success: 'background: #10b981; color: white;',
    error: 'background: #ef4444; color: white;',
    warning: 'background: #f59e0b; color: white;',
    info: 'background: #3b82f6; color: white;',
  };

  return baseStyles + typeStyles[type];
}

/**
 * 애니메이션 스타일 추가 (한 번만)
 */
function ensureAnimationStyles(): void {
  if (document.getElementById('toast-animation-styles')) return;

  const style = document.createElement('style');
  style.id = 'toast-animation-styles';
  style.textContent = `
    @keyframes slideDown {
      from {
        opacity: 0;
        transform: translateY(-20px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }
    @keyframes slideUp {
      from {
        opacity: 1;
        transform: translateY(0);
      }
      to {
        opacity: 0;
        transform: translateY(-20px);
      }
    }
  `;
  document.head.appendChild(style);
}

/**
 * 토스트 메시지 표시
 */
export function showToast(options: ToastOptions): void {
  const { message, type = 'info', duration = 3000 } = options;

  ensureAnimationStyles();
  const container = getOrCreateContainer();

  const toast = document.createElement('div');
  toast.style.cssText = getTypeStyles(type);
  toast.textContent = message;

  container.appendChild(toast);

  // 자동 제거
  setTimeout(() => {
    toast.style.animation = 'slideUp 0.3s ease-out forwards';
    setTimeout(() => {
      toast.remove();
    }, 300);
  }, duration);
}

// 편의 함수들
export const toast = {
  success: (message: string, duration?: number) =>
    showToast({ message, type: 'success', duration }),
  error: (message: string, duration?: number) =>
    showToast({ message, type: 'error', duration }),
  warning: (message: string, duration?: number) =>
    showToast({ message, type: 'warning', duration }),
  info: (message: string, duration?: number) =>
    showToast({ message, type: 'info', duration }),
};

export default toast;
