import api from './api';
import { LoginRequest, RegisterRequest, TokenResponse, User } from '../types';

/**
 * 로그인
 */
export const login = async (data: LoginRequest): Promise<TokenResponse> => {
  const response = await api.post<TokenResponse>('/auth/login', data);
  return response.data;
};

/**
 * 회원가입
 */
export const register = async (data: RegisterRequest): Promise<User> => {
  const response = await api.post<User>('/auth/register', data);
  return response.data;
};

/**
 * 토큰 갱신
 */
export const refreshToken = async (refreshToken: string): Promise<TokenResponse> => {
  const response = await api.post<TokenResponse>('/auth/refresh', { refreshToken });
  return response.data;
};

/**
 * 로그아웃
 */
export const logout = async (): Promise<void> => {
  await api.post('/auth/logout');
};

/**
 * 현재 사용자 정보 조회
 */
export const getCurrentUser = async (): Promise<User> => {
  const response = await api.get<User>('/users/me');
  return response.data;
};
