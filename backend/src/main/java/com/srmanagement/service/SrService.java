package com.srmanagement.service;

import com.srmanagement.dto.request.SrCreateRequest;
import com.srmanagement.dto.request.SrStatusUpdateRequest;
import com.srmanagement.dto.request.SrUpdateRequest;
import com.srmanagement.dto.response.SrResponse;
import com.srmanagement.entity.Priority;
import com.srmanagement.entity.Sr;
import com.srmanagement.entity.SrStatus;
import com.srmanagement.entity.User;
import com.srmanagement.exception.CustomException;
import com.srmanagement.repository.SrRepository;
import com.srmanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                .title(request.getTitle())
                .description(request.getDescription())
                .status(SrStatus.OPEN)
                .priority(request.getPriority() != null ? request.getPriority() : Priority.MEDIUM)
                .requester(requester)
                .assignee(assignee)
                .build();

        Sr savedSr = srRepository.save(sr);
        return SrResponse.from(savedSr);
    }

    /**
     * SR 수정
     * @param id SR ID
     * @param request SR 수정 요청 DTO
     * @return SrResponse
     */
    @Transactional
    public SrResponse updateSr(Long id, SrUpdateRequest request) {
        Sr sr = srRepository.findById(id)
                .orElseThrow(() -> new CustomException("SR not found with id: " + id, HttpStatus.NOT_FOUND));

        if (request.getTitle() != null) {
            sr.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            sr.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            sr.setStatus(request.getStatus());
        }
        if (request.getPriority() != null) {
            sr.setPriority(request.getPriority());
        }
        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new CustomException("Assignee not found", HttpStatus.NOT_FOUND));
            sr.setAssignee(assignee);
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
     * @return SrResponse
     */
    @Transactional
    public SrResponse updateSrStatus(Long id, SrStatusUpdateRequest request) {
        Sr sr = srRepository.findById(id)
                .orElseThrow(() -> new CustomException("SR not found with id: " + id, HttpStatus.NOT_FOUND));

        sr.setStatus(request.getStatus());
        Sr updatedSr = srRepository.save(sr);
        return SrResponse.from(updatedSr);
    }
}
