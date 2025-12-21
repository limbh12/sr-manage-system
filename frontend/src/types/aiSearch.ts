/**
 * 리소스 타입
 */
export type ResourceType = 'WIKI' | 'SR' | 'SURVEY';

/**
 * AI 검색 요청
 */
export interface AiSearchRequest {
  question: string;
  topK?: number;
  categoryId?: number;
  similarityThreshold?: number;
  resourceTypes?: ResourceType[];
  useUnifiedSearch?: boolean;
}

/**
 * AI 검색 응답
 */
export interface AiSearchResponse {
  answer: string;
  sources: SourceDocument[];
  processingTimeMs: number;
}

/**
 * 참고 문서
 */
export interface SourceDocument {
  resourceType?: ResourceType;
  resourceId?: number;
  resourceIdentifier?: string;
  documentId?: number;
  title: string;
  categoryName?: string;
  status?: string;
  snippet: string;
  relevanceScore: number;
}

/**
 * 임베딩 통계
 */
export interface EmbeddingStats {
  wiki: number;
  sr: number;
  survey: number;
  total: number;
}

/**
 * 임베딩 상태 응답
 */
export interface EmbeddingStatusResponse {
  documentId: number;
  hasEmbedding: boolean;
  chunkCount: number;
  lastEmbeddingDate?: string;
  documentUpdatedAt?: string;
  isUpToDate: boolean;
}

/**
 * 임베딩 진행률 이벤트
 */
export interface EmbeddingProgressEvent {
  documentId: number;
  documentTitle?: string;
  status: 'STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED';
  currentChunk: number;
  totalChunks: number;
  progressPercent: number;
  chunkProcessingTimeMs?: number;
  elapsedTimeMs?: number;
  estimatedRemainingMs?: number;
  message?: string;
}

/**
 * AI 요약 응답
 */
export interface SummaryResponse {
  documentId: number;
  summary?: string;
  generatedAt?: string;
  processingTimeMs?: number;
  status: 'GENERATED' | 'CACHED' | 'FAILED' | 'GENERATING' | 'NEEDS_UPDATE';
  message?: string;
}

/**
 * AI 검색 이력 응답
 */
export interface AiSearchHistoryResponse {
  id: number;
  question: string;
  answerPreview?: string;
  sourceCount: number;
  resourceTypes?: string[];
  processingTimeMs?: number;
  createdAt: string;
  username?: string;
}

/**
 * 페이지 응답
 */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

/**
 * 일괄 임베딩 진행률 이벤트
 */
export interface BulkEmbeddingProgressEvent {
  resourceType: ResourceType;
  status: 'STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED';
  currentIndex: number;
  totalCount: number;
  successCount: number;
  failureCount: number;
  progressPercent: number;
  currentTitle?: string;
  elapsedTimeMs?: number;
  estimatedRemainingMs?: number;
  message?: string;
}

/**
 * 일괄 임베딩 시작 응답
 */
export interface BulkEmbeddingStartResponse {
  message: string;
  totalCount?: number;
  status: 'STARTED' | 'IN_PROGRESS';
}

/**
 * SR 임베딩 상태 응답
 */
export interface SrEmbeddingStatusResponse {
  id: number;
  srId: string;
  title: string;
  hasEmbedding: boolean;
  chunkCount: number;
  lastEmbeddingDate?: string;
  sourceUpdatedAt?: string;
  isUpToDate: boolean;
}

/**
 * Survey 임베딩 상태 응답
 */
export interface SurveyEmbeddingStatusResponse {
  id: number;
  systemName: string;
  organizationName: string;
  hasEmbedding: boolean;
  chunkCount: number;
  lastEmbeddingDate?: string;
  sourceUpdatedAt?: string;
  isUpToDate: boolean;
}
