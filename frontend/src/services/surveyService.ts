import api from './api';
import { OpenApiSurvey, OpenApiSurveyCreateRequest, PageResponse, OpenApiSurveySearch } from '../types';
import * as mockSurveyService from './mock/surveyServiceMock';
import { USE_MOCK } from '../config';

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
  const response = await api.put<OpenApiSurveyResponse>(`/surveys/${id}`, data);
  return response.data;
};

/**
 * 설문 삭제
 */
export const deleteSurvey = async (id: number): Promise<void> => {
  if (USE_MOCK) return;
  await api.delete(`/surveys/${id}`);
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
    // RFC-compliant-ish parsing: prefer filename* then filename, strip quotes/semicolons and control chars
    // filename* example: filename*=UTF-8''%e2%82%ac%20rates.pdf
    const filenameStarMatch = contentDisposition.match(/filename\*=(?:UTF-8'')?([^;\n\r]+)/i);
    const filenameMatch = contentDisposition.match(/filename=(?:")?([^";\n\r]+)(?:")?/i);
    try {
      if (filenameStarMatch && filenameStarMatch[1]) {
        // decode percent-encoding
        fileName = decodeURIComponent(filenameStarMatch[1].replace(/^["']|["']$/g, '').trim());
      } else if (filenameMatch && filenameMatch[1]) {
        fileName = filenameMatch[1].replace(/^["']|["']$/g, '').trim();
      }
    } catch (e) {
      // fallback
      fileName = (filenameMatch && filenameMatch[1]) ? filenameMatch[1].replace(/^["']|["']$/g, '').trim() : fileName;
    }

    // sanitize: remove control chars that may be turned into underscores by the filesystem/browser
    fileName = fileName.replace(/[\x00-\x1F\r\n\t]/g, '').trim();
  }
  
  link.setAttribute('download', fileName);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
};

/**
 * 단일 설문 수신파일 업로드 (named export for existing imports)
 */
export const uploadReceivedFile = async (id: number, file: File): Promise<any> => {
  if (USE_MOCK && (mockSurveyService as any).uploadReceivedFile) return (mockSurveyService as any).uploadReceivedFile(id, file);
  const formData = new FormData();
  formData.append('file', file);
  const response = await api.post(`/surveys/${id}/file`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return response.data;
};

/**
 * 기관 검색 (행정표준코드)
 */
export const searchOrganizations = async (keyword: string): Promise<{ code: string; name: string }[]> => {
  if (USE_MOCK) return mockSurveyService.searchOrganizations(keyword);
  const response = await api.get<{ code: string; name: string }[]>('/organizations', { params: { keyword } });
  return response.data;
};

export interface OpenApiSurveyResponse extends OpenApiSurvey {}

export interface BulkUploadResult {
  totalCount: number;
  successCount: number;
  failureCount: number;
  failures: {
    rowNumber: number;
    reason: string;
    data: string;
  }[];
}

export const surveyService = {
  getSurveys: async (page: number = 0, size: number = 10, search?: OpenApiSurveySearch) => {
    if (USE_MOCK) return mockSurveyService.getSurveyList(page, size, search);
    const response = await api.get<PageResponse<OpenApiSurvey>>('/surveys', { params: { page, size, ...search } });
    return response.data;
  },

  getSurveyById: async (id: number) => {
    if (USE_MOCK) return mockSurveyService.getSurveyById(id);
    const response = await api.get<OpenApiSurvey>(`/surveys/${id}`);
    return response.data;
  },

  createSurvey: async (data: OpenApiSurveyCreateRequest) => {
    if (USE_MOCK) return mockSurveyService.createSurvey(data);
    const response = await api.post<OpenApiSurvey>('/surveys', data);
    return response.data;
  },

  updateSurvey: async (id: number, data: OpenApiSurveyCreateRequest) => {
    const response = await api.put<OpenApiSurveyResponse>(`/surveys/${id}`, data);
    return response.data;
  },

  uploadSurveyCsv: async (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post<BulkUploadResult>('/surveys/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },
  // 단일 설문의 수신파일 업로드
  uploadReceivedFile: async (id: number, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post(`/surveys/${id}/file`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },
};
