import api from './api';
import { PageResponse, User, UserUpdateRequest, UserCreateRequest } from '../types';
import * as mockUserService from './mock/userServiceMock';

// 백엔드 서버가 실행되지 않을 때를 대비해 true로 설정 (실제 운영 시 환경변수로 제어)
const USE_MOCK = true; // import.meta.env.VITE_USE_MOCK === 'true';

/**
 * 사용자 목록 조회 (관리자 전용)
 * @param page 페이지 번호 (0부터 시작)
 * @param size 페이지 크기
 */
export const getUsers = async (page = 0, size = 10): Promise<PageResponse<User>> => {
  if (USE_MOCK) return mockUserService.getUsers(page, size);
  const response = await api.get<PageResponse<User>>('/users', {
    params: { page, size },
  });
  return response.data;
};

/**
 * 사용자 생성 (관리자 전용)
 * @param data 생성할 사용자 정보
 */
export const createUser = async (data: UserCreateRequest): Promise<User> => {
  if (USE_MOCK) return mockUserService.createUser(data);
  const response = await api.post<User>('/users', data);
  return response.data;
};

/**
 * 사용자 정보 수정 (관리자 전용)
 * @param id 사용자 ID
 * @param data 수정할 사용자 정보
 */
export const updateUser = async (id: number, data: UserUpdateRequest): Promise<User> => {
  if (USE_MOCK) return mockUserService.updateUser(id, data);
  const response = await api.put<User>(`/users/${id}`, data);
  return response.data;
};

/**
 * 사용자 삭제 (관리자 전용)
 * @param id 사용자 ID
 */
export const deleteUser = async (id: number): Promise<void> => {
  if (USE_MOCK) return mockUserService.deleteUser(id);
  await api.delete(`/users/${id}`);
};

/**
 * 내 정보 수정
 * @param data 수정할 정보
 */
export const updateMyProfile = async (data: UserUpdateRequest): Promise<User> => {
  if (USE_MOCK) return mockUserService.updateMyProfile(data);
  const response = await api.put<User>('/users/me', data);
  return response.data;
};

/**
 * 사용자 목록 조회 (옵션용)
 */
export const getUserOptions = async (): Promise<User[]> => {
  if (USE_MOCK) return mockUserService.getUserOptions();
  const response = await api.get<User[]>('/users/options');
  return response.data;
};
