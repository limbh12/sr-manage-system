import api from './api';
import { AiSearchRequest, AiSearchResponse, EmbeddingStatusResponse, EmbeddingProgressEvent, SummaryResponse } from '../types/aiSearch';

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
}

export default new AiSearchService();
