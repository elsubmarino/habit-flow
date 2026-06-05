package io.streak.habitflow.domain.label.api;

import io.streak.habitflow.domain.label.dto.LabelRequest;
import io.streak.habitflow.domain.label.dto.LabelResponse;
import io.streak.habitflow.domain.label.service.LabelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
     * @param userDetails
     * @param labelRequest
     * @return
     */
    @PostMapping
    public ResponseEntity<LabelResponse> createLabel(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody LabelRequest labelRequest){
        LabelResponse response = labelService.createLabel(labelRequest,userDetails);
        return ResponseEntity.ok(response);
    }

    /**
     * 라벨 다건 조회
     * @param userDetails
     * @param labelRequest
     * @return
     */
    @GetMapping
    public ResponseEntity<List<LabelResponse>> getLabels(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody LabelRequest labelRequest){
        List<LabelResponse> labelResponses = labelService.getLabels(labelRequest,userDetails);
        return ResponseEntity.ok(labelResponses);
    }

    /**
     * 라벨 업데이트
     * @param id
     * @param userDetails
     * @param labelRequest
     * @return
     */
    @PutMapping("/{id}")
    public ResponseEntity<LabelResponse> updateLabel(@PathVariable Long id,
                                                     @AuthenticationPrincipal UserDetails userDetails,
                                                     @RequestBody LabelRequest labelRequest){
        LabelResponse labelResponse = labelService.updateLabel(id,labelRequest,userDetails);
        return ResponseEntity.ok(labelResponse);
    }

    /**
     * 라벨 삭제
     * @param id
     * @param userDetails
     * @return
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLabel(@PathVariable Long id,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        labelService.deleteLabel(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
