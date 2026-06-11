package io.streak.habitflow.domain.favorite.service;

import io.streak.habitflow.domain.favorite.dto.response.FavoriteListResponse;
import io.streak.habitflow.domain.favorite.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {
    private final FavoriteRepository favoriteRepository;

    public List<FavoriteListResponse> getFavoriteListByMemberId(Long memberId){

        List<FavoriteListResponse> favoriteListResponses = favoriteRepository.findByMemberId(memberId);
        return favoriteListResponses;
    }
}
