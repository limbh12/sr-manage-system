import api from './api';
import { AiSearchRequest, AiSearchResponse, EmbeddingStatusResponse, EmbeddingProgressEvent, SummaryResponse, EmbeddingStats, AiSearchHistoryResponse, Page, BulkEmbeddingProgressEvent, BulkEmbeddingStartResponse, ResourceType, SrEmbeddingStatusResponse, SurveyEmbeddingStatusResponse } from '../types/aiSearch';

/**
 * AI 검색 서비스
 */
class AiSearchService {
  /**
   * AI 기반 자연어 검색 (RAG)
   */
  async search(request: AiSearchRequest): Promise<AiSearchResponse> {
    const response = await api.post<AiSearchResponse>('/wiki/search/ai', request);
    return response.data;
  }

  /**
   * 문서 임베딩 생성 (동기 - 수동 트리거)
   */
  async generateEmbeddings(documentId: number): Promise<string> {
    const response = await api.post<string>(`/wiki/search/embeddings/${documentId}`);
    return response.data;
  }

  /**
   * 문서 임베딩 생성 (비동기 - 진행률 추적)
   */
  async generateEmbeddingsAsync(documentId: number): Promise<string> {
    const response = await api.post<string>(`/wiki/search/embeddings/async/${documentId}`);
    return response.data;
  }

  /**
   * 문서 임베딩 상태 조회
   */
  async getEmbeddingStatus(documentId: number): Promise<EmbeddingStatusResponse> {
    const response = await api.get<EmbeddingStatusResponse>(`/wiki/search/embeddings/status/${documentId}`);
    return response.data;
  }

  /**
   * 현재 임베딩 진행 상태 조회 (폴링용)
   */
  async getCurrentProgress(documentId: number): Promise<EmbeddingProgressEvent | null> {
    try {
      const response = await api.get<EmbeddingProgressEvent>(`/wiki/search/embeddings/progress/current/${documentId}`);
      return response.data;
    } catch {
      return null;
    }
  }

  /**
   * 임베딩 진행률 폴링 구독
   * - 인증이 필요한 API이므로 SSE 대신 폴링 사용
   * @param documentId 문서 ID
   * @param onProgress 진행률 콜백
   * @param onComplete 완료 콜백
   * @param onError 에러 콜백
   * @returns 구독 해제 함수
   */
  subscribeProgress(
    documentId: number,
    onProgress: (event: EmbeddingProgressEvent) => void,
    onComplete?: () => void,
    onError?: (error: Error) => void
  ): () => void {
    let isActive = true;
    let intervalId: ReturnType<typeof setInterval> | null = null;

    const poll = async () => {
      if (!isActive) return;

      try {
        const progress = await this.getCurrentProgress(documentId);
        if (!isActive) return;

        if (progress) {
          onProgress(progress);

          if (progress.status === 'COMPLETED') {
            if (intervalId) clearInterval(intervalId);
            onComplete?.();
          } else if (progress.status === 'FAILED') {
            if (intervalId) clearInterval(intervalId);
            onError?.(new Error(progress.message || '임베딩 생성 실패'));
          }
        }
      } catch (error) {
        console.error('폴링 오류:', error);
        // 오류가 발생해도 계속 폴링
      }
    };

    // 즉시 한 번 실행 후 1초마다 폴링
    poll();
    intervalId = setInterval(poll, 1000);

    // 구독 해제 함수 반환
    return () => {
      isActive = false;
      if (intervalId) clearInterval(intervalId);
    };
  }

  /**
   * 문서 AI 요약 생성
   * @param documentId 문서 ID
   * @param forceRegenerate 강제 재생성 여부
   */
  async generateSummary(documentId: number, forceRegenerate = false): Promise<SummaryResponse> {
    const response = await api.post<SummaryResponse>(
      `/wiki/search/summary/${documentId}?forceRegenerate=${forceRegenerate}`
    );
    return response.data;
  }

  /**
   * 문서 AI 요약 상태 조회 (폴링용)
   * - 생성 중이면 GENERATING 상태 반환
   * - 캐시가 있으면 CACHED 상태와 요약 반환
   * - 없으면 NEEDS_UPDATE 상태 반환
   * @param documentId 문서 ID
   */
  async getSummaryStatus(documentId: number): Promise<SummaryResponse> {
    const response = await api.get<SummaryResponse>(`/wiki/search/summary/${documentId}`);
    return response.data;
  }

  /**
   * 임베딩 통계 조회 (Wiki, SR, Survey 각각의 임베딩 개수)
   */
  async getEmbeddingStats(): Promise<EmbeddingStats> {
    const response = await api.get<EmbeddingStats>('/wiki/search/embeddings/stats');
    return response.data;
  }

  /**
   * 전체 Wiki 문서 임베딩 생성 시작 (비동기)
   */
  async generateAllWikiEmbeddings(): Promise<BulkEmbeddingStartResponse> {
    const response = await api.post<BulkEmbeddingStartResponse>('/wiki/search/embeddings/wiki/all');
    return response.data;
  }

  /**
   * 전체 SR 임베딩 생성 시작 (비동기)
   */
  async generateAllSrEmbeddings(): Promise<BulkEmbeddingStartResponse> {
    const response = await api.post<BulkEmbeddingStartResponse>('/wiki/search/embeddings/sr/all');
    return response.data;
  }

  /**
   * 전체 현황조사 임베딩 생성 시작 (비동기)
   */
  async generateAllSurveyEmbeddings(): Promise<BulkEmbeddingStartResponse> {
    const response = await api.post<BulkEmbeddingStartResponse>('/wiki/search/embeddings/survey/all');
    return response.data;
  }

  /**
   * 일괄 임베딩 진행 상태 조회 (폴링용)
   * @param resourceType 리소스 타입 (WIKI, SR, SURVEY)
   */
  async getBulkProgress(resourceType: ResourceType): Promise<BulkEmbeddingProgressEvent | null> {
    try {
      const response = await api.get<BulkEmbeddingProgressEvent>(`/wiki/search/embeddings/bulk/progress/${resourceType}`);
      return response.data;
    } catch {
      return null;
    }
  }

  /**
   * 일괄 임베딩 진행률 폴링 구독
   * @param resourceType 리소스 타입
   * @param onProgress 진행률 콜백
   * @param onComplete 완료 콜백
   * @param onError 에러 콜백
   * @returns 구독 해제 함수
   */
  subscribeBulkProgress(
    resourceType: ResourceType,
    onProgress: (event: BulkEmbeddingProgressEvent) => void,
    onComplete?: (event: BulkEmbeddingProgressEvent) => void,
    onError?: (error: Error) => void
  ): () => void {
    let isActive = true;
    let intervalId: ReturnType<typeof setInterval> | null = null;

    const poll = async () => {
      if (!isActive) return;

      try {
        const progress = await this.getBulkProgress(resourceType);
        if (!isActive) return;

        if (progress) {
          onProgress(progress);

          if (progress.status === 'COMPLETED') {
            if (intervalId) clearInterval(intervalId);
            onComplete?.(progress);
          } else if (progress.status === 'FAILED') {
            if (intervalId) clearInterval(intervalId);
            onError?.(new Error(progress.message || '일괄 임베딩 생성 실패'));
          }
        }
      } catch (error) {
        console.error('폴링 오류:', error);
        // 오류가 발생해도 계속 폴링
      }
    };

    // 즉시 한 번 실행 후 1초마다 폴링
    poll();
    intervalId = setInterval(poll, 1000);

    // 구독 해제 함수 반환
    return () => {
      isActive = false;
      if (intervalId) clearInterval(intervalId);
    };
  }

  /**
   * 개별 SR 임베딩 생성
   */
  async generateSrEmbedding(srId: number): Promise<string> {
    const response = await api.post<string>(`/wiki/search/embeddings/sr/${srId}`);
    return response.data;
  }

  /**
   * 개별 현황조사 임베딩 생성
   */
  async generateSurveyEmbedding(surveyId: number): Promise<string> {
    const response = await api.post<string>(`/wiki/search/embeddings/survey/${surveyId}`);
    return response.data;
  }

  /**
   * SR 임베딩 상태 조회
   */
  async getSrEmbeddingStatus(srId: number): Promise<SrEmbeddingStatusResponse> {
    const response = await api.get<SrEmbeddingStatusResponse>(`/wiki/search/embeddings/sr/status/${srId}`);
    return response.data;
  }

  /**
   * Survey 임베딩 상태 조회
   */
  async getSurveyEmbeddingStatus(surveyId: number): Promise<SurveyEmbeddingStatusResponse> {
    const response = await api.get<SurveyEmbeddingStatusResponse>(`/wiki/search/embeddings/survey/status/${surveyId}`);
    return response.data;
  }

  // ==================== 검색 이력 API ====================

  /**
   * 최근 검색 이력 조회
   * @param limit 조회 개수 (기본값: 10)
   */
  async getRecentHistory(limit = 10): Promise<AiSearchHistoryResponse[]> {
    const response = await api.get<AiSearchHistoryResponse[]>(`/wiki/search/history/recent?limit=${limit}`);
    return response.data;
  }

  /**
   * 검색 이력 페이징 조회
   * @param page 페이지 번호 (0부터 시작)
   * @param size 페이지 크기
   */
  async getHistoryPage(page = 0, size = 20): Promise<Page<AiSearchHistoryResponse>> {
    const response = await api.get<Page<AiSearchHistoryResponse>>(`/wiki/search/history?page=${page}&size=${size}`);
    return response.data;
  }

  /**
   * 검색 이력 키워드 검색
   * @param keyword 검색 키워드
   * @param limit 조회 개수
   */
  async searchHistory(keyword: string, limit = 10): Promise<AiSearchHistoryResponse[]> {
    const response = await api.get<AiSearchHistoryResponse[]>(
      `/wiki/search/history/search?keyword=${encodeURIComponent(keyword)}&limit=${limit}`
    );
    return response.data;
  }

  /**
   * 검색 이력 삭제
   * @param historyId 이력 ID
   */
  async deleteHistory(historyId: number): Promise<void> {
    await api.delete(`/wiki/search/history/${historyId}`);
  }
}

export default new AiSearchService();
