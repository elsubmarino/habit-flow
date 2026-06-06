package io.streak.habitflow.domain.label.api;

import io.streak.habitflow.domain.label.dto.LabelRequest;
import io.streak.habitflow.domain.label.dto.LabelResponse;
import io.streak.habitflow.domain.label.service.LabelService;
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
            @RequestBody LabelRequest labelRequest) {
        return ResponseEntity.ok(labelService.createLabel(labelRequest, userDetails));
    }

    @GetMapping
    public ResponseEntity<List<LabelResponse>> getLabels(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(labelService.getLabels(userDetails));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LabelResponse> updateLabel(@PathVariable Long id,
                                                     @AuthenticationPrincipal UserDetails userDetails,
                                                     @RequestBody LabelRequest labelRequest) {
        return ResponseEntity.ok(labelService.updateLabel(id, labelRequest, userDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLabel(@PathVariable Long id,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        labelService.deleteLabel(id, userDetails);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<LabelResponse>> searchLabels(@RequestParam("keyword") String keyword) {
        return ResponseEntity.ok(labelService.searchLabels(keyword));
    }
}
