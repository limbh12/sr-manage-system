import api from './api';
import { OpenApiSurvey, OpenApiSurveyCreateRequest, PageResponse, OpenApiSurveySearch } from '../types';
import * as mockSurveyService from './mock/surveyServiceMock';

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true';

/**
 * 설문 목록 조회
 */
export const getSurveyList = async (page = 0, size = 10, search?: OpenApiSurveySearch): Promise<PageResponse<OpenApiSurvey>> => {
  if (USE_MOCK) return mockSurveyService.getSurveyList(page, size, search);
  const response = await api.get<PageResponse<OpenApiSurvey>>('/surveys', { params: { page, size, ...search } });
  return response.data;
};

/**
 * 설문 상세 조회
 */
export const getSurveyById = async (id: number): Promise<OpenApiSurvey> => {
  if (USE_MOCK) return mockSurveyService.getSurveyById(id);
  const response = await api.get<OpenApiSurvey>(`/surveys/${id}`);
  return response.data;
};

/**
 * 설문 생성
 */
export const createSurvey = async (data: OpenApiSurveyCreateRequest): Promise<OpenApiSurvey> => {
  if (USE_MOCK) return mockSurveyService.createSurvey(data);
  const response = await api.post<OpenApiSurvey>('/surveys', data);
  return response.data;
};

/**
 * 설문 수정
 */
export const updateSurvey = async (id: number, data: OpenApiSurveyCreateRequest): Promise<OpenApiSurvey> => {
  if (USE_MOCK) return mockSurveyService.updateSurvey(id, data);
  const response = await api.put<OpenApiSurvey>(`/surveys/${id}`, data);
  return response.data;
};

/**
 * 설문 파일 다운로드
 */
export const downloadSurveyFile = async (id: number): Promise<void> => {
  if (USE_MOCK) return mockSurveyService.downloadSurveyFile(id);
  const response = await api.get(`/surveys/${id}/download`, { responseType: 'blob' });
  
  // 파일 다운로드 처리
  const url = window.URL.createObjectURL(new Blob([response.data]));
  const link = document.createElement('a');
  link.href = url;
  
  // Content-Disposition 헤더에서 파일명 추출 시도 (옵션)
  const contentDisposition = response.headers['content-disposition'];
  let fileName = 'downloaded_file';
  if (contentDisposition) {
    const fileNameMatch = contentDisposition.match(/filename="?(.+)"?/);
    if (fileNameMatch && fileNameMatch.length === 2)
      fileName = fileNameMatch[1];
  }
  
  link.setAttribute('download', fileName);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
};

/**
 * 기관 검색 (행정표준코드)
 */
export const searchOrganizations = async (keyword: string): Promise<{ code: string; name: string }[]> => {
  if (USE_MOCK) return mockSurveyService.searchOrganizations(keyword);
  const response = await api.get<{ code: string; name: string }[]>('/organizations', { params: { keyword } });
  return response.data;
};
