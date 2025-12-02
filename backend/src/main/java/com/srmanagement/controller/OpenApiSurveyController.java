package com.srmanagement.controller;

import com.srmanagement.entity.OpenApiSurvey;
import com.srmanagement.service.OpenApiSurveyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/surveys")
public class OpenApiSurveyController {

    @Autowired
    private OpenApiSurveyService openApiSurveyService;

    @GetMapping
    public ResponseEntity<Page<OpenApiSurvey>> getSurveys(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<OpenApiSurvey> page = openApiSurveyService.getSurveys(keyword, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OpenApiSurvey> getSurveyById(@PathVariable Long id) {
        OpenApiSurvey survey = openApiSurveyService.getSurveyById(id);
        return ResponseEntity.ok(survey);
    }
}
