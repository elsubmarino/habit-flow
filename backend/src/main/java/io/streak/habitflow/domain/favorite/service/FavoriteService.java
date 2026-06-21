package io.streak.habitflow.domain.favorite.service;

import io.streak.habitflow.domain.favorite.dto.query.FavoriteSummaryQuery;
import io.streak.habitflow.domain.favorite.dto.response.FavoriteResponse;
import io.streak.habitflow.domain.favorite.repository.FavoriteRepository;
import io.streak.habitflow.global.util.HashidsProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final HashidsProvider hashidsProvider;

    public List<FavoriteResponse.Summary> getFavoriteListByMemberId(Long memberId){

        List<FavoriteSummaryQuery> favoriteListQueries = favoriteRepository.findByMemberId(memberId);
        return favoriteListQueries.stream()
                .map(query->{
                    String encodedTargetId = hashidsProvider.encode(query.getTargetId());
                    String encodeId = hashidsProvider.encode(query.getId());
                    return FavoriteResponse.Summary.to(query,encodedTargetId,encodeId);
                })
                .toList();
    }
}
