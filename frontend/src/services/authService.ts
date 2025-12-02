import api from './api';
import { LoginRequest, RegisterRequest, TokenResponse, User } from '../types';
import * as mockAuthService from './mock/authServiceMock';

// 백엔드 서버가 실행되지 않을 때를 대비해 true로 설정 (실제 운영 시 환경변수로 제어)
const USE_MOCK = true; // import.meta.env.VITE_USE_MOCK === 'true';

/**
 * 로그인
 */
export const login = async (data: LoginRequest): Promise<TokenResponse> => {
  if (USE_MOCK) return mockAuthService.login(data);
  const response = await api.post<TokenResponse>('/auth/login', data);
  return response.data;
};

/**
 * 회원가입
 */
export const register = async (data: RegisterRequest): Promise<User> => {
  if (USE_MOCK) return mockAuthService.register(data);
  const response = await api.post<User>('/auth/register', data);
  return response.data;
};

/**
 * 토큰 갱신
 */
export const refreshToken = async (refreshToken: string): Promise<TokenResponse> => {
  if (USE_MOCK) return mockAuthService.refreshToken(refreshToken);
  const response = await api.post<TokenResponse>('/auth/refresh', { refreshToken });
  return response.data;
};

/**
 * 로그아웃
 */
export const logout = async (): Promise<void> => {
  if (USE_MOCK) return mockAuthService.logout();
  await api.post('/auth/logout');
};

/**
 * 현재 사용자 정보 조회
 */
export const getCurrentUser = async (): Promise<User> => {
  if (USE_MOCK) return mockAuthService.getCurrentUser();
  const response = await api.get<User>('/users/me');
  return response.data;
};
