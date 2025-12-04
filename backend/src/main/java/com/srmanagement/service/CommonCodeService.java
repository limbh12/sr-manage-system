package com.srmanagement.service;

import com.srmanagement.dto.CommonCodeDto;
import com.srmanagement.entity.CommonCode;
import com.srmanagement.exception.CustomException;
import com.srmanagement.repository.CommonCodeRepository;
import com.srmanagement.repository.SrRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommonCodeService {

    private final CommonCodeRepository commonCodeRepository;
    private final SrRepository srRepository;

    public List<CommonCodeDto> getCodesByGroup(String codeGroup) {
        return commonCodeRepository.findByCodeGroupOrderBySortOrderAsc(codeGroup).stream()
                .map(CommonCodeDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<CommonCodeDto> getActiveCodesByGroup(String codeGroup) {
        return commonCodeRepository.findByCodeGroupAndIsActiveTrueOrderBySortOrderAsc(codeGroup).stream()
                .map(CommonCodeDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<String> getCodeGroups() {
        return commonCodeRepository.findDistinctCodeGroups();
    }

    @Transactional
    public CommonCodeDto createCode(CommonCodeDto dto) {
        if (commonCodeRepository.existsByCodeGroupAndCodeValue(dto.getCodeGroup(), dto.getCodeValue())) {
            throw new CustomException("이미 존재하는 코드 값입니다.", HttpStatus.BAD_REQUEST);
        }

        CommonCode commonCode = CommonCode.builder()
                .codeGroup(dto.getCodeGroup())
                .codeValue(dto.getCodeValue())
                .codeName(dto.getCodeName())
                .sortOrder(dto.getSortOrder())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .description(dto.getDescription())
                .build();

        return CommonCodeDto.fromEntity(commonCodeRepository.save(commonCode));
    }

    @Transactional
    public CommonCodeDto updateCode(Long id, CommonCodeDto dto) {
        CommonCode commonCode = commonCodeRepository.findById(id)
                .orElseThrow(() -> new CustomException("코드를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        // 코드 값 변경 시 중복 체크 (본인 제외)
        if (!commonCode.getCodeValue().equals(dto.getCodeValue()) &&
                commonCodeRepository.existsByCodeGroupAndCodeValue(dto.getCodeGroup(), dto.getCodeValue())) {
            throw new CustomException("이미 존재하는 코드 값입니다.", HttpStatus.BAD_REQUEST);
        }

        commonCode.setCodeValue(dto.getCodeValue());
        commonCode.setCodeName(dto.getCodeName());
        commonCode.setSortOrder(dto.getSortOrder());
        commonCode.setIsActive(dto.getIsActive());
        commonCode.setDescription(dto.getDescription());

        return CommonCodeDto.fromEntity(commonCode);
    }

    @Transactional
    public void deleteCode(Long id) {
        CommonCode commonCode = commonCodeRepository.findById(id)
                .orElseThrow(() -> new CustomException("코드를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        // 사용 여부 체크
        if ("SR_CATEGORY".equals(commonCode.getCodeGroup())) {
            if (srRepository.countByCategory(commonCode.getCodeValue()) > 0) {
                throw new CustomException("해당 분류로 등록된 SR이 있어 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST);
            }
        } else if ("SR_REQUEST_TYPE".equals(commonCode.getCodeGroup())) {
            if (srRepository.countByRequestType(commonCode.getCodeValue()) > 0) {
                throw new CustomException("해당 요청구분으로 등록된 SR이 있어 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST);
            }
        }

        commonCodeRepository.delete(commonCode);
    }

    @Transactional
    public void reorderCodes(List<CommonCodeDto> codeList) {
        for (CommonCodeDto dto : codeList) {
            CommonCode commonCode = commonCodeRepository.findById(dto.getId())
                    .orElseThrow(() -> new CustomException("코드를 찾을 수 없습니다. ID: " + dto.getId(), HttpStatus.NOT_FOUND));
            commonCode.setSortOrder(dto.getSortOrder());
        }
    }
}
