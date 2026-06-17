package io.streak.habitflow.domain.favorite.service;

import io.streak.habitflow.domain.favorite.dto.query.FavoriteSummaryQuery;
import io.streak.habitflow.domain.favorite.dto.response.FavoriteResponse;
import io.streak.habitflow.domain.favorite.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {
    private final FavoriteRepository favoriteRepository;

    public List<FavoriteResponse.Summary> getFavoriteListByMemberId(Long memberId){

        List<FavoriteSummaryQuery> favoriteListQueries = favoriteRepository.findByMemberId(memberId);
        return favoriteListQueries.stream()
                .map(FavoriteResponse.Summary::from)
                .toList();
    }
}
