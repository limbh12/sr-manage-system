package com.srmanagement.dto;

import com.srmanagement.entity.CommonCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommonCodeDto {
    private Long id;
    private String codeGroup;
    private String codeValue;
    private String codeName;
    private Integer sortOrder;
    private Boolean isActive;
    private String description;

    public static CommonCodeDto fromEntity(CommonCode entity) {
        return CommonCodeDto.builder()
                .id(entity.getId())
                .codeGroup(entity.getCodeGroup())
                .codeValue(entity.getCodeValue())
                .codeName(entity.getCodeName())
                .sortOrder(entity.getSortOrder())
                .isActive(entity.getIsActive())
                .description(entity.getDescription())
                .build();
    }
}
