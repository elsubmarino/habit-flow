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

    /**
     * 라벨 생성
     *
     * @param userDetails 인증된 사용자 정보
     * @param labelRequest 라벨 요청 정보 DTO
     * @return 라벨 응답 정보 DTO
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
     * @param userDetails 인증된 사용자 정보
     * @return 라벨 응답 정보 DTO
     */
    @GetMapping
    public ResponseEntity<List<LabelResponse>> getLabels(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(labelService.getLabels(userDetails));
    }

    /**
     * 라벨 업데이트
     *
     * @param id 라벨 ID
     * @param userDetails 인증된 사용자 정보
     * @param labelRequest 라벨 요청 정보 DTO
     * @return 라벨 응답 정보 DTO
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
     * @param id 라벨 ID
     * @param userDetails 인증된 사용자 정보
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
     * @param keyword 검색키워드
     * @return 라벨 응답 정보 DTO
     */
    @GetMapping("/search")
    public ResponseEntity<List<LabelResponse>> searchLabels(@RequestParam("keyword") String keyword) {
        return ResponseEntity.ok(labelService.searchLabels(keyword));
    }
}
