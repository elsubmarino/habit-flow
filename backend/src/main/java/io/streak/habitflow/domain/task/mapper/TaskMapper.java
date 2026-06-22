package io.streak.habitflow.domain.task.mapper;

import io.streak.habitflow.domain.label.dto.response.LabelResponse;
import io.streak.habitflow.domain.task.dto.response.TaskResponse;
import io.streak.habitflow.domain.task.entity.Task;
import io.streak.habitflow.global.util.HashidsProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TaskMapper {
    private final HashidsProvider hashidsProvider;

    public TaskResponse.Detail toDetail(Task task, String encodedId, List<LabelResponse.Summary> labelSummaryResponses) {
        TaskResponse.Detail.DetailBuilder builder = TaskResponse.Detail.builder()
                .id(encodedId)
                .name(task.getName())
                .description(task.getDescription())
                .completed(task.isCompleted())
                .taskPriorityType(task.getTaskPriorityType())
                .dueDate(task.getDueDate())
                .sortOrder(task.getSortOrder())
                .labels(labelSummaryResponses)
                .recurring(task.isRecurring())
                .dueTime(task.isTimeSpecified()?task.getDueDate().toLocalTime():null)
                .subTasks(task.getSubTasks().stream()
                        .map(subTask-> TaskResponse.Detail.fromSimpleSubTask(subTask,hashidsProvider.encode(subTask.getId())))
                        .toList());


        if(task.getProject() != null){
            builder.projectId(hashidsProvider.encode(task.getProject().getId()))
                    .projectName(task.getProject().getName())
                    .projectColor(task.getProject().getColor());
        }else{
            builder.projectId(null)
                    .projectName("관리함")
                    .projectColor("#808080");
        }

        if(task.getMember() != null){
            builder.userId(hashidsProvider.encode(task.getMember().getId()));
        }

        if(task.getParent() != null){
            builder.parentId(hashidsProvider.encode(task.getParent().getId()));
        }


        return builder.build();
    }
}
