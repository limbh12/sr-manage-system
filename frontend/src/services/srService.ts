import api from './api';
import { Sr, SrCreateRequest, SrUpdateRequest, SrStatusUpdateRequest, PageResponse, SrStatus, Priority, SrHistory, SrHistoryCreateRequest } from '../types';
import * as mockSrService from './mock/srServiceMock';

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true';

interface GetSrListParams {
  page?: number;
  size?: number;
  status?: SrStatus;
  priority?: Priority;
  search?: string;
}

/**
 * SR 목록 조회
 */
export const getSrList = async (params: GetSrListParams = {}): Promise<PageResponse<Sr>> => {
  if (USE_MOCK) return mockSrService.getSrList(params);
  const response = await api.get<PageResponse<Sr>>('/sr', { params });
  return response.data;
};

/**
 * SR 상세 조회
 */
export const getSrById = async (id: number): Promise<Sr> => {
  if (USE_MOCK) return mockSrService.getSrById(id);
  const response = await api.get<Sr>(`/sr/${id}`);
  return response.data;
};

/**
 * SR 생성
 */
export const createSr = async (data: SrCreateRequest): Promise<Sr> => {
  if (USE_MOCK) return mockSrService.createSr(data);
  const response = await api.post<Sr>('/sr', data);
  return response.data;
};

/**
 * SR 수정
 */
export const updateSr = async (id: number, data: SrUpdateRequest): Promise<Sr> => {
  if (USE_MOCK) return mockSrService.updateSr(id, data);
  const response = await api.put<Sr>(`/sr/${id}`, data);
  return response.data;
};

/**
 * SR 삭제
 */
export const deleteSr = async (id: number): Promise<void> => {
  if (USE_MOCK) return mockSrService.deleteSr(id);
  await api.delete(`/sr/${id}`);
};

/**
 * SR 상태 변경
 */
export const updateSrStatus = async (id: number, data: SrStatusUpdateRequest): Promise<Sr> => {
  if (USE_MOCK) return mockSrService.updateSrStatus(id, data);
  const response = await api.patch<Sr>(`/sr/${id}/status`, data);
  return response.data;
};

/**
 * SR 이력 목록 조회
 */
export const getSrHistories = async (id: number): Promise<SrHistory[]> => {
  if (USE_MOCK) return mockSrService.getSrHistories(id);
  const response = await api.get<SrHistory[]>(`/sr/${id}/histories`);
  return response.data;
};

/**
 * SR 이력(댓글) 생성
 */
export const createSrHistory = async (id: number, data: SrHistoryCreateRequest): Promise<SrHistory> => {
  if (USE_MOCK) return mockSrService.createSrHistory(id, data);
  const response = await api.post<SrHistory>(`/sr/${id}/histories`, data);
  return response.data;
};
