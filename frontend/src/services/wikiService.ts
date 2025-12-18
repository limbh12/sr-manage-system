import api from './api';
import type {
  WikiDocument,
  WikiDocumentRequest,
  WikiCategory,
  WikiCategoryRequest,
  WikiVersion,
  WikiFile,
  WikiPageResponse
} from '../types/wiki';

// Wiki 문서 API
export const wikiDocumentApi = {
  // 문서 생성
  create: (data: WikiDocumentRequest) =>
    api.post<WikiDocument>('/wiki/documents', data),

  // 문서 수정
  update: (id: number, data: WikiDocumentRequest) =>
    api.put<WikiDocument>(`/wiki/documents/${id}`, data),

  // 문서 삭제
  delete: (id: number) =>
    api.delete(`/wiki/documents/${id}`),

  // 문서 조회 (조회수 증가)
  get: (id: number, incrementView: boolean = true) =>
    api.get<WikiDocument>(`/wiki/documents/${id}`, {
      params: { incrementView }
    }),

  // 문서 목록 조회
  getAll: (page: number = 0, size: number = 20) =>
    api.get<WikiPageResponse<WikiDocument>>('/wiki/documents', {
      params: { page, size }
    }),

  // 카테고리별 문서 조회
  getByCategory: (categoryId: number, page: number = 0, size: number = 20) =>
    api.get<WikiPageResponse<WikiDocument>>(`/wiki/documents/category/${categoryId}`, {
      params: { page, size }
    }),

  // SR 연계 문서 조회
  getBySr: (srId: number) =>
    api.get<WikiDocument[]>(`/wiki/documents/sr/${srId}`),

  // 문서 검색
  search: (keyword: string, page: number = 0, size: number = 20) =>
    api.get<WikiPageResponse<WikiDocument>>('/wiki/documents/search', {
      params: { keyword, page, size }
    }),

  // 최근 수정된 문서
  getRecent: (page: number = 0, size: number = 10) =>
    api.get<WikiPageResponse<WikiDocument>>('/wiki/documents/recent', {
      params: { page, size }
    }),

  // 인기 문서
  getPopular: (page: number = 0, size: number = 10) =>
    api.get<WikiPageResponse<WikiDocument>>('/wiki/documents/popular', {
      params: { page, size }
    }),
};

// Wiki 카테고리 API
export const wikiCategoryApi = {
  // 카테고리 생성
  create: (data: WikiCategoryRequest) =>
    api.post<WikiCategory>('/wiki/categories', data),

  // 카테고리 수정
  update: (id: number, data: WikiCategoryRequest) =>
    api.put<WikiCategory>(`/wiki/categories/${id}`, data),

  // 카테고리 삭제
  delete: (id: number) =>
    api.delete(`/wiki/categories/${id}`),

  // 카테고리 조회
  get: (id: number) =>
    api.get<WikiCategory>(`/wiki/categories/${id}`),

  // 전체 카테고리 조회
  getAll: () =>
    api.get<WikiCategory[]>('/wiki/categories'),

  // 최상위 카테고리 조회 (트리 구조)
  getRoot: () =>
    api.get<WikiCategory[]>('/wiki/categories/root'),

  // 하위 카테고리 조회
  getChildren: (parentId: number) =>
    api.get<WikiCategory[]>(`/wiki/categories/parent/${parentId}`),

  // 카테고리 검색
  search: (keyword: string) =>
    api.get<WikiCategory[]>('/wiki/categories/search', {
      params: { keyword }
    }),
};

// Wiki 버전 API
export const wikiVersionApi = {
  // 문서 버전 목록 조회
  getAll: (documentId: number) =>
    api.get<WikiVersion[]>(`/wiki/documents/${documentId}/versions`),

  // 문서 버전 목록 조회 (페이징)
  getAllPaged: (documentId: number, page: number = 0, size: number = 10) =>
    api.get<WikiPageResponse<WikiVersion>>(`/wiki/documents/${documentId}/versions/paged`, {
      params: { page, size }
    }),

  // 특정 버전 조회
  get: (documentId: number, version: number) =>
    api.get<WikiVersion>(`/wiki/documents/${documentId}/versions/${version}`),

  // 최신 버전 조회
  getLatest: (documentId: number) =>
    api.get<WikiVersion>(`/wiki/documents/${documentId}/versions/latest`),

  // 버전 롤백
  rollback: (documentId: number, version: number) =>
    api.post(`/wiki/documents/${documentId}/versions/${version}/rollback`),
};

// Wiki 파일 API
export const wikiFileApi = {
  // 파일 업로드
  upload: (file: File, documentId?: number) => {
    const formData = new FormData();
    formData.append('file', file);
    if (documentId) {
      formData.append('documentId', documentId.toString());
    }
    return api.post<WikiFile>('/wiki/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
  },

  // 파일 다운로드 URL 생성
  getDownloadUrl: (fileId: number) => `/api/wiki/files/${fileId}`,

  // 파일 강제 다운로드 URL 생성
  getForceDownloadUrl: (fileId: number) => `/api/wiki/files/${fileId}/download`,

  // 파일 정보 조회
  getInfo: (fileId: number) =>
    api.get<WikiFile>(`/wiki/files/${fileId}/info`),

  // 문서별 파일 목록 조회
  getByDocument: (documentId: number) =>
    api.get<WikiFile[]>(`/wiki/files/document/${documentId}`),

  // 파일 삭제
  delete: (fileId: number) =>
    api.delete(`/wiki/files/${fileId}`),
};
