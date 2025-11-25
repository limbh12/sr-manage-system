/**
 * 사용자 역할
 */
export type Role = 'ADMIN' | 'USER';

/**
 * SR 상태
 */
export type SrStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';

/**
 * SR 우선순위
 */
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

/**
 * 사용자 정보
 */
export interface User {
  id: number;
  username: string;
  email: string;
  role: Role;
  createdAt: string;
}

/**
 * SR 정보
 */
export interface Sr {
  id: number;
  title: string;
  description: string;
  status: SrStatus;
  priority: Priority;
  requester: User;
  assignee: User | null;
  createdAt: string;
  updatedAt: string;
}

/**
 * 토큰 응답
 */
export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

/**
 * 로그인 요청
 */
export interface LoginRequest {
  username: string;
  password: string;
}

/**
 * 회원가입 요청
 */
export interface RegisterRequest {
  username: string;
  password: string;
  email: string;
}

/**
 * SR 생성 요청
 */
export interface SrCreateRequest {
  title: string;
  description?: string;
  priority?: Priority;
  assigneeId?: number;
}

/**
 * SR 수정 요청
 */
export interface SrUpdateRequest {
  title?: string;
  description?: string;
  status?: SrStatus;
  priority?: Priority;
  assigneeId?: number;
}

/**
 * SR 상태 변경 요청
 */
export interface SrStatusUpdateRequest {
  status: SrStatus;
}

/**
 * 페이지네이션 응답
 */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

/**
 * API 에러 응답
 */
export interface ApiError {
  error: string;
  message: string;
  timestamp: string;
  path?: string;
}

/**
 * 인증 상태
 */
export interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  loading: boolean;
  error: string | null;
}

/**
 * SR 상태
 */
export interface SrState {
  srList: Sr[];
  currentSr: Sr | null;
  totalElements: number;
  totalPages: number;
  currentPage: number;
  loading: boolean;
  error: string | null;
}
