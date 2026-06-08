package io.streak.habitflow.domain.label.api;

import io.streak.habitflow.domain.label.dto.request.LabelCreateRequest;
import io.streak.habitflow.domain.label.dto.request.LabelUpdateRequest;
import io.streak.habitflow.domain.label.dto.response.LabelListResponse;
import io.streak.habitflow.domain.label.dto.response.LabelResponse;
import io.streak.habitflow.domain.label.service.LabelService;
import io.streak.habitflow.domain.project.dto.response.ProjectResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/labels")
public class LabelController {
    private final LabelService labelService;

    @PostMapping
    public ResponseEntity<LabelResponse> createLabel(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody LabelCreateRequest labelCreateRequest) {
        return ResponseEntity.ok(labelService.createLabel(labelCreateRequest, userDetails));
    }

    @GetMapping
    public ResponseEntity<List<LabelListResponse>> getLabels(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(labelService.getLabels(userDetails));
    }

    @GetMapping("/{labelId}")
    public ResponseEntity<LabelResponse> getLabelById(@PathVariable Long labelId,
                                                      @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(labelService.getLabelById(labelId,userDetails));
    }

    @PutMapping("/{labelId}")
    public ResponseEntity<LabelResponse> updateLabel(@PathVariable Long labelId,
                                                     @AuthenticationPrincipal UserDetails userDetails,
                                                     @RequestBody LabelUpdateRequest labelUpdateRequest) {
        return ResponseEntity.ok(labelService.updateLabel(labelId, labelUpdateRequest, userDetails));
    }

    @DeleteMapping("/{labelId}")
    public ResponseEntity<Void> deleteLabel(@PathVariable Long labelId,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        labelService.deleteLabel(labelId, userDetails);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<LabelListResponse>> searchLabels(@RequestParam("keyword") String keyword) {
        return ResponseEntity.ok(labelService.searchLabels(keyword));
    }
}
