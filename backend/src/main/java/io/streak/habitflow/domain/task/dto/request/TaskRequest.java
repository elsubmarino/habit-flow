package io.streak.habitflow.domain.task.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.streak.habitflow.domain.task.type.CursorDirection;
import io.streak.habitflow.domain.task.type.TaskFilterType;
import io.streak.habitflow.domain.task.type.TaskPriorityType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TaskRequest {
    public record Create(
            @NotBlank(message = "제목은 필수 입력 항목입니다.")
            @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.")
            @Schema(description = "테스크 제목 (최대 100자)", requiredMode = Schema.RequiredMode.REQUIRED)
            String name,

            @Size(max=2000,message = "설명은 2000자를 초과할 수 없습니다.")
            @Schema(description = "테스크 상세 설명 (최대 2000자)")
            String description,

            @JsonFormat(shape = JsonFormat.Shape.STRING,pattern = "yyyy-MM-dd'T'HH:mm:ss")
            @Schema(description = "마감 기한 일시 (yyyy-MM-dd'T'HH:mm:ss)", pattern = "yyyy-MM-dd'T'HH:mm:ss",example = "2026-06-26T23:59:59")
            LocalDateTime dueDate,

            @NotNull
            @Schema(description = "테스크 우선 순위 (P1~P4)",examples = {"P1","P2","P3","P4"})
            TaskPriorityType taskPriorityType,

            @Schema(description = "소속 프로젝트 ID (소속 없을 시에 null 넘기면 인박스로 진입)")
            String projectId,

            @Schema(description = "부모 테스크 ID (하위 테스크 생성 시에 필수 채움)")
            String parentId,

            @Size(max=10,message = "라벨은 최대 10개까지만 매핑할 수 있습니다.")
            @Schema(description = "매핑할 라벨 ID 리스트 (최대 10개)")
            List<String> labelIds,

            @Schema(description = "반복 일졍 여부")
            boolean recurring,

            @Schema(description = "반복 규칙 (DAILY, WEEKLY, MONTHLY 등)",examples = {"DAILY","WEEKLY","MONTHLY"})
            String recurrenceRule,

            @Schema(description = "반복 간격")
            Integer recurrenceInterval,

            @Schema(description = "반복 요일 (MON,TUE,WED) 등 문자열 파싱용)",examples = {"MON","TUE","WED"})
            String recurrenceDays,

            @Schema(description = "반복 요일 일자 (매월 몇 일)",example = "13")
            Integer recurrenceDayOfMonth,

            @Schema(description = "시간 존재 여부")
            boolean timeSpecified
    ) {
        public Create{
            labelIds = (labelIds == null) ? new ArrayList<>() : new ArrayList<>(labelIds);

            if(recurring && (recurrenceRule == null || recurrenceRule.isBlank())) {
                throw new IllegalArgumentException("반복 일정을 설정할 경우 반복 규칙이 필수입니다.");
            }
        }
    }

    public record SearchCondition(
            @NotNull
            TaskFilterType taskFilterType,

            @JsonFormat(shape = JsonFormat.Shape.STRING,pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime fromDate,

            @JsonFormat(shape = JsonFormat.Shape.STRING,pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime toDate
    ){
        public SearchCondition(TaskFilterType taskFilterType){
            this(taskFilterType, null, null);
        }
    }

    public record UpdateDueDate(
            @JsonFormat(shape = JsonFormat.Shape.STRING,pattern = "yyyy-MM-dd'T'HH:mm:ss")
            @Schema(description = "마감 기한 일시 (yyyy-MM-dd'T'HH:mm:ss)",pattern="yyyy-MM-dd'T'HH:mm:ss",examples = "2026-06-19T23:59:59")
            LocalDateTime dueDate,

            @Schema(description = "반복 일정 여부")
            boolean recurring,

            @Schema(description = "반복 규칙 (DAILY, WEEKLY, MONTHLY 등), 반복일정을 설정할 경우 필수",requiredMode = Schema.RequiredMode.AUTO,
            examples = {"DAILY","WEEKLY","MONTHLY"})
            String recurrenceRule,

            @Schema(description = "반복 간격")
            Integer recurrenceInterval,

            @Schema(description = "반복 요일 (MON,TUE,WED) 등 문자열 파싱용)",examples = {"MON","TUE","WED"})
            String recurrenceDays,

            @Schema(description = "반복 요일 일자 (매월 몇 일)")
            Integer recurrenceDayOfMonth,

            @Schema(description = "시간 존재 여부")
            boolean timeSpecified
    ){
        public UpdateDueDate{
            if(recurring && (recurrenceRule == null || recurrenceRule.isBlank())) {
                throw new IllegalArgumentException("반복 일정을 설정할 경우 반복 규칙이 필수입니다.");
            }
        }
    }

    public record UpdateLabel(
            @Size(max=10)
            @Schema(description = "매핑할 라벨 ID 리스트 (최대 10개)")
            List<String> labelIds
    ){
        public UpdateLabel{
            labelIds = (labelIds == null) ? new ArrayList<>() : new ArrayList<>(labelIds);
        }
    }

    public record UpdatePriority(
            @NotNull
            @Schema(description = "테스크 우선 순위 (P1~P4)",requiredMode =  Schema.RequiredMode.REQUIRED
            ,examples = {"P1","P2","P3","P4"})
            TaskPriorityType taskPriorityType
    ){}

    public record UpdateProject(
            @Schema(description = "변경할 프로젝트 아아디")
            String projectId
    ){}

    public record Update(
            @NotBlank(message = "제목은 필수 입력 항목입니다.")
            @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.")
            @Schema(description = "테스크 제목 (최대 100자)", requiredMode = Schema.RequiredMode.REQUIRED)
            String name,

            @Size(max=2000,message = "설명은 2000자를 초과할 수 없습니다.")
            @Schema(description = "테스크 상세 설명 (최대 2000자)")
            String description
    ){}

    public record Cursor(
            @JsonFormat(shape = JsonFormat.Shape.STRING,pattern = "yyyy-MM-dd'T'HH:mm:ss")
            @Schema(description = "이전 마감 기한 일시 (yyyy-MM-dd'T'HH:mm:ss)",pattern="yyyy-MM-dd'T'HH:mm:ss",
            example = "2026-06-19T23:59:59")
            LocalDateTime lastDueDate,

            @Schema(description = "이전 테스크 우선 순위 (P1~P4)")
            TaskPriorityType lastPriorityType,

            @Schema(description = "이전 정렬 순위")
            Long lastSortOrder,

            @Schema(description = "이전 테스크 ID")
            String lastTaskId,

            @Schema(description = "이전 방향 여부(NEXT/PREV)",examples = {"NEXT","PREV"})
            CursorDirection direction
    ){
        public Cursor{
            direction = Objects.requireNonNullElse(direction,CursorDirection.NEXT);
        }

        public static Cursor next(LocalDateTime dueDate, TaskPriorityType priorityType, Long sortOrder, String taskId){
            return new Cursor(dueDate,priorityType,sortOrder,taskId,CursorDirection.NEXT);
        }

        public static Cursor prev(LocalDateTime dueDate, TaskPriorityType priorityType, Long sortOrder, String taskId){
            return new Cursor(dueDate,priorityType,sortOrder,taskId,CursorDirection.PREV);
        }
    }

    public record UpdateSortOrder(
            @Schema(description = "정렬순서")
            Long sortOrder
    ){
    }
}
