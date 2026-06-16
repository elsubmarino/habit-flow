package io.streak.habitflow.domain.task.dto.request;

import io.streak.habitflow.domain.task.type.CursorDirection;
import io.streak.habitflow.domain.task.type.TaskFilterType;
import io.streak.habitflow.domain.task.type.TaskPriorityType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class TaskRequest {
    public record Create(
            @NotBlank(message = "제목은 필수 입력 항목입니다.")
            @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.")
            @Schema(description = "테스크 제목 (최대 100자)", requiredMode = Schema.RequiredMode.REQUIRED)
            String name,

            @Schema(description = "테스크 상세 설명")
            String description,

            @Schema(description = "마감 기한 일시")
            LocalDateTime dueDate,

            @Schema(description = "테스크 우선 순위 (P1~P4)")
            TaskPriorityType taskPriorityType,

            @Schema(description = "소속 프로젝트 ID (소속 없을 시에 null 넘기면 인박스로 진입)")
            Long projectId,

            @Schema(description = "부모 테스크 ID (하위 테스크 생성 시에 필수 채움)")
            Long parentId,

            @Schema(description = "매핑할 라벨 ID 리스트")
            List<Long> labelIds,

            @Schema(description = "반복 일졍 여부")
            boolean recurring,

            @Schema(description = "반복 규칙 (DAILY, WEEKLY, MONTHLY 등)")
            String recurrenceRule,

            @Schema(description = "반복 간격")
            Integer recurrenceInterval,

            @Schema(description = "반복 요일 (MON,TUE,WED) 등 문자열 파싱용)")
            String recurrenceDays,

            @Schema(description = "반복 요일 일자 (매월 몇 일)")
            Integer recurrenceDayOfMonth,

            @Schema(description = "시간 존재 여부")
            boolean hasTime
    ) {
        public Create{
            labelIds = (labelIds == null) ? new ArrayList<>() : new ArrayList<>(labelIds);
        }
    }

    public record SearchCondition(
            TaskFilterType taskFilterType,
            LocalDateTime fromDate,
            LocalDateTime toDate
    ){
        public SearchCondition(TaskFilterType taskFilterType){
            this(taskFilterType, null, null);
        }
    }

    public record UpdateDueDate(
            LocalDateTime dueDate,
            boolean recurring,
            String recurrenceRule,
            Integer recurrenceInterval,
            String recurrenceDays,
            Integer recurrenceDayOfMonth,
            boolean hasTime
    ){}

    public record UpdateLabel(
            List<Long> labelIds
    ){
        public UpdateLabel{
            labelIds = (labelIds == null) ? new ArrayList<>() : new ArrayList<>(labelIds);
        }
    }

    public record UpdatePriority(
            TaskPriorityType taskPriorityType
    ){}

    public record UpdateProject(
            Long projectId
    ){}

    public record Update(
            String name,
            String description
    ){}

    public record Cursor(
            LocalDateTime lastDueDate,
            TaskPriorityType lastPriorityType,
            Long lastSortOrder,
            Long lastTaskId,
            CursorDirection direction
    ){
        public Cursor{
            if(direction == null){
                direction =  CursorDirection.NEXT;
            }
        }

        public static Cursor next(LocalDateTime dueDate, TaskPriorityType priorityType, Long sortOrder, Long taskId){
            return new Cursor(dueDate,priorityType,sortOrder,taskId,CursorDirection.NEXT);
        }

        public static Cursor prev(LocalDateTime dueDate, TaskPriorityType priorityType, Long sortOrder, Long taskId){
            return new Cursor(dueDate,priorityType,sortOrder,taskId,CursorDirection.PREV);
        }
    }
}
