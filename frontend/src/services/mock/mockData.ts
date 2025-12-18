import { Sr, User, OpenApiSurvey } from '../../types';

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

// 초기 OPEN API 현황조사 데이터
export const INITIAL_SURVEY_LIST: OpenApiSurvey[] = [
  {
    id: 1,
    organization: { code: 'MOIS', name: '행정안전부' },
    department: '디지털정부국',
    contactName: '홍길동',
    contactPhone: '010-1234-5678',
    contactEmail: 'hong@example.com',
    assignee: null,
    status: 'IN_PROGRESS',
    receivedDate: new Date().toISOString(),
    systemName: '행정정보공동이용시스템',
    operationStatus: 'OPERATING',
    currentMethod: 'CENTRAL',
    desiredMethod: 'CENTRAL_IMPROVED',
    maintenanceOperation: 'INTERNAL',
    maintenanceLocation: 'INTERNAL',
    operationEnv: 'OPS',
    createdAt: new Date(Date.now() - 86400000 * 5).toISOString(),
    updatedAt: new Date(Date.now() - 86400000 * 5).toISOString(),
  },
  {
    id: 2,
    organization: { code: 'SEOUL', name: '서울특별시' },
    department: '정보통신과',
    contactName: '김철수',
    contactPhone: '010-9876-5432',
    contactEmail: 'kim@example.com',
    assignee: null,
    status: 'PENDING',
    receivedDate: new Date().toISOString(),
    systemName: '서울시 열린데이터 광장',
    operationStatus: 'OPERATING',
    currentMethod: 'DISTRIBUTED',
    desiredMethod: 'DISTRIBUTED_IMPROVED',
    maintenanceOperation: 'PROFESSIONAL_RESIDENT',
    maintenanceLocation: 'EXTERNAL',
    operationEnv: 'OPS',
    createdAt: new Date(Date.now() - 86400000 * 3).toISOString(),
    updatedAt: new Date(Date.now() - 86400000 * 3).toISOString(),
  },
  {
    id: 3,
    organization: { code: 'BUSAN', name: '부산광역시' },
    department: '빅데이터통계과',
    contactName: '이영희',
    contactPhone: '010-1111-2222',
    contactEmail: 'lee@example.com',
    assignee: null,
    status: 'COMPLETED',
    receivedDate: new Date().toISOString(),
    systemName: '부산시 공공데이터 포털',
    operationStatus: 'OPERATING',
    currentMethod: 'DISTRIBUTED',
    desiredMethod: 'CENTRAL_IMPROVED',
    maintenanceOperation: 'PROFESSIONAL_NON_RESIDENT',
    maintenanceLocation: 'REMOTE',
    operationEnv: 'OPS',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  }
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
