package com.srmanagement.service;

import com.srmanagement.entity.Organization;
import com.srmanagement.repository.OrganizationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class OrganizationService {

    @Autowired
    private OrganizationRepository organizationRepository;

    /**
     * 기관 검색
     * @param keyword 검색어 (기관명 또는 코드)
     * @return 검색된 기관 목록
     */
    public List<Organization> searchOrganizations(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        return organizationRepository.findByNameContainingOrCodeContaining(keyword, keyword);
    }
}
