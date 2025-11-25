import api from './api';
import { Sr, SrCreateRequest, SrUpdateRequest, SrStatusUpdateRequest, PageResponse, SrStatus, Priority } from '../types';

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
  const response = await api.get<PageResponse<Sr>>('/sr', { params });
  return response.data;
};

/**
 * SR 상세 조회
 */
export const getSrById = async (id: number): Promise<Sr> => {
  const response = await api.get<Sr>(`/sr/${id}`);
  return response.data;
};

/**
 * SR 생성
 */
export const createSr = async (data: SrCreateRequest): Promise<Sr> => {
  const response = await api.post<Sr>('/sr', data);
  return response.data;
};

/**
 * SR 수정
 */
export const updateSr = async (id: number, data: SrUpdateRequest): Promise<Sr> => {
  const response = await api.put<Sr>(`/sr/${id}`, data);
  return response.data;
};

/**
 * SR 삭제
 */
export const deleteSr = async (id: number): Promise<void> => {
  await api.delete(`/sr/${id}`);
};

/**
 * SR 상태 변경
 */
export const updateSrStatus = async (id: number, data: SrStatusUpdateRequest): Promise<Sr> => {
  const response = await api.patch<Sr>(`/sr/${id}/status`, data);
  return response.data;
};
