package io.streak.habitflow.domain.task.dto.response;

import io.streak.habitflow.domain.comment.dto.response.CommentResponse;
import io.streak.habitflow.domain.label.dto.response.LabelResponse;
import io.streak.habitflow.domain.task.dto.query.TaskSummaryQuery;
import io.streak.habitflow.domain.task.dto.request.TaskRequest;
import io.streak.habitflow.domain.task.entity.Task;
import io.streak.habitflow.domain.task.type.TaskPriorityType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TaskResponse{
    @Builder
    public record Detail(
            @Schema(description = "테스크 ID")
            Long id,

            @Schema(description = "테스크 제목 (최대 100자)")
            String name,

            @Schema(description = "테스크 상세 설명 (최대 2000자)")
            String description,

            @Schema(description = "완료/미완료 여부")
            boolean completed,

            @Schema(description = "테스크 우선 순위 (P1~P4)")
            TaskPriorityType taskPriorityType,

            @Schema(description = "마감 기한 일시 (yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime dueDate,

            @Schema(description = "정렬 순서")
            long sortOrder,

            @Schema(description = "사용자 ID")
            Long userId,

            @Schema(description = "사용자명")
            String userName,

            @Schema(description = "프로젝트 ID")
            Long projectId,

            @Schema(description = "프로젝트명")
            String projectName,

            @Schema(description = "프로젝트색상")
            String projectColor,

            @Schema(description = "부모 테스크 ID")
            Long parentId,

            @Schema(description = "반복 여부")
            boolean recurring,

            @Schema(description = "하위 테스크들")
            List<Detail> subTasks,

            @Schema(description = "테스크가 속한 라벨들")
            List<LabelResponse.Summary> labels,

            @Schema(description = "테스크가 속한 라벨들")
            List<CommentResponse.Detail> comments,

            @Schema(description = "만료일")
            LocalTime dueTime
    ) {
        @Builder
        public Detail{
            subTasks = Objects.requireNonNullElse(subTasks, new ArrayList<>());
            labels =  Objects.requireNonNullElse(labels, new ArrayList<>());
            comments =  Objects.requireNonNullElse(comments, new ArrayList<>());
        }
        public static Detail of(Task task, java.util.List<LabelResponse.Summary> labelSummaryRespons) {
            DetailBuilder builder = Detail.builder()
                    .id(task.getId())
                    .name(task.getName())
                    .description(task.getDescription())
                    .completed(task.isCompleted())
                    .taskPriorityType(task.getTaskPriorityType())
                    .dueDate(task.getDueDate())
                    .sortOrder(task.getSortOrder())
                    .labels(labelSummaryRespons)
                    .recurring(task.isRecurring())
                    .dueTime(task.isTimeSpecified()?task.getDueDate().toLocalTime():null)
                    .subTasks(task.getSubTasks().stream()
                            .map(Detail::fromSimpleSubTask)
                            .toList());


            if(task.getProject() != null){
                builder.projectId(task.getProject().getId())
                        .projectName(task.getProject().getName())
                        .projectColor(task.getProject().getColor());
            }else{
                builder.projectId(null)
                        .projectName("관리함")
                        .projectColor("#808080");
            }

            if(task.getMember() != null){
                builder.userId(task.getMember().getId());
            }

            if(task.getParent() != null){
                builder.parentId(task.getParent().getId());
            }


            return builder.build();
        }

        private static Detail fromSimpleSubTask(Task subTask){
            return Detail.builder()
                    .id(subTask.getId())
                    .name(subTask.getName())
                    .completed(subTask.isCompleted())
                    .build();
        }
    }

    @Builder
    public record Summary(
            @Schema(name = "테스크 ID")
            Long id,
            @Schema(name = "테스크명")
            String name,
            @Schema(name = "테스크 설명")
            String description,
            @Schema(description = "테스크 우선 순위 (P1~P4)")
            TaskPriorityType taskPriorityType,
            @Schema(description = "마감 기한 일시 (yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime dueDate,
            @Schema(description = "정렬 순서")
            long sortOrder,
            @Schema(description = "프로젝트명")
            String projectName,
            @Schema(description = "하위테스크 수")
            long countSubTasks,
            @Schema(description = "하위테스크 수(완료한)")
            long countSubTasksCompleted,
            @Schema(description = "댓글수")
            long countComments,
            @Schema(description = "테스크에 속한 라벨 리스트")
            List<LabelResponse.Summary> labels,
            @Schema(description = "테스크 시간")
            LocalTime dueTime
    ){
        public Summary {
            labels = Objects.requireNonNullElse(labels, new ArrayList<>());
        }

        public static Summary of(TaskSummaryQuery taskSummaryQuery, java.util.List<LabelResponse.Summary> labelSummaryRespons) {
            return TaskResponse.Summary.builder()
                    .id(taskSummaryQuery.id())
                    .name(taskSummaryQuery.name())
                    .description(taskSummaryQuery.description())
                    .taskPriorityType(taskSummaryQuery.taskPriorityType())
                    .dueDate(taskSummaryQuery.dueDate())
                    .projectName(taskSummaryQuery.projectName())
                    .countSubTasks(taskSummaryQuery.countSubTasks())
                    .countSubTasksCompleted(taskSummaryQuery.countSubTasksCompleted())
                    .countComments(taskSummaryQuery.countComments())
                    .labels(labelSummaryRespons)
                    .dueTime(taskSummaryQuery.timeSpecified()? taskSummaryQuery.dueDate().toLocalTime():null)
                    .build();
        }

        public static Summary of(Task task, java.util.List<LabelResponse.Summary> labelSummaryRespons) {
            SummaryBuilder builder = TaskResponse.Summary.builder()
                    .id(task.getId())
                    .name(task.getName())
                    .taskPriorityType(task.getTaskPriorityType())
                    .sortOrder(task.getSortOrder())
                    .labels(labelSummaryRespons);
            if(task.getProject() != null){
                builder.projectName(task.getProject().getName());
            }else{
                builder.projectName("관리함");
            }

            return builder.build();
        }
    }

    public record SummarySlice(
            @Schema(description = "내용 리스트")
            List<Summary> content,
            @Schema(description = "다음이 존재하는지 여부")
            boolean hasNext,
            @Schema(description = "이전이 존재하는지 여부")
            boolean hasPrev,
            @Schema(description = "다음 커서")
            TaskRequest.Cursor nextCursor,
            @Schema(description = "이전 커서")
            TaskRequest.Cursor prevCursor
    ){}

    @Builder
    public record UpcomingDateCount(
        LocalDate upcomingDate,
        Long count
    ){
        public static UpcomingDateCount of(LocalDate upcomingDate, Long count){
            return UpcomingDateCount.builder()
                    .upcomingDate(upcomingDate)
                    .count(count)
                    .build();
        }
    }

    public record SidebarTasksCount(
            long inbox,
            long today
    ){
    }
}


