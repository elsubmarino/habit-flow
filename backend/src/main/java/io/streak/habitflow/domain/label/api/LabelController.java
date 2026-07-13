package io.streak.habitflow.domain.label.api;

import io.streak.habitflow.domain.label.dto.request.LabelRequest;
import io.streak.habitflow.domain.label.dto.response.LabelResponse;
import io.streak.habitflow.domain.label.service.LabelService;
import io.streak.habitflow.global.aop.LoginMemberId;
import io.streak.habitflow.global.common.RoutingId;
import io.streak.habitflow.global.common.constant.PageSizeConstants;
import io.streak.habitflow.global.util.HashidsProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
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
    private final HashidsProvider hashidsProvider;

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
            @RequestParam(value = "lastLabelId",required = false) RoutingId lastLabelId,
            @PageableDefault(size= PageSizeConstants.CURSOR_PAGING_NORMAL) Pageable pageable) {
        Long realLastLabelId = (lastLabelId != null) ? lastLabelId.value() : null;
        return ResponseEntity.ok(labelService.getLabels(realLastLabelId,loginMemberId,pageable));
    }

    @GetMapping("/{labelId}")
    @Operation(summary = "라벨 상세 조회")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "라벨 상세 조회 성공")})
    public ResponseEntity<LabelResponse.Detail> getLabelById(@PathVariable RoutingId labelId,
                                                      @LoginMemberId Long loginMemberId) {
        long realLabelId = labelId.value();
        return ResponseEntity.ok(labelService.getLabelById(realLabelId,loginMemberId));
    }

    @PutMapping("/{labelId}")
    @Operation(summary = "라벨 업데이트")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "라벨 업데이트 성공")})
    public ResponseEntity<LabelResponse.Detail> updateLabel(@PathVariable RoutingId labelId,
                                             @LoginMemberId Long loginMemberId,
                                             @RequestBody LabelRequest.Update request) {
        long realLabelId = labelId.value();
        return ResponseEntity.ok(labelService.updateLabel(realLabelId, request, loginMemberId));
    }

    @DeleteMapping("/{labelId}")
    @Operation(summary = "라벨 삭제")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "라벨 삭제 성공")})
    public ResponseEntity<Void> deleteLabel(@PathVariable RoutingId labelId,
                                            @LoginMemberId Long loginMemberId) {
        long realLabelId = labelId.value();
        labelService.deleteLabel(realLabelId, loginMemberId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{labelId}/sort-order")
    @Operation(summary = "정렬순서 변경")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "정렬순서 변경 성공")})
    public ResponseEntity<LabelResponse.Summary> updateSortOrder(@PathVariable RoutingId labelId,
                                                                @RequestBody @Valid LabelRequest.UpdateSortOrder request,
                                                                @LoginMemberId Long loginMemberId) {
        long realLabelId = labelId.value();
        return ResponseEntity.ok(labelService.updateSortOrder(realLabelId, request, loginMemberId));
    }
}
