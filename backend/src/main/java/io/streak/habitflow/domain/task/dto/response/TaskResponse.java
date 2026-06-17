package io.streak.habitflow.domain.task.dto.response;

import io.streak.habitflow.domain.comment.dto.response.CommentResponse;
import io.streak.habitflow.domain.label.dto.response.LabelResponse;
import io.streak.habitflow.domain.task.dto.query.TaskSummaryQuery;
import io.streak.habitflow.domain.task.dto.request.TaskRequest;
import io.streak.habitflow.domain.task.entity.Task;
import io.streak.habitflow.domain.task.type.TaskPriorityType;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Objects;

public final class TaskResponse{
    @Builder
    public record Detail(
            Long id,
            String name,
            String description,
            boolean completed,
            TaskPriorityType taskPriorityType,
            LocalDateTime dueDate,
            long sortOrder,
            Long userId,
            String userName,
            Long projectId,
            String projectName,
            String projectColor,
            Long parentId,
            boolean recurring,
            java.util.List<Detail> subTasks,
            java.util.List<LabelResponse.Summary> labels,
            java.util.List<CommentResponse.Detail> comments,
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
            Long id,
            String name,
            String description,
            TaskPriorityType taskPriorityType,
            LocalDateTime dueDate,
            long sortOrder,
            String projectName,
            long countSubTasks,
            long countSubTasksCompleted,
            long countComments,
            java.util.List<LabelResponse.Summary> labels,
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
            java.util.List<Summary> content,
            boolean hasNext,
            boolean hasPrev,
            TaskRequest.Cursor nextCursor,
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


