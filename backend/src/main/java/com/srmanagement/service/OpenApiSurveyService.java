package com.srmanagement.service;

import com.srmanagement.dto.request.OpenApiSurveyCreateRequest;
import com.srmanagement.dto.response.OpenApiSurveyResponse;
import com.srmanagement.entity.OpenApiSurvey;
import com.srmanagement.entity.Organization;
import com.srmanagement.exception.CustomException;
import com.srmanagement.repository.OpenApiSurveyRepository;
import com.srmanagement.repository.OrganizationRepository;
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

    @Autowired
    private OrganizationRepository organizationRepository;

    @Transactional(readOnly = true)
    public Page<OpenApiSurveyResponse> getSurveys(String keyword, Pageable pageable) {
        Page<OpenApiSurvey> page = openApiSurveyRepository.search(keyword, pageable);
        return page.map(this::convertToResponse);
    }

    @Transactional(readOnly = true)
    public OpenApiSurveyResponse getSurveyById(Long id) {
        OpenApiSurvey survey = openApiSurveyRepository.findById(id)
                .orElseThrow(() -> new CustomException("Survey not found with id: " + id, HttpStatus.NOT_FOUND));
        return convertToResponse(survey);
    }

    @Transactional
    public OpenApiSurveyResponse createSurvey(OpenApiSurveyCreateRequest request) {
        Organization organization = organizationRepository.findById(request.getOrganizationCode())
                .orElseThrow(() -> new CustomException("Organization not found with code: " + request.getOrganizationCode(), HttpStatus.NOT_FOUND));

        OpenApiSurvey survey = OpenApiSurvey.builder()
                .organization(organization)
                .department(request.getDepartment())
                .contactName(request.getContactName())
                .contactPhone(request.getContactPhone())
                .contactEmail(request.getContactEmail())
                .receivedFileName(request.getReceivedFileName())
                .receivedDate(request.getReceivedDate())
                .systemName(request.getSystemName())
                .currentMethod(request.getCurrentMethod())
                .desiredMethod(request.getDesiredMethod())
                .reasonForDistributed(request.getReasonForDistributed())
                .maintenanceOperation(request.getMaintenanceOperation())
                .maintenanceLocation(request.getMaintenanceLocation())
                .maintenanceAddress(request.getMaintenanceAddress())
                .maintenanceNote(request.getMaintenanceNote())
                .operationEnv(request.getOperationEnv())
                .serverLocation(request.getServerLocation())
                .webServerOs(request.getWebServerOs())
                .webServerOsType(request.getWebServerOsType())
                .webServerOsVersion(request.getWebServerOsVersion())
                .webServerType(request.getWebServerType())
                .webServerTypeOther(request.getWebServerTypeOther())
                .webServerVersion(request.getWebServerVersion())
                .wasServerOs(request.getWasServerOs())
                .wasServerOsType(request.getWasServerOsType())
                .wasServerOsVersion(request.getWasServerOsVersion())
                .wasServerType(request.getWasServerType())
                .wasServerTypeOther(request.getWasServerTypeOther())
                .wasServerVersion(request.getWasServerVersion())
                .dbServerOs(request.getDbServerOs())
                .dbServerOsType(request.getDbServerOsType())
                .dbServerOsVersion(request.getDbServerOsVersion())
                .dbServerType(request.getDbServerType())
                .dbServerTypeOther(request.getDbServerTypeOther())
                .dbServerVersion(request.getDbServerVersion())
                .devLanguage(request.getDevLanguage())
                .devLanguageOther(request.getDevLanguageOther())
                .devLanguageVersion(request.getDevLanguageVersion())
                .devFramework(request.getDevFramework())
                .devFrameworkOther(request.getDevFrameworkOther())
                .devFrameworkVersion(request.getDevFrameworkVersion())
                .otherRequests(request.getOtherRequests())
                .note(request.getNote())
                .build();

        OpenApiSurvey savedSurvey = openApiSurveyRepository.save(survey);
        return convertToResponse(savedSurvey);
    }

    @Transactional
    public OpenApiSurveyResponse updateSurvey(Long id, OpenApiSurveyCreateRequest request) {
        OpenApiSurvey survey = openApiSurveyRepository.findById(id)
                .orElseThrow(() -> new CustomException("Survey not found with id: " + id, HttpStatus.NOT_FOUND));

        if (!survey.getOrganization().getCode().equals(request.getOrganizationCode())) {
            Organization organization = organizationRepository.findById(request.getOrganizationCode())
                    .orElseThrow(() -> new CustomException("Organization not found with code: " + request.getOrganizationCode(), HttpStatus.NOT_FOUND));
            survey.setOrganization(organization);
        }

        survey.setDepartment(request.getDepartment());
        survey.setContactName(request.getContactName());
        survey.setContactPhone(request.getContactPhone());
        survey.setContactEmail(request.getContactEmail());
        survey.setReceivedFileName(request.getReceivedFileName());
        survey.setReceivedDate(request.getReceivedDate());
        survey.setSystemName(request.getSystemName());
        survey.setCurrentMethod(request.getCurrentMethod());
        survey.setDesiredMethod(request.getDesiredMethod());
        survey.setReasonForDistributed(request.getReasonForDistributed());
        survey.setMaintenanceOperation(request.getMaintenanceOperation());
        survey.setMaintenanceLocation(request.getMaintenanceLocation());
        survey.setMaintenanceAddress(request.getMaintenanceAddress());
        survey.setMaintenanceNote(request.getMaintenanceNote());
        survey.setOperationEnv(request.getOperationEnv());
        survey.setServerLocation(request.getServerLocation());
        survey.setWebServerOs(request.getWebServerOs());
        survey.setWebServerOsType(request.getWebServerOsType());
        survey.setWebServerOsVersion(request.getWebServerOsVersion());
        survey.setWebServerType(request.getWebServerType());
        survey.setWebServerTypeOther(request.getWebServerTypeOther());
        survey.setWebServerVersion(request.getWebServerVersion());
        survey.setWasServerOs(request.getWasServerOs());
        survey.setWasServerOsType(request.getWasServerOsType());
        survey.setWasServerOsVersion(request.getWasServerOsVersion());
        survey.setWasServerType(request.getWasServerType());
        survey.setWasServerTypeOther(request.getWasServerTypeOther());
        survey.setWasServerVersion(request.getWasServerVersion());
        survey.setDbServerOs(request.getDbServerOs());
        survey.setDbServerOsType(request.getDbServerOsType());
        survey.setDbServerOsVersion(request.getDbServerOsVersion());
        survey.setDbServerType(request.getDbServerType());
        survey.setDbServerTypeOther(request.getDbServerTypeOther());
        survey.setDbServerVersion(request.getDbServerVersion());
        survey.setDevLanguage(request.getDevLanguage());
        survey.setDevLanguageOther(request.getDevLanguageOther());
        survey.setDevLanguageVersion(request.getDevLanguageVersion());
        survey.setDevFramework(request.getDevFramework());
        survey.setDevFrameworkOther(request.getDevFrameworkOther());
        survey.setDevFrameworkVersion(request.getDevFrameworkVersion());
        survey.setOtherRequests(request.getOtherRequests());
        survey.setNote(request.getNote());

        return convertToResponse(survey);
    }

    private OpenApiSurveyResponse convertToResponse(OpenApiSurvey survey) {
        String organizationName = "Unknown Organization";
        String organizationCode = "";
        
        if (survey.getOrganization() != null) {
            organizationName = survey.getOrganization().getName();
            organizationCode = survey.getOrganization().getCode();
        }

        return OpenApiSurveyResponse.builder()
                .id(survey.getId())
                .organizationCode(organizationCode)
                .organizationName(organizationName)
                .department(survey.getDepartment())
                .contactName(survey.getContactName())
                .contactPhone(survey.getContactPhone())
                .contactEmail(survey.getContactEmail())
                .receivedFileName(survey.getReceivedFileName())
                .receivedDate(survey.getReceivedDate())
                .systemName(survey.getSystemName())
                .currentMethod(survey.getCurrentMethod())
                .desiredMethod(survey.getDesiredMethod())
                .reasonForDistributed(survey.getReasonForDistributed())
                .maintenanceOperation(survey.getMaintenanceOperation())
                .maintenanceLocation(survey.getMaintenanceLocation())
                .maintenanceAddress(survey.getMaintenanceAddress())
                .maintenanceNote(survey.getMaintenanceNote())
                .operationEnv(survey.getOperationEnv())
                .serverLocation(survey.getServerLocation())
                .webServerOs(survey.getWebServerOs())
                .webServerOsType(survey.getWebServerOsType())
                .webServerOsVersion(survey.getWebServerOsVersion())
                .webServerType(survey.getWebServerType())
                .webServerTypeOther(survey.getWebServerTypeOther())
                .webServerVersion(survey.getWebServerVersion())
                .wasServerOs(survey.getWasServerOs())
                .wasServerOsType(survey.getWasServerOsType())
                .wasServerOsVersion(survey.getWasServerOsVersion())
                .wasServerType(survey.getWasServerType())
                .wasServerTypeOther(survey.getWasServerTypeOther())
                .wasServerVersion(survey.getWasServerVersion())
                .dbServerOs(survey.getDbServerOs())
                .dbServerOsType(survey.getDbServerOsType())
                .dbServerOsVersion(survey.getDbServerOsVersion())
                .dbServerType(survey.getDbServerType())
                .dbServerTypeOther(survey.getDbServerTypeOther())
                .dbServerVersion(survey.getDbServerVersion())
                .devLanguage(survey.getDevLanguage())
                .devLanguageOther(survey.getDevLanguageOther())
                .devLanguageVersion(survey.getDevLanguageVersion())
                .devFramework(survey.getDevFramework())
                .devFrameworkOther(survey.getDevFrameworkOther())
                .devFrameworkVersion(survey.getDevFrameworkVersion())
                .otherRequests(survey.getOtherRequests())
                .note(survey.getNote())
                .createdAt(survey.getCreatedAt())
                .updatedAt(survey.getUpdatedAt())
                .build();
    }
}
