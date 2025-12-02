/**
 * 전역 설정 파일
 */

// Mock 데이터 사용 여부
// .env 파일에 VITE_USE_MOCK=true 로 설정되어 있으면 Mock 서비스를 사용합니다.
// 설정이 없으면 기본값은 false (실제 백엔드 API 사용) 입니다.
export const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true';
