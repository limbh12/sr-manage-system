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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * @param search 검색어 (optional)
     * @param pageable 페이지네이션
     * @return Page<SrResponse>
     */
    @Transactional(readOnly = true)
    public Page<SrResponse> getSrList(SrStatus status, Priority priority, String search, Pageable pageable) {
        Page<Sr> srPage;
        
        if (search != null && !search.isEmpty()) {
            srPage = srRepository.searchByTitleOrDescription(search, pageable);
        } else if (status != null && priority != null) {
            srPage = srRepository.findByStatusAndPriority(status, priority, pageable);
        } else if (status != null) {
            srPage = srRepository.findByStatus(status, pageable);
        } else if (priority != null) {
            srPage = srRepository.findByPriority(priority, pageable);
        } else {
            srPage = srRepository.findAll(pageable);
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
                .build();

        Sr savedSr = srRepository.save(sr);
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

        Sr updatedSr = srRepository.save(sr);
        return SrResponse.from(updatedSr);
    }

    /**
     * SR 삭제
     * @param id SR ID
     */
    @Transactional
    public void deleteSr(Long id) {
        if (!srRepository.existsById(id)) {
            throw new CustomException("SR not found with id: " + id, HttpStatus.NOT_FOUND);
        }
        srRepository.deleteById(id);
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
            sr.setStatus(request.getStatus());
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
