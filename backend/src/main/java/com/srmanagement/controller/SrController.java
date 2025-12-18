package com.srmanagement.controller;

import com.srmanagement.dto.request.SrCreateRequest;
import com.srmanagement.dto.request.SrHistoryCreateRequest;
import com.srmanagement.dto.request.SrStatusUpdateRequest;
import com.srmanagement.dto.request.SrUpdateRequest;
import com.srmanagement.dto.response.SrHistoryResponse;
import com.srmanagement.dto.response.SrResponse;
import com.srmanagement.entity.Priority;
import com.srmanagement.entity.Role;
import com.srmanagement.entity.SrStatus;
import com.srmanagement.entity.User;
import com.srmanagement.exception.CustomException;
import com.srmanagement.repository.UserRepository;
import com.srmanagement.service.SrService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SR 컨트롤러
 * 
 * SR CRUD 및 상태 변경 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/sr")
public class SrController {

    @Autowired
    private SrService srService;

    @Autowired
    private UserRepository userRepository;

    /**
     * SR 목록 조회
     * @param status 상태 필터
     * @param priority 우선순위 필터
     * @param category 분류 필터
     * @param requestType 요청구분 필터
     * @param assigneeId 담당자 ID 필터
     * @param search 검색어
     * @param includeDeleted 삭제된 항목 포함 여부 (관리자만 사용 가능)
     * @param pageable 페이지네이션
     * @param authentication 현재 인증 정보
     * @return Page<SrResponse>
     */
    @GetMapping
    public ResponseEntity<Page<SrResponse>> getSrList(
            @RequestParam(required = false) SrStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String requestType,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean includeDeleted,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {

        // 관리자가 아닌 경우 삭제된 항목 포함 불가
        Boolean showDeleted = false;
        if (includeDeleted != null && includeDeleted) {
            User user = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));
            if (user.getRole() == Role.ADMIN) {
                showDeleted = true;
            }
        }

        Page<SrResponse> response = srService.getSrList(status, priority, category, requestType, assigneeId, search, showDeleted, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * SR 상세 조회
     * @param id SR ID
     * @return SrResponse
     */
    @GetMapping("/{id}")
    public ResponseEntity<SrResponse> getSrById(@PathVariable Long id) {
        SrResponse response = srService.getSrById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * SR 생성
     * @param request SR 생성 요청
     * @param authentication 현재 인증 정보
     * @return SrResponse
     */
    @PostMapping
    public ResponseEntity<SrResponse> createSr(
            @Valid @RequestBody SrCreateRequest request,
            Authentication authentication) {
        SrResponse response = srService.createSr(request, authentication.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * SR 수정
     * @param id SR ID
     * @param request SR 수정 요청
     * @param authentication 현재 인증 정보
     * @return SrResponse
     */
    @PutMapping("/{id}")
    public ResponseEntity<SrResponse> updateSr(
            @PathVariable Long id,
            @Valid @RequestBody SrUpdateRequest request,
            Authentication authentication) {
        SrResponse response = srService.updateSr(id, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * SR 삭제
     * @param id SR ID
     * @param authentication 현재 인증 정보
     * @return ResponseEntity
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSr(@PathVariable Long id, Authentication authentication) {
        srService.deleteSr(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    /**
     * SR 복구 (소프트 삭제 취소) - 관리자 전용
     * @param id SR ID
     * @param authentication 현재 인증 정보
     * @return SrResponse
     */
    @PostMapping("/{id}/restore")
    public ResponseEntity<SrResponse> restoreSr(@PathVariable Long id, Authentication authentication) {
        SrResponse response = srService.restoreSr(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * SR 상태 변경
     * @param id SR ID
     * @param request 상태 변경 요청
     * @param authentication 현재 인증 정보
     * @return SrResponse
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<SrResponse> updateSrStatus(
            @PathVariable Long id,
            @Valid @RequestBody SrStatusUpdateRequest request,
            Authentication authentication) {
        SrResponse response = srService.updateSrStatus(id, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * SR 이력 목록 조회
     * @param id SR ID
     * @return List<SrHistoryResponse>
     */
    @GetMapping("/{id}/histories")
    public ResponseEntity<List<SrHistoryResponse>> getSrHistories(@PathVariable Long id) {
        List<SrHistoryResponse> response = srService.getSrHistories(id);
        return ResponseEntity.ok(response);
    }

    /**
     * SR 이력(댓글) 생성
     * @param id SR ID
     * @param request 이력 생성 요청
     * @param authentication 현재 인증 정보
     * @return SrHistoryResponse
     */
    @PostMapping("/{id}/histories")
    public ResponseEntity<SrHistoryResponse> createSrHistory(
            @PathVariable Long id,
            @Valid @RequestBody SrHistoryCreateRequest request,
            Authentication authentication) {
        SrHistoryResponse response = srService.createSrHistory(id, request, authentication.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
