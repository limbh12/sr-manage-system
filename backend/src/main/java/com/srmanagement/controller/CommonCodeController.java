package com.srmanagement.controller;

import com.srmanagement.dto.CommonCodeDto;
import com.srmanagement.service.CommonCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/common-codes")
@RequiredArgsConstructor
public class CommonCodeController {

    private final CommonCodeService commonCodeService;

    @GetMapping("/groups")
    public ResponseEntity<List<String>> getCodeGroups() {
        return ResponseEntity.ok(commonCodeService.getCodeGroups());
    }

    @GetMapping("/{codeGroup}")
    public ResponseEntity<List<CommonCodeDto>> getCodesByGroup(@PathVariable String codeGroup) {
        return ResponseEntity.ok(commonCodeService.getCodesByGroup(codeGroup));
    }

    @GetMapping("/{codeGroup}/active")
    public ResponseEntity<List<CommonCodeDto>> getActiveCodesByGroup(@PathVariable String codeGroup) {
        return ResponseEntity.ok(commonCodeService.getActiveCodesByGroup(codeGroup));
    }

    @PostMapping
    public ResponseEntity<CommonCodeDto> createCode(@RequestBody CommonCodeDto dto) {
        return ResponseEntity.ok(commonCodeService.createCode(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonCodeDto> updateCode(@PathVariable Long id, @RequestBody CommonCodeDto dto) {
        return ResponseEntity.ok(commonCodeService.updateCode(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCode(@PathVariable Long id) {
        commonCodeService.deleteCode(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/reorder")
    public ResponseEntity<Void> reorderCodes(@RequestBody List<CommonCodeDto> codeList) {
        commonCodeService.reorderCodes(codeList);
        return ResponseEntity.ok().build();
    }
}
