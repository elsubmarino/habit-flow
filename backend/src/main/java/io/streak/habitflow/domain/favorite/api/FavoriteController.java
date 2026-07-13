package io.streak.habitflow.domain.favorite.api;

import io.streak.habitflow.domain.favorite.dto.response.FavoriteResponse;
import io.streak.habitflow.domain.favorite.service.FavoriteService;
import io.streak.habitflow.global.web.LoginMemberId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/favorite")
@RequiredArgsConstructor
public class FavoriteController {
    private final FavoriteService favoriteService;

    @Operation(summary = "즐겨찾기 목록 조회")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "즐겨찾기 조회 성공")})
    @GetMapping
    public ResponseEntity<List<FavoriteResponse.Summary>> getFavorites(@LoginMemberId Long loginMemberId) {
        return ResponseEntity.ok(favoriteService.getFavorites(loginMemberId));
    }
}
