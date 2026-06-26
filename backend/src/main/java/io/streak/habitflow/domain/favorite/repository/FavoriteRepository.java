package io.streak.habitflow.domain.favorite.repository;

import io.streak.habitflow.domain.favorite.entity.Favorite;
import io.streak.habitflow.global.common.type.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long>, FavoriteRepositoryCustom {
    Optional<Favorite> findByMemberIdAndTargetTypeAndTargetId(Long memberId, TargetType targetType, Long targetId);
    void deleteByTargetTypeAndTargetId(TargetType targetType, Long targetId);
    void deleteByMemberIdAndTargetTypeAndTargetId(Long memberId, TargetType targetType, Long targetId);
}
