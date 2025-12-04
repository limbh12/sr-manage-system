import api from './api';
import { LoginRequest, TokenResponse, User } from '../types';
import * as mockAuthService from './mock/authServiceMock';
import { USE_MOCK } from '../config';

/**
 * 로그인
 */
export const login = async (data: LoginRequest): Promise<TokenResponse> => {
  if (USE_MOCK) return mockAuthService.login(data);
  const response = await api.post<TokenResponse>('/auth/login', data);
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
