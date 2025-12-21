package com.srmanagement.wiki.service;

import com.srmanagement.entity.User;
import com.srmanagement.repository.UserRepository;
import com.srmanagement.wiki.dto.AiSearchHistoryResponse;
import com.srmanagement.wiki.dto.AiSearchResponse;
import com.srmanagement.wiki.entity.AiSearchHistory;
import com.srmanagement.wiki.repository.AiSearchHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 검색 이력 서비스
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AiSearchHistoryService {

    private final AiSearchHistoryRepository historyRepository;
    private final UserRepository userRepository;

    private static final int MAX_ANSWER_LENGTH = 5000;

    /**
     * 검색 이력 저장 (비동기)
     */
    @Async
    @Transactional
    public void saveSearchHistoryAsync(String username, String question, AiSearchResponse response, List<String> resourceTypes) {
        try {
            saveSearchHistory(username, question, response, resourceTypes);
        } catch (Exception e) {
            log.error("검색 이력 저장 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 검색 이력 저장
     */
    @Transactional
    public void saveSearchHistory(String username, String question, AiSearchResponse response, List<String> resourceTypes) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));

        String answer = response.getAnswer();
        if (answer != null && answer.length() > MAX_ANSWER_LENGTH) {
            answer = answer.substring(0, MAX_ANSWER_LENGTH) + "...";
        }

        String resourceTypesStr = null;
        if (resourceTypes != null && !resourceTypes.isEmpty()) {
            resourceTypesStr = String.join(",", resourceTypes);
        }

        AiSearchHistory history = AiSearchHistory.builder()
                .user(user)
                .question(question)
                .answer(answer)
                .sourceCount(response.getSources() != null ? response.getSources().size() : 0)
                .resourceTypes(resourceTypesStr)
                .processingTimeMs(response.getProcessingTimeMs())
                .build();

        historyRepository.save(history);
        log.debug("검색 이력 저장 완료: userId={}, question={}", user.getId(), question);
    }

    /**
     * 특정 사용자의 최근 검색 이력 조회
     */
    @Transactional(readOnly = true)
    public List<AiSearchHistoryResponse> getRecentHistory(String username, int limit) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));

        List<AiSearchHistory> histories = historyRepository.findRecentByUserId(
                user.getId(),
                PageRequest.of(0, limit)
        );

        return histories.stream()
                .map(AiSearchHistoryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 사용자의 검색 이력 페이징 조회
     */
    @Transactional(readOnly = true)
    public Page<AiSearchHistoryResponse> getHistoryPage(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));

        return historyRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(AiSearchHistoryResponse::from);
    }

    /**
     * 전체 검색 이력 페이징 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public Page<AiSearchHistoryResponse> getAllHistoryPage(Pageable pageable) {
        return historyRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(AiSearchHistoryResponse::from);
    }

    /**
     * 검색 이력 삭제
     */
    @Transactional
    public void deleteHistory(Long historyId, String username) {
        AiSearchHistory history = historyRepository.findById(historyId)
                .orElseThrow(() -> new RuntimeException("검색 이력을 찾을 수 없습니다: " + historyId));

        if (!history.getUser().getUsername().equals(username)) {
            throw new RuntimeException("권한이 없습니다.");
        }

        historyRepository.delete(history);
        log.info("검색 이력 삭제: id={}", historyId);
    }

    /**
     * 오래된 검색 이력 정리 (30일 이전)
     */
    @Transactional
    public int cleanupOldHistory(int daysOld) {
        LocalDateTime before = LocalDateTime.now().minusDays(daysOld);
        int deleted = historyRepository.deleteByCreatedAtBefore(before);
        log.info("오래된 검색 이력 정리: {}건 삭제 ({}일 이전)", deleted, daysOld);
        return deleted;
    }

    /**
     * 질문으로 검색
     */
    @Transactional(readOnly = true)
    public List<AiSearchHistoryResponse> searchHistory(String username, String keyword, int limit) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));

        List<AiSearchHistory> histories = historyRepository.searchByQuestion(
                user.getId(),
                keyword,
                PageRequest.of(0, limit)
        );

        return histories.stream()
                .map(AiSearchHistoryResponse::from)
                .collect(Collectors.toList());
    }
}
