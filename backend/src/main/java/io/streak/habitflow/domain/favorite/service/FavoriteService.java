package io.streak.habitflow.domain.favorite.service;

import io.streak.habitflow.domain.favorite.dto.query.FavoriteListQuery;
import io.streak.habitflow.domain.favorite.dto.response.FavoriteResponse;
import io.streak.habitflow.domain.favorite.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {
    private final FavoriteRepository favoriteRepository;

    public List<FavoriteResponse.List> getFavoriteListByMemberId(Long memberId){

        List<FavoriteListQuery> favoriteListQueries = favoriteRepository.findByMemberId(memberId);
        return favoriteListQueries.stream()
                .map(FavoriteResponse.List::from)
                .toList();
    }
}
