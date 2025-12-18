// SR 정보 (Wiki 문서 연계용)
export interface SrInfo {
  id: number;
  title: string;
  status: string;
}

// Wiki 문서
export interface WikiDocument {
  id: number;
  title: string;
  content: string;
  categoryId?: number;
  categoryName?: string;
  srs: SrInfo[];
  createdById: number;
  createdByName: string;
  updatedById?: number;
  updatedByName?: string;
  createdAt: string;
  updatedAt: string;
  viewCount: number;
  currentVersion?: number;
}

// Wiki 문서 생성/수정 요청
export interface WikiDocumentRequest {
  title: string;
  content: string;
  categoryId?: number;
  srIds?: number[];
  changeSummary?: string;
}

// Wiki 카테고리
export interface WikiCategory {
  id: number;
  name: string;
  parentId?: number;
  parentName?: string;
  sortOrder: number;
  createdAt: string;
  documentCount: number;
  children?: WikiCategory[];
}

// Wiki 카테고리 생성/수정 요청
export interface WikiCategoryRequest {
  name: string;
  parentId?: number;
  sortOrder?: number;
}

// Wiki 버전
export interface WikiVersion {
  id: number;
  documentId: number;
  version: number;
  content: string;
  changeSummary: string;
  createdById: number;
  createdByName: string;
  createdAt: string;
}

// Wiki 파일
export interface WikiFile {
  id: number;
  documentId?: number;
  originalFileName: string;
  storedFileName: string;
  fileSize: number;
  fileType: string;
  type: 'IMAGE' | 'DOCUMENT' | 'ATTACHMENT';
  uploadedById: number;
  uploadedByName: string;
  uploadedAt: string;
  downloadUrl: string;
}

// 페이지 응답
export interface WikiPageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}
