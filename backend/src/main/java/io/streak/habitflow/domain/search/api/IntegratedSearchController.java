package io.streak.habitflow.domain.search.api;

import io.streak.habitflow.domain.search.dto.response.IntegratedSearchResponse;
import io.streak.habitflow.domain.search.service.IntegratedSearchService;
import io.streak.habitflow.global.security.dto.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<IntegratedSearchResponse> searchIntegratedItems(@RequestParam("keyword") String keyword,
                                                                          @AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                          @PageableDefault(size=5) Pageable pageable) {
        return ResponseEntity.ok(integratedSearchService.searchAll(keyword,userPrincipal.getMemberId(),pageable));
    }
}
