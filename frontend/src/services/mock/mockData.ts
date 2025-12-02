import { Sr, User } from '../../types';

// Mock 사용자
export const MOCK_USER: User = {
  id: 1,
  username: 'tester',
  name: '테스터',
  email: 'tester@example.com',
  role: 'USER',
  createdAt: new Date().toISOString(),
};

export const MOCK_ADMIN: User = {
  id: 2,
  username: 'admin',
  name: '관리자',
  email: 'admin@example.com',
  role: 'ADMIN',
  createdAt: new Date().toISOString(),
};

// 초기 SR 데이터
export const INITIAL_SR_LIST: Sr[] = [
  {
    id: 1,
    srId: 'SR-2512-0001',
    title: '로그인 페이지 오류 수정 요청',
    description: '로그인 시 간헐적으로 500 에러가 발생합니다.',
    status: 'OPEN',
    priority: 'HIGH',
    requester: MOCK_USER,
    assignee: null,
    createdAt: new Date(Date.now() - 86400000 * 2).toISOString(), // 2일 전
    updatedAt: new Date(Date.now() - 86400000 * 2).toISOString(),
  },
  {
    id: 2,
    srId: 'SR-2512-0002',
    title: '대시보드 디자인 개선',
    description: '대시보드 UI를 좀 더 현대적으로 변경해주세요.',
    status: 'IN_PROGRESS',
    priority: 'MEDIUM',
    requester: MOCK_USER,
    assignee: MOCK_ADMIN,
    createdAt: new Date(Date.now() - 86400000).toISOString(), // 1일 전
    updatedAt: new Date(Date.now() - 43200000).toISOString(),
  },
  {
    id: 3,
    srId: 'SR-2512-0003',
    title: '서버 증설 요청',
    description: '트래픽 증가로 인한 서버 증설이 필요합니다.',
    status: 'OPEN',
    priority: 'CRITICAL',
    requester: MOCK_USER,
    assignee: null,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  },
];

// 로컬 스토리지 키
export const STORAGE_KEYS = {
  SR_LIST: 'mock_sr_list',
  SR_HISTORIES: 'mock_sr_histories',
  USERS: 'mock_users',
  CURRENT_USER: 'mock_current_user',
};

// 지연 시간 시뮬레이션 (ms)
export const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));
