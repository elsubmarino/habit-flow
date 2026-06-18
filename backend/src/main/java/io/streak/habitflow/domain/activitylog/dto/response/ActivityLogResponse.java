package io.streak.habitflow.domain.activitylog.dto.response;

import io.streak.habitflow.domain.activitylog.dto.ChangeSet;
import io.streak.habitflow.domain.task.type.ActivityType;
import io.streak.habitflow.domain.task.type.TargetType;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public final class ActivityLogResponse {
    @Builder
    public record Summary(
            Long id,
            ActivityType activityType,
            ActorInfo actor,
            TargetInfo target,
            LocalDateTime createdAt,
            List<ChangeSet> changes
    ){}

    @Builder
    public record ActorInfo(
       Long id,
       String name
    ){}

    @Builder
    public record TargetInfo(
       TargetType type,
       Long id,
       String name
    ){}
}
