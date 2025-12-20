package com.srmanagement.wiki.service;

import com.srmanagement.wiki.dto.EmbeddingProgressEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 임베딩 진행률 관리 서비스
 * - SSE를 통해 클라이언트에 실시간 진행률 전송
 * - 문서별 진행 상태 관리
 */
@Service
@Slf4j
public class EmbeddingProgressService {

    /**
     * 문서 ID별 SSE Emitter 목록 (동일 문서를 여러 클라이언트가 구독 가능)
     */
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * 문서 ID별 현재 진행 상태
     */
    private final Map<Long, EmbeddingProgressEvent> progressStates = new ConcurrentHashMap<>();

    /**
     * SSE 구독 등록
     *
     * @param documentId 문서 ID
     * @return SSE Emitter
     */
    public SseEmitter subscribe(Long documentId) {
        // 타임아웃: 10분 (긴 문서의 임베딩 처리 대응)
        SseEmitter emitter = new SseEmitter(600_000L);

        emitters.computeIfAbsent(documentId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // 연결 종료 시 제거
        emitter.onCompletion(() -> removeEmitter(documentId, emitter));
        emitter.onTimeout(() -> removeEmitter(documentId, emitter));
        emitter.onError(e -> removeEmitter(documentId, emitter));

        // 현재 진행 중인 상태가 있으면 즉시 전송
        EmbeddingProgressEvent currentState = progressStates.get(documentId);
        if (currentState != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data(currentState));
            } catch (IOException e) {
                log.error("초기 상태 전송 실패: documentId={}", documentId, e);
            }
        }

        log.debug("SSE 구독 등록: documentId={}, 구독자 수={}",
                documentId, emitters.get(documentId).size());
        return emitter;
    }

    /**
     * 진행률 이벤트 발송
     *
     * @param event 진행률 이벤트
     */
    public void sendProgress(EmbeddingProgressEvent event) {
        Long documentId = event.getDocumentId();

        // 상태 저장
        progressStates.put(documentId, event);

        // 완료/실패 시 상태 제거 (일정 시간 후)
        if ("COMPLETED".equals(event.getStatus()) || "FAILED".equals(event.getStatus())) {
            // 10초 후 상태 제거 (클라이언트가 최종 상태를 받을 수 있도록)
            new Thread(() -> {
                try {
                    Thread.sleep(10_000);
                    progressStates.remove(documentId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }

        // 구독자들에게 이벤트 전송
        CopyOnWriteArrayList<SseEmitter> documentEmitters = emitters.get(documentId);
        if (documentEmitters == null || documentEmitters.isEmpty()) {
            log.debug("구독자 없음: documentId={}", documentId);
            return;
        }

        for (SseEmitter emitter : documentEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data(event));
            } catch (IOException e) {
                log.debug("SSE 전송 실패, 구독 해제: documentId={}", documentId);
                removeEmitter(documentId, emitter);
            }
        }

        log.debug("진행률 전송: documentId={}, {}/{} ({}%)",
                documentId, event.getCurrentChunk(), event.getTotalChunks(), event.getProgressPercent());
    }

    /**
     * 현재 진행 상태 조회
     *
     * @param documentId 문서 ID
     * @return 진행 상태 (없으면 null)
     */
    public EmbeddingProgressEvent getCurrentProgress(Long documentId) {
        return progressStates.get(documentId);
    }

    /**
     * 진행 중인지 확인
     *
     * @param documentId 문서 ID
     * @return 진행 중 여부
     */
    public boolean isInProgress(Long documentId) {
        EmbeddingProgressEvent state = progressStates.get(documentId);
        return state != null &&
               ("STARTED".equals(state.getStatus()) || "IN_PROGRESS".equals(state.getStatus()));
    }

    private void removeEmitter(Long documentId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> documentEmitters = emitters.get(documentId);
        if (documentEmitters != null) {
            documentEmitters.remove(emitter);
            if (documentEmitters.isEmpty()) {
                emitters.remove(documentId);
            }
        }
    }
}
