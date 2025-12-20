/**
 * AI 검색 요청
 */
export interface AiSearchRequest {
  question: string;
  topK?: number;
  categoryId?: number;
  similarityThreshold?: number;
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
  documentId: number;
  title: string;
  categoryName?: string;
  snippet: string;
  relevanceScore: number;
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
