package com.srmanagement.controller;

import com.srmanagement.dto.request.OpenApiSurveyCreateRequest;
import com.srmanagement.dto.response.OpenApiSurveyResponse;
import com.srmanagement.dto.response.BulkUploadResult;
import com.srmanagement.service.OpenApiSurveyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/surveys")
public class OpenApiSurveyController {

    @Autowired
    private OpenApiSurveyService openApiSurveyService;

    @GetMapping
    public ResponseEntity<Page<OpenApiSurveyResponse>> getSurveys(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String currentMethod,
            @RequestParam(required = false) String desiredMethod,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<OpenApiSurveyResponse> page = openApiSurveyService.getSurveys(keyword, currentMethod, desiredMethod, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OpenApiSurveyResponse> getSurveyById(@PathVariable Long id) {
        OpenApiSurveyResponse survey = openApiSurveyService.getSurveyById(id);
        return ResponseEntity.ok(survey);
    }

    @PostMapping
    public ResponseEntity<OpenApiSurveyResponse> createSurvey(@Valid @RequestBody OpenApiSurveyCreateRequest request) {
        OpenApiSurveyResponse survey = openApiSurveyService.createSurvey(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(survey);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OpenApiSurveyResponse> updateSurvey(@PathVariable Long id, @Valid @RequestBody OpenApiSurveyCreateRequest request) {
        OpenApiSurveyResponse survey = openApiSurveyService.updateSurvey(id, request);
        return ResponseEntity.ok(survey);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkUploadResult> uploadSurveyCsv(@RequestParam("file") MultipartFile file) {
        BulkUploadResult result = openApiSurveyService.bulkCreateSurveys(file);
        return ResponseEntity.ok(result);
    }

    // CSV 템플릿 다운로드
    @GetMapping("/template")
    public ResponseEntity<org.springframework.core.io.Resource> downloadTemplate() {
        try {
            org.springframework.core.io.ClassPathResource resource =
                new org.springframework.core.io.ClassPathResource("static/templates/openapi_survey_template.csv");

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"openapi_survey_template.csv\"")
                    .contentType(org.springframework.http.MediaType.parseMediaType("text/csv"))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 단일 설문에 첨부된 수신파일 업로드
    @PostMapping(value = "{id}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadReceivedFile(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        openApiSurveyService.storeReceivedFile(id, file);
        return ResponseEntity.ok().build();
    }

    // 단일 설문에 첨부된 수신파일 다운로드
    @GetMapping("/{id}/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadReceivedFile(@PathVariable Long id) {
        org.springframework.core.io.Resource resource = openApiSurveyService.loadReceivedFileAsResource(id);

        String fileName = openApiSurveyService.getReceivedFileName(id);

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
