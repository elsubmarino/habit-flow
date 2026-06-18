package io.streak.habitflow.domain.activitylog.dto.response;

import io.streak.habitflow.domain.activitylog.dto.ChangeSet;
import io.streak.habitflow.domain.task.type.ActivityType;
import io.streak.habitflow.domain.task.type.TargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public final class ActivityLogResponse {
    @Builder
    public record Summary(
            @Schema(description = "액티비티 로그 ID")
            Long id,
            @Schema(description = """
                    액티비티 유형 (ADDED,
                        COMPLETED,
                        UPDATED,
                        DELETED,
                        MOVED,
                        INVITED,
                        UNCOMPLETED,
                        JOINED)""")
            ActivityType activityType,

            @Schema(description = "액트비티 로그를 발동한 자의 정보 (ID,이름으로 구성)")
            ActorInfo actor,

            @Schema(description = """
                해당 액티비티의 대상자 정보 (타겟대상,ID,이름으로 구성)
                프로젝트 초대시에 사용됨
            """)
            TargetInfo target,
            @Schema(description = "생성일")
            LocalDateTime createdAt,
            @Schema(description = """
                    변경 세트
                    field: 대상필드,
                    from:변경이전정보
                    to:변경이후정보
                    """)
            List<ChangeSet> changes
    ){}

    @Builder
    public record ActorInfo(
       @Schema(description = "행위자의 member id")
       Long id,
       @Schema(description = "행위자의 이름")
       String name
    ){}

    @Builder
    public record TargetInfo(
       @Schema(description = """
               대상의 유형
               PROJECT,TASK,COMMENT
               """)
       TargetType type,
       @Schema(description = "대상자의 member id")
       Long id,
       @Schema(description = "대상자의 이름")
       String name
    ){}
}
