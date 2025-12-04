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
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<OpenApiSurveyResponse> page = openApiSurveyService.getSurveys(keyword, pageable);
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
}
