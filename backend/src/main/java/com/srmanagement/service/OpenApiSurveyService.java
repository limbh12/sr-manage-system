package com.srmanagement.service;

import com.srmanagement.dto.request.OpenApiSurveyCreateRequest;
import com.srmanagement.dto.request.SrCreateRequest;
import com.srmanagement.dto.response.BulkUploadResult;
import com.srmanagement.dto.response.OpenApiSurveyResponse;
import com.srmanagement.dto.response.OrganizationResponse;
import com.srmanagement.dto.response.SrResponse;
import com.srmanagement.entity.OpenApiSurvey;
import com.srmanagement.entity.Organization;
import com.srmanagement.entity.Priority;
import com.srmanagement.entity.Sr;
import com.srmanagement.entity.SrStatus;
import com.srmanagement.exception.CustomException;
import com.srmanagement.repository.OpenApiSurveyRepository;
import com.srmanagement.repository.OrganizationRepository;
import com.srmanagement.repository.SrRepository;
import com.srmanagement.repository.UserRepository;
import com.srmanagement.entity.User;
import com.srmanagement.dto.response.UserResponse;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.srmanagement.util.CryptoUtil;
import com.srmanagement.wiki.service.ContentEmbeddingService;
import com.srmanagement.wiki.service.WikiNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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

    @Autowired
    private SrRepository srRepository;

    @Autowired
    private SrService srService;

    @Autowired
    private WikiNotificationService notificationService;

    @Autowired(required = false)
    private ContentEmbeddingService contentEmbeddingService;

    @Transactional(readOnly = true)
    public Page<OpenApiSurveyResponse> getSurveys(String keyword, String currentMethod, String desiredMethod, Pageable pageable) {
        // keyword: LIKE 검색용 (평문 유지)
        // exactKeyword: 일치 검색용 (JPA Converter에 의해 암호화됨)
        // 동일한 파라미터 이름을 사용하면 JPA가 컨텍스트에 따라 암호화 여부를 혼동할 수 있어 분리함

        // 빈 문자열을 null로 변환
        String normalizedCurrentMethod = (currentMethod != null && !currentMethod.trim().isEmpty()) ? currentMethod : null;
        String normalizedDesiredMethod = (desiredMethod != null && !desiredMethod.trim().isEmpty()) ? desiredMethod : null;

        Page<OpenApiSurvey> page = openApiSurveyRepository.search(keyword, keyword, normalizedCurrentMethod, normalizedDesiredMethod, pageable);
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
        return createSurvey(request, true);
    }

    /**
     * 현황조사 생성 (임베딩 생성 여부 선택)
     * @param request 생성 요청
     * @param generateEmbedding 임베딩 생성 여부 (일괄 등록 시 false)
     */
    @Transactional
    public OpenApiSurveyResponse createSurvey(OpenApiSurveyCreateRequest request, boolean generateEmbedding) {
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
                .operationStatus(request.getOperationStatus() != null ? request.getOperationStatus() : "OPERATING")
                .currentMethod(normalizeCurrentMethod(request.getCurrentMethod()))
                .desiredMethod(normalizeDesiredMethod(request.getDesiredMethod()))
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

        // SR 자동 생성 (generateEmbedding 플래그에 따라 SR 임베딩도 생성 여부 결정)
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            createSrForNewSurvey(savedSurvey, currentUser, generateEmbedding);

            // 알림 발송 (모든 사용자에게)
            notificationService.notifySurveyCreated(
                    savedSurvey.getId(),
                    savedSurvey.getOrganization().getName(),
                    savedSurvey.getSystemName(),
                    currentUser
            );
        }

        // 현황조사 임베딩 비동기 생성 (AI 검색용) - 트랜잭션 커밋 후 실행
        // 일괄 등록 시에는 generateEmbedding=false로 호출하여 임베딩 생성 생략
        // (나중에 관리자 패널에서 일괄 생성)
        if (generateEmbedding && contentEmbeddingService != null) {
            final Long surveyId = savedSurvey.getId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    contentEmbeddingService.generateSurveyEmbeddingAsync(surveyId);
                }
            });
        }

        return convertToResponse(savedSurvey);
    }

    @Transactional
    public OpenApiSurveyResponse updateSurvey(Long id, OpenApiSurveyCreateRequest request) {
        OpenApiSurvey survey = openApiSurveyRepository.findById(id)
                .orElseThrow(() -> new CustomException("Survey not found with id: " + id, HttpStatus.NOT_FOUND));

        // 이전 상태 복사 (SR 생성용)
        OpenApiSurvey oldSurvey = copyForComparison(survey);

        if (!survey.getOrganization().getCode().equals(request.getOrganizationCode())) {
            Organization organization = organizationRepository.findById(request.getOrganizationCode())
                    .orElseThrow(() -> new CustomException("Organization not found with code: " + request.getOrganizationCode(), HttpStatus.NOT_FOUND));
            survey.setOrganization(organization);
        }

        // 담당자 업데이트
        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new CustomException("User not found with id: " + request.getAssigneeId(), HttpStatus.NOT_FOUND));
            survey.setAssignee(assignee);
        } else {
            survey.setAssignee(null);
        }

        survey.setDepartment(request.getDepartment());
        survey.setContactName(request.getContactName());
        survey.setContactPhone(request.getContactPhone());
        survey.setContactEmail(request.getContactEmail());
        survey.setReceivedFileName(request.getReceivedFileName());
        survey.setReceivedDate(request.getReceivedDate());
        survey.setSystemName(request.getSystemName());
        survey.setOperationStatus(request.getOperationStatus() != null ? request.getOperationStatus() : "OPERATING");
        survey.setCurrentMethod(normalizeCurrentMethod(request.getCurrentMethod()));
        survey.setDesiredMethod(normalizeDesiredMethod(request.getDesiredMethod()));
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

        // SR 자동 생성
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            createSrForUpdatedSurvey(oldSurvey, survey, currentUser);

            // 알림 발송 (모든 사용자에게)
            notificationService.notifySurveyUpdated(
                    survey.getId(),
                    survey.getOrganization().getName(),
                    survey.getSystemName(),
                    currentUser
            );
        }

        // 현황조사 임베딩 비동기 재생성 (AI 검색용) - 트랜잭션 커밋 후 실행
        if (contentEmbeddingService != null) {
            final Long surveyId = survey.getId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    contentEmbeddingService.generateSurveyEmbeddingAsync(surveyId);
                }
            });
        }

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
                            .operationStatus(getValueOrDefault(record, 8, "OPERATING"))
                            .currentMethod(normalizeCurrentMethod(getValue(record, 9)))
                            .desiredMethod(normalizeDesiredMethod(getValue(record, 10)))
                            .reasonForDistributed(getValue(record, 11))
                            .maintenanceOperation(getValueOrDefault(record, 12, "NO_RESPONSE"))
                            .maintenanceLocation(getValueOrDefault(record, 13, "NO_RESPONSE"))
                            .maintenanceAddress(getValue(record, 14))
                            .maintenanceNote(getValue(record, 15))
                            .operationEnv(getValueOrDefault(record, 16, "NO_RESPONSE"))
                            .serverLocation(getValueOrDefault(record, 17, "NO_RESPONSE"))
                            // WEB
                            .webServerOs(getValueOrDefault(record, 18, "NO_RESPONSE"))
                            .webServerOsType(getValue(record, 19))
                            .webServerOsVersion(getValue(record, 20))
                            .webServerType(getValueOrDefault(record, 21, "NO_RESPONSE"))
                            .webServerTypeOther(getValue(record, 22))
                            .webServerVersion(getValue(record, 23))
                            // WAS
                            .wasServerOs(getValueOrDefault(record, 24, "NO_RESPONSE"))
                            .wasServerOsType(getValue(record, 25))
                            .wasServerOsVersion(getValue(record, 26))
                            .wasServerType(getValueOrDefault(record, 27, "NO_RESPONSE"))
                            .wasServerTypeOther(getValue(record, 28))
                            .wasServerVersion(getValue(record, 29))
                            // DB
                            .dbServerOs(getValueOrDefault(record, 30, "NO_RESPONSE"))
                            .dbServerOsType(getValue(record, 31))
                            .dbServerOsVersion(getValue(record, 32))
                            .dbServerType(getValueOrDefault(record, 33, "NO_RESPONSE"))
                            .dbServerTypeOther(getValue(record, 34))
                            .dbServerVersion(getValue(record, 35))
                            // Dev
                            .devLanguage(getValueOrDefault(record, 36, "NO_RESPONSE"))
                            .devLanguageOther(getValue(record, 37))
                            .devLanguageVersion(getValue(record, 38))
                            .devFramework(getValueOrDefault(record, 39, "NO_RESPONSE"))
                            .devFrameworkOther(getValue(record, 40))
                            .devFrameworkVersion(getValue(record, 41))
                            // Others
                            .otherRequests(getValue(record, 42))
                            .note(getValue(record, 43))
                            .build();

                    validateRequest(request);
                    // 일괄 등록 시 임베딩 생성 생략 (나중에 관리자 패널에서 일괄 생성)
                    createSurvey(request, false);
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
                .operationStatus(survey.getOperationStatus())
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

    /**
     * 현재방식 한글 → 영문 코드 변환
     */
    private String normalizeCurrentMethod(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "NO_RESPONSE";
        }

        String normalized = value.trim();
        switch (normalized) {
            case "중앙":
            case "중앙형":
                return "CENTRAL";
            case "분산":
            case "분산형":
                return "DISTRIBUTED";
            case "미회신":
                return "NO_RESPONSE";
            default:
                // 이미 영문 코드인 경우 그대로 반환
                if (normalized.equals("CENTRAL") || normalized.equals("DISTRIBUTED") || normalized.equals("NO_RESPONSE")) {
                    return normalized;
                }
                return "NO_RESPONSE";
        }
    }

    /**
     * 희망전환방식 한글 → 영문 코드 변환
     */
    private String normalizeDesiredMethod(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "NO_RESPONSE";
        }

        String normalized = value.trim();
        switch (normalized) {
            case "중앙개선형":
            case "중앙 개선형":
                return "CENTRAL_IMPROVED";
            case "분산개선형":
            case "분산 개선형":
                return "DISTRIBUTED_IMPROVED";
            case "미회신":
                return "NO_RESPONSE";
            default:
                // 이미 영문 코드인 경우 그대로 반환
                if (normalized.equals("CENTRAL_IMPROVED") || normalized.equals("DISTRIBUTED_IMPROVED") || normalized.equals("NO_RESPONSE")) {
                    return normalized;
                }
                return "NO_RESPONSE";
        }
    }

    /**
     * 현재 로그인한 사용자 조회
     */
    private User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                String username = authentication.getName();
                return userRepository.findByUsername(username).orElse(null);
            }
        } catch (Exception e) {
            System.err.println("Failed to get current user: " + e.getMessage());
        }
        return null;
    }

    /**
     * 현황조사 데이터 복사 (비교용)
     */
    private OpenApiSurvey copyForComparison(OpenApiSurvey survey) {
        return OpenApiSurvey.builder()
                .id(survey.getId())
                .organization(survey.getOrganization())
                .department(survey.getDepartment())
                .contactName(survey.getContactName())
                .contactPhone(survey.getContactPhone())
                .contactEmail(survey.getContactEmail())
                .assignee(survey.getAssignee())
                .systemName(survey.getSystemName())
                .currentMethod(survey.getCurrentMethod())
                .desiredMethod(survey.getDesiredMethod())
                .build();
    }

    /**
     * 현황조사 생성 시 SR 자동 생성
     * @param survey 현황조사
     * @param requester 요청자
     * @param generateEmbedding SR 임베딩 생성 여부 (일괄 등록 시 false)
     */
    private void createSrForNewSurvey(OpenApiSurvey survey, User requester, boolean generateEmbedding) {
        try {
            // 1. SR 생성 (generateEmbedding 플래그 전달)
            SrCreateRequest srRequest = new SrCreateRequest();
            srRequest.setTitle(String.format("[OPEN API 현황조사] %s - %s",
                    survey.getOrganization().getName(), survey.getSystemName()));
            srRequest.setDescription(buildSurveyDescription(survey, true));
            srRequest.setPriority(Priority.MEDIUM);
            srRequest.setCategory("OPEN_API");
            srRequest.setRequestType("SURVEY");
            srRequest.setAssigneeId(survey.getAssignee() != null ? survey.getAssignee().getId() : null);
            srRequest.setOpenApiSurveyId(survey.getId());
            srRequest.setApplicantName(survey.getContactName());
            srRequest.setApplicantPhone(survey.getContactPhone());

            SrResponse createdSr = srService.createSr(srRequest, requester.getUsername(), generateEmbedding);

            // 2. SR을 CLOSED 상태로 변경하고 처리내용 설정
            Sr sr = srRepository.findById(createdSr.getId())
                    .orElseThrow(() -> new CustomException("SR not found", HttpStatus.NOT_FOUND));
            sr.setStatus(SrStatus.CLOSED);
            sr.setProcessingDetails("현황조사 정보가 신규 등록되었습니다.");
            srRepository.save(sr);
        } catch (Exception e) {
            // SR 생성 실패 시 로그만 남기고 현황조사 저장은 계속 진행
            System.err.println("Failed to create SR for survey: " + e.getMessage());
        }
    }

    /**
     * 현황조사 수정 시 SR 자동 생성
     */
    private void createSrForUpdatedSurvey(OpenApiSurvey oldSurvey, OpenApiSurvey newSurvey, User requester) {
        try {
            String changes = buildChangeDescription(oldSurvey, newSurvey);

            // 1. SR 생성
            SrCreateRequest srRequest = new SrCreateRequest();
            srRequest.setTitle(String.format("[OPEN API 현황조사 수정] %s - %s",
                    newSurvey.getOrganization().getName(), newSurvey.getSystemName()));
            srRequest.setDescription(buildSurveyDescription(newSurvey, false));
            srRequest.setPriority(Priority.MEDIUM);
            srRequest.setCategory("OPEN_API");
            srRequest.setRequestType("SURVEY");
            srRequest.setAssigneeId(newSurvey.getAssignee() != null ? newSurvey.getAssignee().getId() : null);
            srRequest.setOpenApiSurveyId(newSurvey.getId());
            srRequest.setApplicantName(newSurvey.getContactName());
            srRequest.setApplicantPhone(newSurvey.getContactPhone());

            SrResponse createdSr = srService.createSr(srRequest, requester.getUsername());

            // 2. SR을 CLOSED 상태로 변경하고 처리내용 설정 (변경사항 요약)
            Sr sr = srRepository.findById(createdSr.getId())
                    .orElseThrow(() -> new CustomException("SR not found", HttpStatus.NOT_FOUND));
            sr.setStatus(SrStatus.CLOSED);
            sr.setProcessingDetails(changes);
            srRepository.save(sr);
        } catch (Exception e) {
            // SR 생성 실패 시 로그만 남기고 현황조사 저장은 계속 진행
            System.err.println("Failed to create SR for updated survey: " + e.getMessage());
        }
    }

    /**
     * 현황조사 정보로 SR 요청사항 생성
     */
    private String buildSurveyDescription(OpenApiSurvey survey, boolean isNew) {
        StringBuilder sb = new StringBuilder();

        if (isNew) {
            sb.append("## 현황조사 정보 신규 등록\n\n");
        } else {
            sb.append("## 현황조사 정보 수정\n\n");
        }

        sb.append("### 기본 정보\n");
        sb.append("- **기관명**: ").append(survey.getOrganization().getName()).append("\n");
        sb.append("- **부서**: ").append(survey.getDepartment()).append("\n");
        sb.append("- **시스템명**: ").append(survey.getSystemName()).append("\n");
        sb.append("- **수신일자**: ").append(survey.getReceivedDate()).append("\n\n");

        sb.append("### API 시스템 현황\n");
        sb.append("- **현재방식**: ").append(getMethodLabel(survey.getCurrentMethod())).append("\n");
        sb.append("- **희망전환방식**: ").append(getMethodLabel(survey.getDesiredMethod())).append("\n");

        if (survey.getReasonForDistributed() != null && !survey.getReasonForDistributed().isEmpty()) {
            sb.append("- **분산형 희망사유**: ").append(survey.getReasonForDistributed()).append("\n");
        }
        sb.append("\n");

        sb.append("### 담당자 정보\n");
        sb.append("- **담당자명**: ").append(survey.getContactName()).append("\n");
        sb.append("- **연락처**: ").append(survey.getContactPhone()).append("\n");
        sb.append("- **이메일**: ").append(survey.getContactEmail()).append("\n");

        return sb.toString();
    }

    /**
     * 현황조사 변경사항 생성
     */
    private String buildChangeDescription(OpenApiSurvey oldSurvey, OpenApiSurvey newSurvey) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 현황조사 정보 수정 내역\n\n");

        // 기본 정보 변경
        if (!oldSurvey.getDepartment().equals(newSurvey.getDepartment())) {
            sb.append("- **부서**: ").append(oldSurvey.getDepartment())
              .append(" → ").append(newSurvey.getDepartment()).append("\n");
        }

        if (!oldSurvey.getSystemName().equals(newSurvey.getSystemName())) {
            sb.append("- **시스템명**: ").append(oldSurvey.getSystemName())
              .append(" → ").append(newSurvey.getSystemName()).append("\n");
        }

        if (!oldSurvey.getOperationStatus().equals(newSurvey.getOperationStatus())) {
            sb.append("- **운영상태**: ").append(getOperationStatusLabel(oldSurvey.getOperationStatus()))
              .append(" → ").append(getOperationStatusLabel(newSurvey.getOperationStatus())).append("\n");
        }

        // 전환방식 변경
        if (!oldSurvey.getCurrentMethod().equals(newSurvey.getCurrentMethod())) {
            sb.append("- **현재방식**: ").append(getMethodLabel(oldSurvey.getCurrentMethod()))
              .append(" → ").append(getMethodLabel(newSurvey.getCurrentMethod())).append("\n");
        }

        if (!oldSurvey.getDesiredMethod().equals(newSurvey.getDesiredMethod())) {
            sb.append("- **희망전환방식**: ").append(getMethodLabel(oldSurvey.getDesiredMethod()))
              .append(" → ").append(getMethodLabel(newSurvey.getDesiredMethod())).append("\n");
        }

        // 담당자 변경
        if (!oldSurvey.getContactName().equals(newSurvey.getContactName())) {
            sb.append("- **담당자명**: ").append(oldSurvey.getContactName())
              .append(" → ").append(newSurvey.getContactName()).append("\n");
        }

        if (!oldSurvey.getContactPhone().equals(newSurvey.getContactPhone())) {
            sb.append("- **연락처**: ").append(oldSurvey.getContactPhone())
              .append(" → ").append(newSurvey.getContactPhone()).append("\n");
        }

        // 담당자(assignee) 변경
        String oldAssignee = oldSurvey.getAssignee() != null ? oldSurvey.getAssignee().getName() : "미지정";
        String newAssignee = newSurvey.getAssignee() != null ? newSurvey.getAssignee().getName() : "미지정";
        if (!oldAssignee.equals(newAssignee)) {
            sb.append("- **담당자(처리)**: ").append(oldAssignee)
              .append(" → ").append(newAssignee).append("\n");
        }

        if (sb.toString().equals("## 현황조사 정보 수정 내역\n\n")) {
            sb.append("변경사항 없음\n");
        }

        return sb.toString();
    }

    /**
     * 전환방식 코드를 한글 레이블로 변환
     */
    private String getMethodLabel(String methodCode) {
        if (methodCode == null) return "미회신";

        switch (methodCode) {
            case "CENTRAL":
                return "중앙형";
            case "DISTRIBUTED":
                return "분산형";
            case "CENTRAL_IMPROVED":
                return "중앙개선형";
            case "DISTRIBUTED_IMPROVED":
                return "분산개선형";
            case "NO_RESPONSE":
                return "미회신";
            default:
                return methodCode;
        }
    }

    /**
     * 운영상태 코드를 한글 레이블로 변환
     */
    private String getOperationStatusLabel(String statusCode) {
        if (statusCode == null) return "운영중";

        switch (statusCode) {
            case "OPERATING":
                return "운영중";
            case "DEPRECATED":
                return "폐기";
            case "SCHEDULED_DEPRECATION":
                return "폐기예정";
            default:
                return statusCode;
        }
    }
}
