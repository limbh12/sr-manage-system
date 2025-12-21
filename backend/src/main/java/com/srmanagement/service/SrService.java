package com.srmanagement.service;

import com.srmanagement.dto.request.SrCreateRequest;
import com.srmanagement.dto.request.SrHistoryCreateRequest;
import com.srmanagement.dto.request.SrStatusUpdateRequest;
import com.srmanagement.dto.request.SrUpdateRequest;
import com.srmanagement.dto.response.SrHistoryResponse;
import com.srmanagement.dto.response.SrResponse;
import com.srmanagement.entity.*;
import com.srmanagement.exception.CustomException;
import com.srmanagement.repository.SrHistoryRepository;
import com.srmanagement.repository.SrRepository;
import com.srmanagement.repository.UserRepository;
import com.srmanagement.wiki.service.ContentEmbeddingService;
import com.srmanagement.wiki.service.WikiNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SR 서비스
 * 
 * SR 관련 비즈니스 로직을 처리합니다.
 */
@Service
public class SrService {

    @Autowired
    private SrRepository srRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SrHistoryRepository srHistoryRepository;

    @Autowired
    private WikiNotificationService notificationService;

    @Autowired(required = false)
    private ContentEmbeddingService contentEmbeddingService;

    /**
     * SR ID 생성 (SR-YYMM-XXXX)
     */
    private String generateSrId() {
        LocalDateTime now = LocalDateTime.now();
        String datePrefix = now.format(DateTimeFormatter.ofPattern("yyMM"));
        String pattern = "SR-" + datePrefix + "-%";
        
        String lastSrId = srRepository.findLastSrId(pattern);
        int sequence = 1;
        
        if (lastSrId != null) {
            String lastSequenceStr = lastSrId.substring(lastSrId.lastIndexOf("-") + 1);
            try {
                sequence = Integer.parseInt(lastSequenceStr) + 1;
            } catch (NumberFormatException e) {
                // 시퀀스 파싱 실패 시 1부터 시작
            }
        }
        
        return String.format("SR-%s-%04d", datePrefix, sequence);
    }

    /**
     * SR 목록 조회
     * @param status 상태 필터 (optional)
     * @param priority 우선순위 필터 (optional)
     * @param category 분류 필터 (optional)
     * @param requestType 요청구분 필터 (optional)
     * @param assigneeId 담당자 ID 필터 (optional)
     * @param search 검색어 (optional) - SR ID, 제목, 설명, 요청자명, 전화번호로 검색
     * @param includeDeleted 삭제된 항목 포함 여부 (관리자용)
     * @param pageable 페이지네이션
     * @return Page<SrResponse>
     */
    @Transactional(readOnly = true)
    public Page<SrResponse> getSrList(SrStatus status, Priority priority, String category, String requestType,
                                      Long assigneeId, String search, Boolean includeDeleted, Pageable pageable) {
        // 삭제된 항목 포함 여부 결정 (관리자가 아니면 무조건 false)
        Boolean deleted = (includeDeleted != null && includeDeleted) ? null : false;

        // 복합 필터 쿼리 사용
        Page<Sr> srPage = srRepository.findByMultipleFilters(
                deleted,
                status,
                priority,
                category,
                requestType,
                assigneeId,
                search,
                pageable
        );

        // 검색어가 있고 요청자 정보로 검색 가능한 경우, 추가 필터링
        if (search != null && !search.isEmpty()) {
            String searchLower = search.toLowerCase();
            // 암호화된 필드(applicantName, applicantPhone)는 복호화 후 검색
            // JPA Converter가 자동으로 복호화하므로 엔티티에서 직접 비교 가능
            List<Sr> filteredList = srPage.getContent().stream()
                    .filter(sr -> {
                        // 기본 검색 (srId, title, description)은 이미 쿼리에서 처리됨
                        // 추가로 요청자 정보로 검색
                        if (sr.getApplicantName() != null && sr.getApplicantName().toLowerCase().contains(searchLower)) {
                            return true;
                        }
                        if (sr.getApplicantPhone() != null && sr.getApplicantPhone().replace("-", "").contains(search.replace("-", ""))) {
                            return true;
                        }
                        // 이미 다른 필드로 매칭된 경우도 포함
                        return sr.getSrId() != null && sr.getSrId().toLowerCase().contains(searchLower) ||
                               sr.getTitle().toLowerCase().contains(searchLower) ||
                               (sr.getDescription() != null && sr.getDescription().toLowerCase().contains(searchLower));
                    })
                    .collect(java.util.stream.Collectors.toList());

            // 페이지 정보 유지하면서 필터링된 결과 반환
            return new org.springframework.data.domain.PageImpl<>(
                    filteredList.stream().map(SrResponse::from).collect(java.util.stream.Collectors.toList()),
                    pageable,
                    filteredList.size()
            );
        }

        return srPage.map(SrResponse::from);
    }

    /**
     * SR 상세 조회
     * @param id SR ID
     * @return SrResponse
     */
    @Transactional(readOnly = true)
    public SrResponse getSrById(Long id) {
        Sr sr = srRepository.findById(id)
                .orElseThrow(() -> new CustomException("SR not found with id: " + id, HttpStatus.NOT_FOUND));
        return SrResponse.from(sr);
    }

    /**
     * SR 생성
     * @param request SR 생성 요청 DTO
     * @param username 요청자 사용자명
     * @return SrResponse
     */
    @Transactional
    public SrResponse createSr(SrCreateRequest request, String username) {
        return createSr(request, username, true);
    }

    /**
     * SR 생성 (임베딩 생성 여부 지정 가능)
     * @param request SR 생성 요청 DTO
     * @param username 요청자 사용자명
     * @param generateEmbedding 임베딩 생성 여부 (일괄 등록 시 false)
     * @return SrResponse
     */
    @Transactional
    public SrResponse createSr(SrCreateRequest request, String username, boolean generateEmbedding) {
        User requester = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        User assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new CustomException("Assignee not found", HttpStatus.NOT_FOUND));
        }

        Sr sr = Sr.builder()
                .srId(generateSrId())
                .title(request.getTitle())
                .description(request.getDescription())
                .status(SrStatus.OPEN)
                .priority(request.getPriority() != null ? request.getPriority() : Priority.MEDIUM)
                .category(request.getCategory())
                .requestType(request.getRequestType())
                .requester(requester)
                .assignee(assignee)
                .openApiSurveyId(request.getOpenApiSurveyId())
                .applicantName(request.getApplicantName())
                .applicantPhone(request.getApplicantPhone())
                .expectedCompletionDate(request.getExpectedCompletionDate())
                .build();

        Sr savedSr = srRepository.save(sr);

        // SR 생성 알림 발송 (담당자에게)
        if (assignee != null) {
            notificationService.notifySrCreated(savedSr.getId(), savedSr.getTitle(), assignee, requester);
        }

        // SR 임베딩 비동기 생성 (AI 검색용) - 트랜잭션 커밋 후 실행
        // 일괄 등록 시에는 generateEmbedding=false로 호출하여 임베딩 생성 생략
        if (generateEmbedding && contentEmbeddingService != null) {
            final Long srId = savedSr.getId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    contentEmbeddingService.generateSrEmbeddingAsync(srId);
                }
            });
        }

        return SrResponse.from(savedSr);
    }

    private String getPriorityLabel(Priority priority) {
        if (priority == null) return "";
        switch (priority) {
            case LOW: return "낮음";
            case MEDIUM: return "보통";
            case HIGH: return "높음";
            case CRITICAL: return "긴급";
            default: return priority.name();
        }
    }

    private String getStatusLabel(SrStatus status) {
        if (status == null) return "";
        switch (status) {
            case OPEN: return "신규";
            case IN_PROGRESS: return "처리중";
            case RESOLVED: return "해결됨";
            case CLOSED: return "종료";
            default: return status.name();
        }
    }

    /**
     * SR 수정
     * @param id SR ID
     * @param request SR 수정 요청 DTO
     * @param username 수정자 사용자명
     * @return SrResponse
     */
    @Transactional
    public SrResponse updateSr(Long id, SrUpdateRequest request, String username) {
        Sr sr = srRepository.findById(id)
                .orElseThrow(() -> new CustomException("SR not found with id: " + id, HttpStatus.NOT_FOUND));
        
        User modifier = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        if (request.getTitle() != null && !request.getTitle().equals(sr.getTitle())) {
            createHistory(sr, "제목이 변경되었습니다: " + sr.getTitle() + " -> " + request.getTitle(), SrHistoryType.INFO_CHANGE, modifier);
            sr.setTitle(request.getTitle());
        }
        if (request.getDescription() != null && !request.getDescription().equals(sr.getDescription())) {
            createHistory(sr, "요청사항이 변경되었습니다.", SrHistoryType.INFO_CHANGE, modifier, sr.getDescription(), request.getDescription());
            sr.setDescription(request.getDescription());
        }
        if (request.getProcessingDetails() != null && !request.getProcessingDetails().equals(sr.getProcessingDetails())) {
            createHistory(sr, "처리내용이 변경되었습니다.", SrHistoryType.INFO_CHANGE, modifier, sr.getProcessingDetails(), request.getProcessingDetails());
            sr.setProcessingDetails(request.getProcessingDetails());
        }
        if (request.getStatus() != null && request.getStatus() != sr.getStatus()) {
            createHistory(sr, "상태가 변경되었습니다: " + getStatusLabel(sr.getStatus()) + " -> " + getStatusLabel(request.getStatus()), SrHistoryType.STATUS_CHANGE, modifier);
            sr.setStatus(request.getStatus());
        }
        if (request.getPriority() != null && request.getPriority() != sr.getPriority()) {
            createHistory(sr, "우선순위가 변경되었습니다: " + getPriorityLabel(sr.getPriority()) + " -> " + getPriorityLabel(request.getPriority()), SrHistoryType.PRIORITY_CHANGE, modifier);
            sr.setPriority(request.getPriority());
        }
        if (request.getCategory() != null && !request.getCategory().equals(sr.getCategory())) {
            createHistory(sr, "분류가 변경되었습니다: " + (sr.getCategory() != null ? sr.getCategory() : "없음") + " -> " + request.getCategory(), SrHistoryType.INFO_CHANGE, modifier);
            sr.setCategory(request.getCategory());
        }
        if (request.getRequestType() != null && !request.getRequestType().equals(sr.getRequestType())) {
            createHistory(sr, "요청구분이 변경되었습니다: " + (sr.getRequestType() != null ? sr.getRequestType() : "없음") + " -> " + request.getRequestType(), SrHistoryType.INFO_CHANGE, modifier);
            sr.setRequestType(request.getRequestType());
        }
        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new CustomException("Assignee not found", HttpStatus.NOT_FOUND));

            if (sr.getAssignee() == null || !sr.getAssignee().getId().equals(assignee.getId())) {
                String oldAssigneeName = sr.getAssignee() != null ? sr.getAssignee().getName() : "없음";
                createHistory(sr, "담당자가 변경되었습니다: " + oldAssigneeName + " -> " + assignee.getName(), SrHistoryType.ASSIGNEE_CHANGE, modifier);

                // 담당자 변경 알림 발송 (새 담당자에게)
                notificationService.notifySrAssigned(sr.getId(), sr.getTitle(), assignee, modifier);

                sr.setAssignee(assignee);
            }
        }
        if (request.getOpenApiSurveyId() != null) {
            sr.setOpenApiSurveyId(request.getOpenApiSurveyId());
        }
        if (request.getApplicantName() != null && !request.getApplicantName().equals(sr.getApplicantName())) {
            createHistory(sr, "요청자 이름이 변경되었습니다: " + (sr.getApplicantName() != null ? sr.getApplicantName() : "없음") + " -> " + request.getApplicantName(), SrHistoryType.INFO_CHANGE, modifier);
            sr.setApplicantName(request.getApplicantName());
        }
        if (request.getApplicantPhone() != null && !request.getApplicantPhone().equals(sr.getApplicantPhone())) {
            createHistory(sr, "요청자 연락처가 변경되었습니다: " + (sr.getApplicantPhone() != null ? sr.getApplicantPhone() : "없음") + " -> " + request.getApplicantPhone(), SrHistoryType.INFO_CHANGE, modifier);
            sr.setApplicantPhone(request.getApplicantPhone());
        }
        if (request.getExpectedCompletionDate() != null) {
            if (sr.getExpectedCompletionDate() == null || !request.getExpectedCompletionDate().equals(sr.getExpectedCompletionDate())) {
                String oldDate = sr.getExpectedCompletionDate() != null ? sr.getExpectedCompletionDate().toString() : "없음";
                String newDate = request.getExpectedCompletionDate().toString();
                createHistory(sr, "처리예정일자가 변경되었습니다: " + oldDate + " -> " + newDate, SrHistoryType.INFO_CHANGE, modifier);
                sr.setExpectedCompletionDate(request.getExpectedCompletionDate());
            }
        } else if (sr.getExpectedCompletionDate() != null) {
            createHistory(sr, "처리예정일자가 삭제되었습니다: " + sr.getExpectedCompletionDate().toString() + " -> 없음", SrHistoryType.INFO_CHANGE, modifier);
            sr.setExpectedCompletionDate(null);
        }

        Sr updatedSr = srRepository.save(sr);

        // SR 수정 알림 발송 (담당자와 등록자에게)
        notificationService.notifySrUpdated(
                updatedSr.getId(),
                updatedSr.getTitle(),
                updatedSr.getAssignee(),
                updatedSr.getRequester(),
                modifier
        );

        // SR 임베딩 비동기 재생성 (AI 검색용) - 트랜잭션 커밋 후 실행
        if (contentEmbeddingService != null) {
            final Long srId = updatedSr.getId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    contentEmbeddingService.generateSrEmbeddingAsync(srId);
                }
            });
        }

        return SrResponse.from(updatedSr);
    }

    /**
     * SR 삭제 (소프트 삭제)
     * @param id SR ID
     * @param username 요청자 사용자명
     */
    @Transactional
    public void deleteSr(Long id, String username) {
        Sr sr = srRepository.findById(id)
                .orElseThrow(() -> new CustomException("SR not found with id: " + id, HttpStatus.NOT_FOUND));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        // 권한 체크: 관리자이거나 본인이 작성한 SR인 경우에만 삭제 가능
        if (user.getRole() != Role.ADMIN && !sr.getRequester().getId().equals(user.getId())) {
            throw new CustomException("삭제 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }

        // 소프트 삭제: deleted 플래그를 true로 설정
        sr.setDeleted(true);
        sr.setDeletedAt(LocalDateTime.now());
        srRepository.save(sr);

        // 삭제 이력 기록
        createHistory(sr, "SR이 삭제되었습니다.", SrHistoryType.INFO_CHANGE, user);

        // SR 임베딩 제거 (삭제된 SR은 검색에서 제외) - 트랜잭션 커밋 후 실행
        if (contentEmbeddingService != null) {
            final Long srId = sr.getId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    contentEmbeddingService.generateSrEmbeddingAsync(srId);
                }
            });
        }
    }

    /**
     * SR 복구 (소프트 삭제 취소) - 관리자 전용
     * @param id SR ID
     * @param username 요청자 사용자명
     * @return SrResponse
     */
    @Transactional
    public SrResponse restoreSr(Long id, String username) {
        Sr sr = srRepository.findById(id)
                .orElseThrow(() -> new CustomException("SR not found with id: " + id, HttpStatus.NOT_FOUND));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        // 권한 체크: 관리자만 복구 가능
        if (user.getRole() != Role.ADMIN) {
            throw new CustomException("복구 권한이 없습니다. 관리자만 복구할 수 있습니다.", HttpStatus.FORBIDDEN);
        }

        // 이미 복구된 SR인지 확인
        if (!sr.getDeleted()) {
            throw new CustomException("이미 복구된 SR입니다.", HttpStatus.BAD_REQUEST);
        }

        // SR 복구
        sr.restore();
        Sr restoredSr = srRepository.save(sr);

        // 복구 이력 기록
        createHistory(sr, "SR이 복구되었습니다.", SrHistoryType.INFO_CHANGE, user);

        return SrResponse.from(restoredSr);
    }

    /**
     * SR 상태 변경
     * @param id SR ID
     * @param request 상태 변경 요청 DTO
     * @param username 수정자 사용자명
     * @return SrResponse
     */
    @Transactional
    public SrResponse updateSrStatus(Long id, SrStatusUpdateRequest request, String username) {
        Sr sr = srRepository.findById(id)
                .orElseThrow(() -> new CustomException("SR not found with id: " + id, HttpStatus.NOT_FOUND));

        User modifier = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        if (request.getStatus() != sr.getStatus()) {
            createHistory(sr, "상태가 변경되었습니다: " + getStatusLabel(sr.getStatus()) + " -> " + getStatusLabel(request.getStatus()), SrHistoryType.STATUS_CHANGE, modifier);
            SrStatus oldStatus = sr.getStatus();
            sr.setStatus(request.getStatus());

            Sr updatedSr = srRepository.save(sr);

            // SR 상태 변경 알림 발송 (담당자와 등록자에게)
            notificationService.notifySrStatusChanged(
                    updatedSr.getId(),
                    updatedSr.getTitle(),
                    request.getStatus().name(),
                    updatedSr.getAssignee(),
                    updatedSr.getRequester(),
                    modifier
            );

            return SrResponse.from(updatedSr);
        }

        Sr updatedSr = srRepository.save(sr);
        return SrResponse.from(updatedSr);
    }

    /**
     * SR 이력 목록 조회
     * @param srId SR ID
     * @return List<SrHistoryResponse>
     */
    @Transactional(readOnly = true)
    public List<SrHistoryResponse> getSrHistories(Long srId) {
        if (!srRepository.existsById(srId)) {
            throw new CustomException("SR not found with id: " + srId, HttpStatus.NOT_FOUND);
        }
        return srHistoryRepository.findBySrIdOrderByCreatedAtDesc(srId).stream()
                .map(SrHistoryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * SR 이력(댓글) 생성
     * @param srId SR ID
     * @param request 이력 생성 요청 DTO
     * @param username 작성자 사용자명
     * @return SrHistoryResponse
     */
    @Transactional
    public SrHistoryResponse createSrHistory(Long srId, SrHistoryCreateRequest request, String username) {
        Sr sr = srRepository.findById(srId)
                .orElseThrow(() -> new CustomException("SR not found with id: " + srId, HttpStatus.NOT_FOUND));
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        SrHistory history = createHistory(sr, request.getContent(), SrHistoryType.COMMENT, user, null, null);
        return SrHistoryResponse.from(history);
    }

    private SrHistory createHistory(Sr sr, String content, SrHistoryType type, User user) {
        return createHistory(sr, content, type, user, null, null);
    }

    private SrHistory createHistory(Sr sr, String content, SrHistoryType type, User user, String previousValue, String newValue) {
        SrHistory history = SrHistory.builder()
                .sr(sr)
                .content(content)
                .historyType(type)
                .createdBy(user)
                .previousValue(previousValue)
                .newValue(newValue)
                .build();
        return srHistoryRepository.save(history);
    }
}
