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
  name: string;
  email: string;
  role: Role;
  createdAt: string;
}

/**
 * SR 정보
 */
export interface Sr {
  id: number;
  srId: string;
  title: string;
  description: string;
  processingDetails?: string;
  status: SrStatus;
  priority: Priority;
  requester: User;
  assignee: User | null;
  createdAt: string;
  updatedAt: string;
  openApiSurveyId?: number;
  applicantName?: string;
  applicantPhone?: string;
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
  name: string;
  password: string;
  email: string;
}

/**
 * 사용자 생성 요청 (관리자용)
 */
export interface UserCreateRequest extends RegisterRequest {
  role: Role;
}

/**
 * SR 생성 요청
 */
export interface SrCreateRequest {
  title: string;
  description?: string;
  priority?: Priority;
  assigneeId?: number;
  openApiSurveyId?: number;
  applicantName?: string;
  applicantPhone?: string;
}

/**
 * SR 수정 요청
 */
export interface SrUpdateRequest {
  title?: string;
  description?: string;
  processingDetails?: string;
  status?: SrStatus;
  priority?: Priority;
  assigneeId?: number;
  openApiSurveyId?: number;
  applicantName?: string;
  applicantPhone?: string;
}

/**
 * SR 상태 변경 요청
 */
export interface SrStatusUpdateRequest {
  status: SrStatus;
}

/**
 * SR 이력 유형
 */
export type SrHistoryType = 'COMMENT' | 'STATUS_CHANGE' | 'PRIORITY_CHANGE' | 'ASSIGNEE_CHANGE' | 'INFO_CHANGE';

/**
 * SR 이력 정보
 */
export interface SrHistory {
  id: number;
  srId: number;
  content: string;
  historyType: SrHistoryType;
  previousValue?: string;
  newValue?: string;
  createdBy: User;
  createdAt: string;
}

/**
 * SR 이력 생성 요청
 */
export interface SrHistoryCreateRequest {
  content: string;
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
  srHistories: SrHistory[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  loading: boolean;
  error: string | null;
}

/**
 * 전환 방식
 */
export type TransitionMethod = 'CENTRAL' | 'DISTRIBUTED';
export type DesiredTransitionMethod = 'CENTRAL_IMPROVED' | 'DISTRIBUTED_IMPROVED';

/**
 * 유지보수 운영인력
 */
export type MaintenanceOperation = 'INTERNAL' | 'PROFESSIONAL_RESIDENT' | 'PROFESSIONAL_NON_RESIDENT' | 'OTHER';

/**
 * 유지보수 수행장소
 */
export type MaintenanceLocation = 'INTERNAL' | 'EXTERNAL' | 'REMOTE';

/**
 * 운영환경 구분
 */
export type OperationEnv = 'OPS' | 'DEV_OPS' | 'TEST_OPS' | 'DEV_TEST_OPS' | 'OTHER';

/**
 * 서버 OS 종류
 */
export type ServerOs = 'LINUX' | 'WINDOWS' | 'UNIX' | 'OTHER';

/**
 * WEB 서버 종류
 */
export type WebServerType = 'APACHE' | 'NGINX' | 'WEBTOB' | 'IIS' | 'OTHER';

/**
 * WAS 서버 종류
 */
export type WasServerType = 'JBOSS_EAP' | 'TOMCAT' | 'WILDFLY' | 'WEBLOGIC' | 'JEUS' | 'JETTY' | 'OTHER';

/**
 * DB 서버 종류
 */
export type DbServerType = 'TIBERO' | 'POSTGRESQL' | 'CUBRID' | 'MYSQL' | 'ORACLE' | 'MSSQL' | 'OTHER';

/**
 * 개발 언어
 */
export type DevLanguage = 'JAVA' | 'PHP' | 'PYTHON' | 'CSHARP' | 'OTHER';

/**
 * 개발 프레임워크
 */
export type DevFramework = 'EGOV' | 'SPRING' | 'SPRING_BOOT' | 'OTHER';

/**
 * OPEN API 현황조사 데이터
 */
export interface OpenApiSurvey {
  id: number;
  // 기본 정보
  organizationCode: string;
  organizationName: string;
  department: string;
  contactName: string;
  contactPhone: string;
  contactEmail: string;

  // 수신 파일 정보
  receivedFileName?: string;
  receivedDate: string;

  // API 시스템 현황
  systemName: string;
  currentMethod: TransitionMethod;
  desiredMethod: DesiredTransitionMethod;
  reasonForDistributed?: string; // 분산개선형 선택 시 사유

  // 유지보수
  maintenanceOperation: MaintenanceOperation;
  maintenanceLocation: MaintenanceLocation;
  maintenanceAddress?: string;
  maintenanceNote?: string;

  // 운영환경
  operationEnv: OperationEnv;
  serverLocation?: string;

  // 4. 개발 및 운영환경
  // WEB Server
  webServerOs?: ServerOs;
  webServerOsType?: string;
  webServerOsVersion?: string;
  webServerType?: WebServerType;
  webServerTypeOther?: string;
  webServerVersion?: string;

  // WAS Server
  wasServerOs?: ServerOs;
  wasServerOsType?: string;
  wasServerOsVersion?: string;
  wasServerType?: WasServerType;
  wasServerTypeOther?: string;
  wasServerVersion?: string;

  // DB Server
  dbServerOs?: ServerOs;
  dbServerOsType?: string;
  dbServerOsVersion?: string;
  dbServerType?: DbServerType;
  dbServerTypeOther?: string;
  dbServerVersion?: string;

  // 개발 및 운영환경
  devLanguage?: DevLanguage;
  devLanguageOther?: string;
  devLanguageVersion?: string;
  devFramework?: DevFramework;
  devFrameworkOther?: string;
  devFrameworkVersion?: string;

  // 기타
  otherRequests?: string;
  note?: string;

  createdAt: string;
  updatedAt: string;
}

/**
 * OPEN API 현황조사 생성 요청
 */
export type OpenApiSurveyCreateRequest = Omit<OpenApiSurvey, 'id' | 'createdAt' | 'updatedAt' | 'organizationName'>;

/**
 * OPEN API 현황조사 검색 조건
 */
export interface OpenApiSurveySearch {
  keyword?: string;
  currentMethod?: TransitionMethod | '';
  desiredMethod?: DesiredTransitionMethod | '';
}

/**
 * 사용자 정보 수정 요청
 */
export interface UserUpdateRequest {
  name?: string;
  email?: string;
  role?: Role;
}
