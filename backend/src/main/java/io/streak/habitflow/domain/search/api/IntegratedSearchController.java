package io.streak.habitflow.domain.search.api;

import io.streak.habitflow.domain.search.dto.response.IntegratedResponse;
import io.streak.habitflow.domain.search.service.IntegratedSearchService;
import io.streak.habitflow.global.aop.LoginMemberId;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
public class IntegratedSearchController {
    private final IntegratedSearchService integratedSearchService;

    @GetMapping
    @Operation(summary = "통합 검색")
    public ResponseEntity<IntegratedResponse.Search> searchAll(@RequestParam("keyword") String keyword,
                                                               @LoginMemberId Long loginMemberId,
                                                               @PageableDefault(size=5) Pageable pageable) {
        return ResponseEntity.ok(integratedSearchService.searchAll(keyword,loginMemberId,pageable));
    }
}
