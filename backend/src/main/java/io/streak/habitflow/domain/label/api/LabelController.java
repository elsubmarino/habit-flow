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
@RequestMapping("/api/label")
public class LabelController {
    private final LabelService labelService;

    /**
     * 라벨 생성
     *
     * @param userDetails
     * @param labelRequest
     * @return
     */
    @PostMapping
    public ResponseEntity<LabelResponse> createLabel(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody LabelRequest labelRequest) {
        return ResponseEntity.ok(labelService.createLabel(labelRequest, userDetails));
    }

    /**
     * 라벨 다건 조회
     *
     * @param userDetails
     * @param labelRequest
     * @return
     */
    @GetMapping
    public ResponseEntity<List<LabelResponse>> getLabels(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(labelService.getLabels(userDetails));
    }

    /**
     * 라벨 업데이트
     *
     * @param id
     * @param userDetails
     * @param labelRequest
     * @return
     */
    @PutMapping("/{id}")
    public ResponseEntity<LabelResponse> updateLabel(@PathVariable Long id,
                                                     @AuthenticationPrincipal UserDetails userDetails,
                                                     @RequestBody LabelRequest labelRequest) {
        return ResponseEntity.ok(labelService.updateLabel(id, labelRequest, userDetails));
    }

    /**
     * 라벨 삭제
     *
     * @param id
     * @param userDetails
     * @return
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLabel(@PathVariable Long id,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        labelService.deleteLabel(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 라벨 검색
     *
     * @param name
     * @return
     */
    @GetMapping("/search")
    public ResponseEntity<List<LabelResponse>> searchLabels(@RequestParam String keyword) {
        return ResponseEntity.ok(labelService.searchLabels(keyword));
    }
}
