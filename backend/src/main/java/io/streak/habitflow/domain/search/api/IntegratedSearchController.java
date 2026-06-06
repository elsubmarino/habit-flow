package io.streak.habitflow.domain.search.api;

import io.streak.habitflow.domain.search.dto.IntegratedSearchResponse;
import io.streak.habitflow.domain.search.service.IntegratedSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
public class IntegratedSearchController {
    private final IntegratedSearchService integratedSearchService;

    @GetMapping
    public ResponseEntity<IntegratedSearchResponse> searchIntegratedItems(@RequestParam String keyword,
                                                                                @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(integratedSearchService.searchAll(keyword,userDetails));
    }
}
