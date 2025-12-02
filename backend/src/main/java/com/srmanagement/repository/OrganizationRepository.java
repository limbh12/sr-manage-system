package com.srmanagement.repository;

import com.srmanagement.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, String> {
    
    /**
     * 기관명 또는 코드로 검색
     * @param name 기관명 검색어
     * @param code 코드 검색어
     * @return 검색된 기관 목록
     */
    List<Organization> findByNameContainingOrCodeContaining(String name, String code);
}
