package io.streak.habitflow.domain.label.repository;

import io.streak.habitflow.domain.label.entity.Label;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LabelRepository extends JpaRepository<Label, Long>, LabelRepositoryCustom {
    List<Label> findByMemberId(Long memberId);
    List<Label> findByNameContaining(String name);

    default Label getOrThrow(Long labelId){
        return this.findById(labelId)
                .orElseThrow(()->new IllegalArgumentException("존재하지 않는 라벨입니다."));
    }
}
