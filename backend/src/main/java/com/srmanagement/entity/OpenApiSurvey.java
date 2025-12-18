package com.srmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.srmanagement.converter.EncryptConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "open_api_survey")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenApiSurvey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "open_api_survey_seq_gen")
    @SequenceGenerator(name = "open_api_survey_seq_gen", sequenceName = "open_api_survey_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_code", referencedColumnName = "code", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(nullable = false, length = 100)
    @Convert(converter = EncryptConverter.class)
    private String contactName;

    @Column(length = 50)
    private String contactPosition;

    @Column(nullable = false, length = 100)
    @Convert(converter = EncryptConverter.class)
    private String contactPhone;

    @Column(nullable = false, length = 255)
    @Convert(converter = EncryptConverter.class)
    private String contactEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SurveyStatus status = SurveyStatus.PENDING;

    @Column(length = 255)
    private String receivedFileName;

    @Column(nullable = false)
    private LocalDate receivedDate;

    @Column(nullable = false, length = 100)
    private String systemName;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String operationStatus = "OPERATING";

    @Column(nullable = false, length = 20)
    private String currentMethod;

    @Column(nullable = false, length = 20)
    private String desiredMethod;

    @Column(columnDefinition = "TEXT")
    private String reasonForDistributed;

    @Column(nullable = false, length = 30)
    private String maintenanceOperation;

    @Column(nullable = false, length = 20)
    private String maintenanceLocation;

    @Column(length = 255)
    private String maintenanceAddress;

    @Column(columnDefinition = "TEXT")
    private String maintenanceNote;

    @Column(nullable = false, length = 20)
    private String operationEnv;

    @Column(length = 255)
    private String serverLocation;

    // WEB Server
    @Column(length = 20)
    private String webServerOs;
    @Column(length = 50)
    private String webServerOsType;
    @Column(length = 50)
    private String webServerOsVersion;
    @Column(length = 20)
    private String webServerType;
    @Column(length = 50)
    private String webServerTypeOther;
    @Column(length = 50)
    private String webServerVersion;

    // WAS Server
    @Column(length = 20)
    private String wasServerOs;
    @Column(length = 50)
    private String wasServerOsType;
    @Column(length = 50)
    private String wasServerOsVersion;
    @Column(length = 20)
    private String wasServerType;
    @Column(length = 50)
    private String wasServerTypeOther;
    @Column(length = 50)
    private String wasServerVersion;

    // DB Server
    @Column(length = 20)
    private String dbServerOs;
    @Column(length = 50)
    private String dbServerOsType;
    @Column(length = 50)
    private String dbServerOsVersion;
    @Column(length = 20)
    private String dbServerType;
    @Column(length = 50)
    private String dbServerTypeOther;
    @Column(length = 50)
    private String dbServerVersion;

    // Dev Environment
    @Column(length = 20)
    private String devLanguage;
    @Column(length = 50)
    private String devLanguageOther;
    @Column(length = 50)
    private String devLanguageVersion;
    @Column(length = 20)
    private String devFramework;
    @Column(length = 50)
    private String devFrameworkOther;
    @Column(length = 50)
    private String devFrameworkVersion;

    @Column(columnDefinition = "TEXT")
    private String otherRequests;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
