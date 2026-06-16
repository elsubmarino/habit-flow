package io.streak.habitflow.domain.task.dto.response;

import io.streak.habitflow.domain.comment.dto.response.CommentResponse;
import io.streak.habitflow.domain.label.dto.response.LabelResponse;
import io.streak.habitflow.domain.task.dto.query.TaskListQuery;
import io.streak.habitflow.domain.task.entity.Task;
import io.streak.habitflow.domain.task.type.TaskPriorityType;
import lombok.Builder;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;

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
            java.util.List<LabelResponse.List> labels,
            java.util.List<CommentResponse.Detail> comments,
            LocalTime dueTime
    ) {
        @Builder
        public Detail{
            if(subTasks == null){
                subTasks = new ArrayList<>();
            }
            if(labels == null){
                labels = new ArrayList<>();
            }
            if(comments == null){
                comments = new ArrayList<>();
            }
        }
        public static Detail of(Task task, java.util.List<LabelResponse.List> labelListResponses) {
            DetailBuilder builder = Detail.builder()
                    .id(task.getId())
                    .name(task.getName())
                    .description(task.getDescription())
                    .completed(task.isCompleted())
                    .taskPriorityType(task.getTaskPriorityType())
                    .dueDate(task.getDueDate())
                    .sortOrder(task.getSortOrder())
                    .labels(labelListResponses)
                    .recurring(task.isRecurring())
                    .dueTime(task.isHasTime()?task.getDueDate().toLocalTime():null)
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
    public record List(
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
            java.util.List<LabelResponse.List> labels,
            LocalTime dueTime
    ){
        public List{
            if(labels== null){
                labels = new ArrayList<>();
            }
        }

        public static List of(TaskListQuery taskListQuery, java.util.List<LabelResponse.List> labelListResponses) {
            return List.builder()
                    .id(taskListQuery.getId())
                    .name(taskListQuery.getName())
                    .description(taskListQuery.getDescription())
                    .taskPriorityType(taskListQuery.getTaskPriorityType())
                    .dueDate(taskListQuery.getDueDate())
                    .projectName(taskListQuery.getProjectName())
                    .countSubTasks(taskListQuery.getCountSubTasks())
                    .countSubTasksCompleted(taskListQuery.getCountSubTasksCompleted())
                    .countComments(taskListQuery.getCountComments())
                    .labels(labelListResponses)
                    .dueTime(taskListQuery.isHasTime()?taskListQuery.getDueDate().toLocalTime():null)
                    .build();
        }

        public static List of(Task task, java.util.List<LabelResponse.List> labelListResponses) {
            ListBuilder builder = List.builder()
                    .id(task.getId())
                    .name(task.getName())
                    .taskPriorityType(task.getTaskPriorityType())
                    .sortOrder(task.getSortOrder())
                    .labels(labelListResponses);
            if(task.getProject() != null){
                builder.projectName(task.getProject().getName());
            }else{
                builder.projectName("관리함");
            }

            return builder.build();
        }
    }
}


