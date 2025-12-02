package com.srmanagement.controller;

import com.srmanagement.entity.Organization;
import com.srmanagement.service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 행정기관 컨트롤러
 * 
 * 행정기관 검색 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    @Autowired
    private OrganizationService organizationService;

    /**
     * 기관 검색
     * @param keyword 검색어
     * @return 기관 목록
     */
    @GetMapping
    public ResponseEntity<List<Organization>> searchOrganizations(@RequestParam String keyword) {
        List<Organization> organizations = organizationService.searchOrganizations(keyword);
        return ResponseEntity.ok(organizations);
    }
}
