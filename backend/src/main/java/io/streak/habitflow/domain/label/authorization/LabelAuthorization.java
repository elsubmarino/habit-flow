package io.streak.habitflow.domain.label.authorization;

import io.streak.habitflow.domain.label.entity.Label;
import io.streak.habitflow.domain.label.repository.LabelRepository;
import io.streak.habitflow.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("labelAuthorization")
@RequiredArgsConstructor
public class LabelAuthorization {
    private final LabelRepository labelRepository;

    public boolean canAccess(UUID publicLabelId){
        Label label = labelRepository.getOrThrowByPublicId(publicLabelId);
        return label.getMember().getId().equals(SecurityUtils.currentMemberId());
    }
}
