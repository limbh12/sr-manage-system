package com.srmanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 행정기관 엔티티
 * 
 * 행정표준코드 및 기관명 정보를 저장합니다.
 */
@Entity
@Table(name = "organizations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Organization {

    /** 행정표준코드 (PK) */
    @Id
    @Column(length = 20)
    private String code;

    /** 기관명 */
    @Column(nullable = false, length = 100)
    private String name;
}
