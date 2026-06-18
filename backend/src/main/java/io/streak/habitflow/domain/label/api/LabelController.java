package io.streak.habitflow.domain.label.api;

import io.streak.habitflow.domain.label.dto.request.LabelRequest;
import io.streak.habitflow.domain.label.dto.response.LabelResponse;
import io.streak.habitflow.domain.label.service.LabelService;
import io.streak.habitflow.global.aop.LoginMemberId;
import io.streak.habitflow.global.common.constant.PageSizeConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/labels")
public class LabelController {
    private final LabelService labelService;

    @PostMapping
    @Operation(summary = "라벨 생성")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "라벨 생성 성공")})
    public ResponseEntity<LabelResponse.Detail> createLabel(
            @LoginMemberId Long loginMemberId,
            @RequestBody LabelRequest.Create request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(labelService.createLabel(request, loginMemberId));
    }

    @GetMapping
    @Operation(summary = "라벨 다건 조회")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "라벨 다건 조회 성공")})
    public ResponseEntity<Slice<LabelResponse.Summary>> getLabels(
            @LoginMemberId Long loginMemberId,
            @RequestParam(value = "lastLabelId",required = false) Long lastLabelId,
            @PageableDefault(size= PageSizeConstants.CURSOR_PAGING_NORMAL) Pageable pageable) {
        return ResponseEntity.ok(labelService.getLabels(lastLabelId,loginMemberId,pageable));
    }

    @GetMapping("/{labelId}")
    @Operation(summary = "라벨 상세 조회")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "라벨 상세 조회 성공")})
    public ResponseEntity<LabelResponse.Detail> getLabelById(@PathVariable Long labelId,
                                                      @LoginMemberId Long loginMemberId) {
        return ResponseEntity.ok(labelService.getLabelById(labelId,loginMemberId));
    }

    @PutMapping("/{labelId}")
    @Operation(summary = "라벨 업데이트")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "라벨 업데이트 성공")})
    public ResponseEntity<Void> updateLabel(@PathVariable Long labelId,
                                                     @LoginMemberId Long loginMemberId,
                                                     @RequestBody LabelRequest.Update request) {
        labelService.updateLabel(labelId, request, loginMemberId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{labelId}")
    @Operation(summary = "라벨 삭제")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "라벨 삭제 성공")})
    public ResponseEntity<Void> deleteLabel(@PathVariable Long labelId,
                                            @LoginMemberId Long loginMemberId) {
        labelService.deleteLabel(labelId, loginMemberId);
        return ResponseEntity.noContent().build();
    }
}
