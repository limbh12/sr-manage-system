package com.srmanagement.service;

import com.srmanagement.entity.OpenApiSurvey;
import com.srmanagement.exception.CustomException;
import com.srmanagement.repository.OpenApiSurveyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpenApiSurveyService {

    @Autowired
    private OpenApiSurveyRepository openApiSurveyRepository;

    @Transactional(readOnly = true)
    public Page<OpenApiSurvey> getSurveys(String keyword, Pageable pageable) {
        return openApiSurveyRepository.search(keyword, pageable);
    }

    @Transactional(readOnly = true)
    public OpenApiSurvey getSurveyById(Long id) {
        return openApiSurveyRepository.findById(id)
                .orElseThrow(() -> new CustomException("Survey not found with id: " + id, HttpStatus.NOT_FOUND));
    }
}
