package com.srmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenApiSurveyResponse {
    private Long id;
    private OrganizationResponse organization;
    private String department;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String receivedFileName;
    private LocalDate receivedDate;
    private String systemName;
    private String currentMethod;
    private String desiredMethod;
    private String reasonForDistributed;
    private String maintenanceOperation;
    private String maintenanceLocation;
    private String maintenanceAddress;
    private String maintenanceNote;
    private String operationEnv;
    private String serverLocation;
    
    // WEB Server
    private String webServerOs;
    private String webServerOsType;
    private String webServerOsVersion;
    private String webServerType;
    private String webServerTypeOther;
    private String webServerVersion;

    // WAS Server
    private String wasServerOs;
    private String wasServerOsType;
    private String wasServerOsVersion;
    private String wasServerType;
    private String wasServerTypeOther;
    private String wasServerVersion;

    // DB Server
    private String dbServerOs;
    private String dbServerOsType;
    private String dbServerOsVersion;
    private String dbServerType;
    private String dbServerTypeOther;
    private String dbServerVersion;

    // Dev Environment
    private String devLanguage;
    private String devLanguageOther;
    private String devLanguageVersion;
    private String devFramework;
    private String devFrameworkOther;
    private String devFrameworkVersion;

    private String otherRequests;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
