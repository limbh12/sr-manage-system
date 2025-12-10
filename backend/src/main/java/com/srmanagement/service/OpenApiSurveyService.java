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
import com.srmanagement.repository.UserRepository;
import com.srmanagement.entity.User;
import com.srmanagement.dto.response.UserResponse;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.srmanagement.util.CryptoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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

    @Autowired
    private UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<OpenApiSurveyResponse> getSurveys(String keyword, Pageable pageable) {
        // keyword: LIKE 검색용 (평문 유지)
        // exactKeyword: 일치 검색용 (JPA Converter에 의해 암호화됨)
        // 동일한 파라미터 이름을 사용하면 JPA가 컨텍스트에 따라 암호화 여부를 혼동할 수 있어 분리함
        Page<OpenApiSurvey> page = openApiSurveyRepository.search(keyword, keyword, pageable);
        return page.map(this::convertToResponse);
    }

    // 단일 설문에 첨부된 수신파일 저장
    @Transactional
    public void storeReceivedFile(Long id, org.springframework.web.multipart.MultipartFile file) {
        try {
            OpenApiSurvey survey = openApiSurveyRepository.findById(id)
                    .orElseThrow(() -> new CustomException("Survey not found with id: " + id, org.springframework.http.HttpStatus.NOT_FOUND));

            // Use project backend directory under repo to ensure predictable location
            Path projectRoot = Paths.get(System.getProperty("user.dir"));
            Path uploadDir = projectRoot.resolve("data").resolve("uploads");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            String origName = file.getOriginalFilename();
            if (origName == null || origName.trim().isEmpty()) {
                origName = "file";
            }
            // sanitize original name: remove path separators and control chars
            String sanitized = origName.trim().replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("[\\u0000-\\u001F\\r\\n\\t]", "");

            // store as survey_{id}__{sanitizedOriginal}
            String storedName = "survey_" + id + "__" + sanitized;
            Path target = uploadDir.resolve(storedName);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }

            // keep original (sanitized) name in DB for Content-Disposition
            survey.setReceivedFileName(sanitized);
            openApiSurveyRepository.save(survey);
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    // 단일 설문 첨부파일을 Resource로 반환
    @Transactional(readOnly = true)
    public org.springframework.core.io.Resource loadReceivedFileAsResource(Long id) {
        OpenApiSurvey survey = openApiSurveyRepository.findById(id)
                .orElseThrow(() -> new CustomException("Survey not found with id: " + id, org.springframework.http.HttpStatus.NOT_FOUND));

        Path projectRoot = Paths.get(System.getProperty("user.dir"));
        Path uploadDir = projectRoot.resolve("data").resolve("uploads");
        if (!Files.exists(uploadDir) || !Files.isDirectory(uploadDir)) {
            throw new CustomException("첨부된 파일이 존재하지 않습니다.", org.springframework.http.HttpStatus.NOT_FOUND);
        }

        String prefix = "survey_" + id + "__";
        try {
            try (java.util.stream.Stream<Path> stream = Files.list(uploadDir)) {
                Path found = stream.filter(p -> p.getFileName().toString().startsWith(prefix)).findFirst().orElse(null);
                if (found == null) {
                    throw new CustomException("첨부된 파일이 존재하지 않습니다.", org.springframework.http.HttpStatus.NOT_FOUND);
                }
                org.springframework.core.io.UrlResource resource = new org.springframework.core.io.UrlResource(found.toUri());
                if (!resource.exists() || !resource.isReadable()) {
                    throw new CustomException("파일을 읽을 수 없습니다.", org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
                }
                return resource;
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("파일을 로드하는 동안 오류가 발생했습니다: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("업로드 디렉터리를 읽는 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public String getReceivedFileName(Long id) {
        OpenApiSurvey survey = openApiSurveyRepository.findById(id)
                .orElseThrow(() -> new CustomException("Survey not found with id: " + id, org.springframework.http.HttpStatus.NOT_FOUND));
        return survey.getReceivedFileName() == null ? "file" : survey.getReceivedFileName();
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

        User assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new CustomException("User not found with id: " + request.getAssigneeId(), HttpStatus.NOT_FOUND));
        }

        OpenApiSurvey survey = OpenApiSurvey.builder()
                .organization(organization)
                .department(request.getDepartment())
                .contactName(request.getContactName())
                .contactPosition(request.getContactPosition())
                .contactPhone(request.getContactPhone())
                .contactEmail(request.getContactEmail())
                .assignee(assignee)
                .status(request.getStatus() != null ? request.getStatus() : com.srmanagement.entity.SurveyStatus.PENDING)
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
                            .department(getValueOrDefault(record, 1, "미입력"))
                            .contactName(getValueOrDefault(record, 2, "미입력"))
                            .contactPhone(getValueOrDefault(record, 3, "미입력"))
                            .contactEmail(getValueOrDefault(record, 4, "미입력"))
                            .receivedFileName(getValue(record, 5))
                            .receivedDate(parseDate(getValue(record, 6)))
                            .systemName(getValueOrDefault(record, 7, "미입력"))
                            .currentMethod(getValueOrDefault(record, 8, "NO_RESPONSE"))
                            .desiredMethod(getValueOrDefault(record, 9, "NO_RESPONSE"))
                            .reasonForDistributed(getValue(record, 10))
                            .maintenanceOperation(getValueOrDefault(record, 11, "NO_RESPONSE"))
                            .maintenanceLocation(getValueOrDefault(record, 12, "NO_RESPONSE"))
                            .maintenanceAddress(getValue(record, 13))
                            .maintenanceNote(getValue(record, 14))
                            .operationEnv(getValueOrDefault(record, 15, "NO_RESPONSE"))
                            .serverLocation(getValueOrDefault(record, 16, "NO_RESPONSE"))
                            // WEB
                            .webServerOs(getValueOrDefault(record, 17, "NO_RESPONSE"))
                            .webServerOsType(getValue(record, 18))
                            .webServerOsVersion(getValue(record, 19))
                            .webServerType(getValueOrDefault(record, 20, "NO_RESPONSE"))
                            .webServerTypeOther(getValue(record, 21))
                            .webServerVersion(getValue(record, 22))
                            // WAS
                            .wasServerOs(getValueOrDefault(record, 23, "NO_RESPONSE"))
                            .wasServerOsType(getValue(record, 24))
                            .wasServerOsVersion(getValue(record, 25))
                            .wasServerType(getValueOrDefault(record, 26, "NO_RESPONSE"))
                            .wasServerTypeOther(getValue(record, 27))
                            .wasServerVersion(getValue(record, 28))
                            // DB
                            .dbServerOs(getValueOrDefault(record, 29, "NO_RESPONSE"))
                            .dbServerOsType(getValue(record, 30))
                            .dbServerOsVersion(getValue(record, 31))
                            .dbServerType(getValueOrDefault(record, 32, "NO_RESPONSE"))
                            .dbServerTypeOther(getValue(record, 33))
                            .dbServerVersion(getValue(record, 34))
                            // Dev
                            .devLanguage(getValueOrDefault(record, 35, "NO_RESPONSE"))
                            .devLanguageOther(getValue(record, 36))
                            .devLanguageVersion(getValue(record, 37))
                            .devFramework(getValueOrDefault(record, 38, "NO_RESPONSE"))
                            .devFrameworkOther(getValue(record, 39))
                            .devFrameworkVersion(getValue(record, 40))
                            // Others
                            .otherRequests(getValue(record, 41))
                            .note(getValue(record, 42))
                            .build();

                    validateRequest(request);
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

    private String getValueOrDefault(String[] record, int index, String defaultValue) {
        String val = getValue(record, index);
        return val == null ? defaultValue : val;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null) return LocalDate.now();
        String formattedDate = dateStr.replace(".", "-");
        try {
            return LocalDate.parse(formattedDate, DateTimeFormatter.ISO_DATE); // yyyy-MM-dd
        } catch (DateTimeParseException e) {
            // Try other formats or default to now?
            // For now, let's assume yyyy-MM-dd or fail
            throw new IllegalArgumentException("날짜 형식이 올바르지 않습니다 (yyyy-MM-dd 또는 yyyy.MM.dd): " + dateStr);
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
                .contactPosition(survey.getContactPosition())
                .contactPhone(survey.getContactPhone())
                .contactEmail(survey.getContactEmail())
                .assignee(survey.getAssignee() != null ? UserResponse.from(survey.getAssignee()) : null)
                .status(survey.getStatus())
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

    private void validateRequest(OpenApiSurveyCreateRequest request) {
        validateLength("부서", request.getDepartment(), 100);
        validateLength("담당자명", request.getContactName(), 50);
        validateLength("연락처", request.getContactPhone(), 30);
        validateLength("이메일", request.getContactEmail(), 100);
        validateLength("수신파일명", request.getReceivedFileName(), 255);
        validateLength("시스템명", request.getSystemName(), 100);
        validateLength("현행방식", request.getCurrentMethod(), 20);
        validateLength("희망방식", request.getDesiredMethod(), 20);
        validateLength("유지관리운영", request.getMaintenanceOperation(), 30);
        validateLength("유지관리장소", request.getMaintenanceLocation(), 20);
        validateLength("유지관리주소", request.getMaintenanceAddress(), 255);
        validateLength("운영환경", request.getOperationEnv(), 20);
        validateLength("서버위치", request.getServerLocation(), 255);

        validateLength("WEB서버OS", request.getWebServerOs(), 20);
        validateLength("WEB서버OS종류", request.getWebServerOsType(), 50);
        validateLength("WEB서버OS버전", request.getWebServerOsVersion(), 50);
        validateLength("WEB서버종류", request.getWebServerType(), 20);
        validateLength("WEB서버종류기타", request.getWebServerTypeOther(), 50);
        validateLength("WEB서버버전", request.getWebServerVersion(), 50);

        validateLength("WAS서버OS", request.getWasServerOs(), 20);
        validateLength("WAS서버OS종류", request.getWasServerOsType(), 50);
        validateLength("WAS서버OS버전", request.getWasServerOsVersion(), 50);
        validateLength("WAS서버종류", request.getWasServerType(), 20);
        validateLength("WAS서버종류기타", request.getWasServerTypeOther(), 50);
        validateLength("WAS서버버전", request.getWasServerVersion(), 50);

        validateLength("DB서버OS", request.getDbServerOs(), 20);
        validateLength("DB서버OS종류", request.getDbServerOsType(), 50);
        validateLength("DB서버OS버전", request.getDbServerOsVersion(), 50);
        validateLength("DB서버종류", request.getDbServerType(), 20);
        validateLength("DB서버종류기타", request.getDbServerTypeOther(), 50);
        validateLength("DB서버버전", request.getDbServerVersion(), 50);

        validateLength("개발언어", request.getDevLanguage(), 20);
        validateLength("개발언어기타", request.getDevLanguageOther(), 50);
        validateLength("개발언어버전", request.getDevLanguageVersion(), 50);
        validateLength("개발프레임워크", request.getDevFramework(), 20);
        validateLength("개발프레임워크기타", request.getDevFrameworkOther(), 50);
        validateLength("개발프레임워크버전", request.getDevFrameworkVersion(), 50);
    }

    private void validateLength(String fieldName, String value, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(String.format("%s 항목의 길이가 %d자를 초과했습니다. (현재: %d자)", fieldName, maxLength, value.length()));
        }
    }
}
