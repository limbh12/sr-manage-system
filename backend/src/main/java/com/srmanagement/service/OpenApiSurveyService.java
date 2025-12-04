package com.srmanagement.service;

import com.srmanagement.dto.request.OpenApiSurveyCreateRequest;
import com.srmanagement.dto.response.BulkUploadResult;
import com.srmanagement.dto.response.OpenApiSurveyResponse;
import com.srmanagement.dto.response.OrganizationResponse;
import com.srmanagement.entity.OpenApiSurvey;
import com.srmanagement.entity.Organization;
import com.srmanagement.exception.CustomException;
import com.srmanagement.repository.OpenApiSurveyRepository;
import com.srmanagement.repository.OrganizationRepository;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

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

    @Transactional
    public BulkUploadResult bulkCreateSurveys(MultipartFile file) {
        List<BulkUploadResult.FailureDetail> failures = new ArrayList<>();
        int successCount = 0;
        int totalCount = 0;

        try (CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String[] headers = csvReader.readNext(); // Skip header
            // Validate headers if necessary, but for now assuming template format

            String[] record;
            int rowNumber = 1; // Header is 0, first data row is 1 (or 2 in Excel view)

            while ((record = csvReader.readNext()) != null) {
                rowNumber++;
                totalCount++;

                // Basic validation: Check if row is empty
                if (record.length == 0 || (record.length == 1 && record[0].trim().isEmpty())) {
                    continue;
                }

                try {
                    // Map CSV columns to Request
                    // Assuming CSV order matches the template provided
                    // 기관명,부서,담당자명,연락처,이메일,수신파일명,수신일자,시스템명,현행방식,희망방식,분산형희망사유,유지관리운영,유지관리장소,유지관리주소,유지관리비고,운영환경,서버위치,WEB서버OS,WEB서버OS종류,WEB서버OS버전,WEB서버종류,WEB서버종류기타,WEB서버버전,WAS서버OS,WAS서버OS종류,WAS서버OS버전,WAS서버종류,WAS서버종류기타,WAS서버버전,DB서버OS,DB서버OS종류,DB서버OS버전,DB서버종류,DB서버종류기타,DB서버버전,개발언어,개발언어기타,개발언어버전,개발프레임워크,개발프레임워크기타,개발프레임워크버전,기타요청사항,비고

                    String orgName = record[0];
                    if (orgName == null || orgName.trim().isEmpty()) {
                        throw new IllegalArgumentException("기관명은 필수입니다.");
                    }

                    Organization organization = organizationRepository.findByName(orgName.trim())
                            .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 기관명입니다: " + orgName));

                    OpenApiSurveyCreateRequest request = OpenApiSurveyCreateRequest.builder()
                            .organizationCode(organization.getCode())
                            .department(getValue(record, 1))
                            .contactName(getValue(record, 2))
                            .contactPhone(getValue(record, 3))
                            .contactEmail(getValue(record, 4))
                            .receivedFileName(getValue(record, 5))
                            .receivedDate(parseDate(getValue(record, 6)))
                            .systemName(getValue(record, 7))
                            .currentMethod(getValue(record, 8))
                            .desiredMethod(getValue(record, 9))
                            .reasonForDistributed(getValue(record, 10))
                            .maintenanceOperation(getValue(record, 11))
                            .maintenanceLocation(getValue(record, 12))
                            .maintenanceAddress(getValue(record, 13))
                            .maintenanceNote(getValue(record, 14))
                            .operationEnv(getValue(record, 15))
                            .serverLocation(getValue(record, 16))
                            // WEB
                            .webServerOs(getValue(record, 17))
                            .webServerOsType(getValue(record, 18))
                            .webServerOsVersion(getValue(record, 19))
                            .webServerType(getValue(record, 20))
                            .webServerTypeOther(getValue(record, 21))
                            .webServerVersion(getValue(record, 22))
                            // WAS
                            .wasServerOs(getValue(record, 23))
                            .wasServerOsType(getValue(record, 24))
                            .wasServerOsVersion(getValue(record, 25))
                            .wasServerType(getValue(record, 26))
                            .wasServerTypeOther(getValue(record, 27))
                            .wasServerVersion(getValue(record, 28))
                            // DB
                            .dbServerOs(getValue(record, 29))
                            .dbServerOsType(getValue(record, 30))
                            .dbServerOsVersion(getValue(record, 31))
                            .dbServerType(getValue(record, 32))
                            .dbServerTypeOther(getValue(record, 33))
                            .dbServerVersion(getValue(record, 34))
                            // Dev
                            .devLanguage(getValue(record, 35))
                            .devLanguageOther(getValue(record, 36))
                            .devLanguageVersion(getValue(record, 37))
                            .devFramework(getValue(record, 38))
                            .devFrameworkOther(getValue(record, 39))
                            .devFrameworkVersion(getValue(record, 40))
                            // Others
                            .otherRequests(getValue(record, 41))
                            .note(getValue(record, 42))
                            .build();

                    createSurvey(request);
                    successCount++;

                } catch (Exception e) {
                    failures.add(BulkUploadResult.FailureDetail.builder()
                            .rowNumber(rowNumber)
                            .reason(e.getMessage())
                            .data(record.length > 0 ? record[0] : "Empty Row")
                            .build());
                }
            }

        } catch (IOException | CsvValidationException e) {
            throw new RuntimeException("CSV 파일 처리 중 오류가 발생했습니다: " + e.getMessage());
        }

        return BulkUploadResult.builder()
                .totalCount(totalCount)
                .successCount(successCount)
                .failureCount(failures.size())
                .failures(failures)
                .build();
    }

    private String getValue(String[] record, int index) {
        if (index >= record.length) return null;
        String val = record[index];
        return (val == null || val.trim().isEmpty()) ? null : val.trim();
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null) return LocalDate.now();
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE); // yyyy-MM-dd
        } catch (DateTimeParseException e) {
            // Try other formats or default to now?
            // For now, let's assume yyyy-MM-dd or fail
            throw new IllegalArgumentException("날짜 형식이 올바르지 않습니다 (yyyy-MM-dd): " + dateStr);
        }
    }

    private OpenApiSurveyResponse convertToResponse(OpenApiSurvey survey) {
        return OpenApiSurveyResponse.builder()
                .id(survey.getId())
                .organization(OrganizationResponse.builder()
                        .code(survey.getOrganization().getCode())
                        .name(survey.getOrganization().getName())
                        .build())
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
