package io.streak.habitflow.domain.label.api;

import io.streak.habitflow.domain.label.dto.request.LabelCreateRequest;
import io.streak.habitflow.domain.label.dto.request.LabelUpdateRequest;
import io.streak.habitflow.domain.label.dto.response.LabelListResponse;
import io.streak.habitflow.domain.label.dto.response.LabelResponse;
import io.streak.habitflow.domain.label.service.LabelService;
import io.streak.habitflow.global.common.dto.ScrollResponse;
import io.streak.habitflow.global.security.dto.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/labels")
public class LabelController {
    private final LabelService labelService;

    @PostMapping
    public ResponseEntity<LabelResponse> createLabel(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody LabelCreateRequest labelCreateRequest) {
        return ResponseEntity.ok(labelService.createLabel(labelCreateRequest, userPrincipal.getMemberId()));
    }

    @GetMapping
    public ResponseEntity<ScrollResponse<LabelListResponse>> getLabels(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(value = "lastLabelId",required = false) Long lastLabelId,
            @PageableDefault(size=20) Pageable pageable) {
        return ResponseEntity.ok(labelService.getLabels(lastLabelId,userPrincipal.getMemberId(),pageable));
    }

    @GetMapping("/{labelId}")
    public ResponseEntity<LabelResponse> getLabelById(@PathVariable Long labelId,
                                                      @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(labelService.getLabelById(labelId,userPrincipal.getMemberId()));
    }

    @PutMapping("/{labelId}")
    public ResponseEntity<LabelResponse> updateLabel(@PathVariable Long labelId,
                                                     @AuthenticationPrincipal UserPrincipal userPrincipal,
                                                     @RequestBody LabelUpdateRequest labelUpdateRequest) {
        return ResponseEntity.ok(labelService.updateLabel(labelId, labelUpdateRequest, userPrincipal.getMemberId()));
    }

    @DeleteMapping("/{labelId}")
    public ResponseEntity<Void> deleteLabel(@PathVariable Long labelId,
                                            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        labelService.deleteLabel(labelId, userPrincipal.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<LabelListResponse>> searchLabels(@RequestParam("keyword") String keyword) {
        return ResponseEntity.ok(labelService.searchLabels(keyword));
    }
}
