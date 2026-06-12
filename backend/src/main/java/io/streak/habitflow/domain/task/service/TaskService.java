package io.streak.habitflow.domain.task.service;

import io.streak.habitflow.domain.attachment.entity.Attachment;
import io.streak.habitflow.domain.comment.entity.Comment;
import io.streak.habitflow.domain.label.dto.response.LabelListResponse;
import io.streak.habitflow.domain.label.entity.Label;
import io.streak.habitflow.domain.label.repository.LabelRepository;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.project.entity.Project;
import io.streak.habitflow.domain.project.repository.ProjectMemberRepository;
import io.streak.habitflow.domain.project.repository.ProjectRepository;
import io.streak.habitflow.domain.task.dto.request.TaskCreateRequest;
import io.streak.habitflow.domain.task.dto.request.TaskSearchCondition;
import io.streak.habitflow.domain.task.dto.request.TaskUpdateDueDateRequest;
import io.streak.habitflow.domain.task.dto.request.TaskUpdateRequest;
import io.streak.habitflow.domain.task.dto.response.TaskListQuery;
import io.streak.habitflow.domain.task.dto.response.TaskListResponse;
import io.streak.habitflow.domain.task.dto.response.TaskResponse;
import io.streak.habitflow.domain.task.entity.TaskInstance;
import io.streak.habitflow.domain.task.entity.TaskLabel;
import io.streak.habitflow.domain.task.entity.TaskMaster;
import io.streak.habitflow.domain.task.event.TaskChangedEvent;
import io.streak.habitflow.domain.task.repository.TaskInstanceRepository;
import io.streak.habitflow.domain.task.repository.TaskMasterMasterRepository;
import io.streak.habitflow.domain.task.type.ActivityType;
import io.streak.habitflow.domain.task.type.TargetType;
import io.streak.habitflow.domain.task.type.TaskFilterType;
import io.streak.habitflow.domain.task.type.TaskPriorityType;
import io.streak.habitflow.global.aop.CheckOwnership;
import io.streak.habitflow.global.common.dto.ScrollResponse;
import io.streak.habitflow.global.infra.file.FileDto;
import io.streak.habitflow.global.infra.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {
    private final TaskMasterMasterRepository taskMasterRepository;
    private final TaskInstanceRepository taskInstanceRepository;
    private final MemberRepository memberRepository;
    private final ProjectRepository projectRepository;
    private final FileStorageService fileStorageService;
    private final LabelRepository labelRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    @CheckOwnership(type="TASK")
    @SuppressWarnings("unused")
    public void deleteTask(Long taskId, Long memberId){
        TaskMaster taskMaster = taskMasterRepository.findById(taskId).orElseThrow();
        taskMasterRepository.deleteById(taskId);
        applicationEventPublisher.publishEvent(new TaskChangedEvent(
                taskId,
                memberId,
                TargetType.TASK,
                ActivityType.DELETED,
                "당신이 테스크 "+ taskMaster.getName()+"을(를) 삭제했습니다"
        ));
    }

    @Transactional
    @CheckOwnership(type="SUB_TASK")
    public TaskResponse createTask(TaskCreateRequest taskCreateRequest, MultipartFile file, Long memberId){

        Member member = memberRepository.getReferenceById(memberId);

        TaskMaster parentTaskMaster = null;
        if(taskCreateRequest.getParentId() != null){
            parentTaskMaster = taskMasterRepository.findById(taskCreateRequest.getParentId())
                    .orElseThrow(()-> new IllegalArgumentException("존재하지 않는 테스크입니다."));

            if(parentTaskMaster.getSubTaskMasters().size() >= 4){
                throw new IllegalStateException("하위 테스크는 최대 4개 까지만 생성할 수 있습니다.");
            }
        }

        Project project = null;
        if(taskCreateRequest.getProjectId() != null){
            project = projectRepository.findById(taskCreateRequest.getProjectId())
                    .orElseThrow(()->new IllegalArgumentException("존재하지 않는 프로젝트입니다."));
            boolean isMember = projectMemberRepository.existsByProjectAndMember(project, member);
            if(!isMember){
                throw new IllegalStateException("해당 프로젝트에 대한 접근 권한이 없습니다.");
            }
        }else if(parentTaskMaster != null){
            project = parentTaskMaster.getProject();
        }

        TaskMaster taskMaster = TaskMaster.builder()
                .name(taskCreateRequest.getName())
                .description(taskCreateRequest.getDescription())
                .taskPriorityType(taskCreateRequest.getTaskPriorityType())
                .member(member)
                .project(project)
                .parent(parentTaskMaster)
                .recurring(taskCreateRequest.isRecurring())
                .recurrenceRule(taskCreateRequest.getRecurrenceRule())
                .recurrenceInterval(taskCreateRequest.getRecurrenceInterval())
                .recurrenceDays(taskCreateRequest.getRecurrenceDays())
                .recurrenceDayOfMonth(taskCreateRequest.getRecurrenceDayOfMonth())
                .subTaskMasters(new ArrayList<>())
                .taskLabels(new ArrayList<>())
                .comments(new ArrayList<>())
                .taskInstances(new ArrayList<>())
                .build();

        LocalDate initialDate = taskCreateRequest.getDueDate() != null ?
                taskCreateRequest.getDueDate() : null;

        TaskInstance firstInstance = TaskInstance.builder()
                .taskMaster(taskMaster)
                .dueDate(initialDate)
                .isCompleted(false)
                .build();

        taskMaster.getTaskInstances().add(firstInstance);

        if(parentTaskMaster != null){
            parentTaskMaster.getSubTaskMasters().add(taskMaster);
        }

        if(taskCreateRequest.getLabelIds() != null && !taskCreateRequest.getLabelIds().isEmpty()){
            for(Long labelId : taskCreateRequest.getLabelIds()){
                Label label = labelRepository.findById(labelId)
                        .orElseThrow(()->new IllegalArgumentException("존재하지 않는 라벨입니다."));

                TaskLabel taskLabel = TaskLabel.builder()
                        .label(label)
                        .build();

                taskMaster.addTaskLabel(taskLabel);
            }
        }

        if(file != null && !file.isEmpty()){
            FileDto fileDto = fileStorageService.upload(file);

            Comment comment = Comment.builder()
                    .content("첨부파일이 등록되었습니다.")
                    .member(member)
                    .attachments(new ArrayList<>())
                    .build();

            Attachment attachment = Attachment.builder()
                    .originalFileName(fileDto.getOriginalFileName())
                    .savedFileName(fileDto.getSavedFileName())
                    .fileUrl(fileDto.getFileUrl())
                    .build();

            comment.addAttachment(attachment);
            taskMaster.addComment(comment);
        }

        TaskMaster savedTaskMaster = taskMasterRepository.save(taskMaster);

        applicationEventPublisher.publishEvent(new TaskChangedEvent(
                savedTaskMaster.getId(),
                memberId,
                TargetType.TASK,
                ActivityType.ADDED,
                "당신이 테스크 "+ savedTaskMaster.getName()+"을(를) 추가했습니다"
        ));

        List<LabelListResponse> labelListResponses = savedTaskMaster.getTaskLabels()
                .stream()
                .map(taskLabel -> {
                    Label realLabel = taskLabel.getLabel();
                    return LabelListResponse.from(realLabel);
                })
                .toList();

        return TaskResponse.of(savedTaskMaster, firstInstance, labelListResponses);
    }

    @CheckOwnership(type="TASK")
    @SuppressWarnings("unused")
    public TaskResponse getTaskById(Long taskInstanceId, Long memberId){
        TaskInstance taskInstance = taskInstanceRepository.findById(taskInstanceId)
                .orElseThrow();

        TaskMaster taskMaster = taskInstance.getTaskMaster();

        List<LabelListResponse> labelListResponses = taskMaster.getTaskLabels().stream()
                .map(taskLabel -> LabelListResponse.from(taskLabel.getLabel()))
                .toList();

        return TaskResponse.of(taskMaster, taskInstance, labelListResponses);
    }

    public List<TaskListResponse> getTasksByProject(Long ProjectId){
        List<TaskMaster> taskMasters = taskMasterRepository.findByProjectId(ProjectId);


        return taskMasters.stream()
                .map(task -> TaskListResponse.of(task,new ArrayList<>()))
                .toList();
    }

    public ScrollResponse<TaskListResponse> getTasks(TaskSearchCondition taskSearchCondition, Long memberId, Pageable pageable){

        List<TaskListQuery> tasks = taskMasterRepository.searchTasksByCondition(taskSearchCondition, memberId, pageable);

        boolean hasNext = false;
        Long nextCursor = null;

        if(tasks.size() > pageable.getPageSize()){
            hasNext = true;
            tasks = tasks.subList(0, pageable.getPageSize());
        }

        if(!tasks.isEmpty()){
            nextCursor = tasks.get(tasks.size() - 1).getId();
        }


        List<TaskListResponse> taskListResponses = tasks.stream()
                .map(task->TaskListResponse.of(task,new ArrayList<>()))
                .toList();

        return ScrollResponse.<TaskListResponse>builder()
                .content(taskListResponses)
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .build();

    }

    @Transactional
    @CheckOwnership(type="TASK")
    public TaskResponse updateTask(Long taskId, TaskUpdateRequest taskUpdateRequest, Long memberId){
        TaskMaster taskMaster = taskMasterRepository.findById(taskId)
                .orElseThrow();

        boolean isNameChanged = false;
        boolean isDescriptionChanged = false;

        if(taskUpdateRequest.getName()!=null){
            if(!taskMaster.getName().equals(taskUpdateRequest.getName())){
                taskMaster.updateName(taskUpdateRequest.getName());
                isNameChanged = true;
            }
        }

        if(taskUpdateRequest.getDescription()!=null){
            if(!taskMaster.getDescription().equals(taskUpdateRequest.getDescription())){
                taskMaster.updateDescription(taskUpdateRequest.getDescription());
                isDescriptionChanged = true;
            }
        }

        if(taskUpdateRequest.getLabelIds() != null){
            taskMaster.getTaskLabels().clear();
            for(Long labelId : taskUpdateRequest.getLabelIds()){
                Label label = labelRepository.findById(labelId)
                        .orElseThrow(()->new IllegalArgumentException("라벨이 존재하지 않습니다."));
                taskMaster.addTaskLabel(TaskLabel.builder().label(label).build());
            }
        }

        if(isNameChanged || isDescriptionChanged){
            StringBuilder sb = new StringBuilder();
            sb.append("당신이").append(" 테스크 ").append(taskMaster.getName()).append("의");
            if(isNameChanged && isDescriptionChanged) sb.append("이름과 설명을");
            else if(isNameChanged) sb.append("이름을");
            else sb.append("설명을");

            sb.append(" 변경했습니다.");

            applicationEventPublisher.publishEvent(new TaskChangedEvent(
                    taskMaster.getId(),
                    memberId,
                    TargetType.TASK,
                    ActivityType.UPDATED,
                    sb.toString()
            ));
        }

        TaskInstance activeInstance = taskInstanceRepository.findByTaskMasterAndIsCompletedFalse(taskMaster)
                .orElseGet(() -> taskInstanceRepository.findTopByTaskMasterOrderByDueDateDesc(taskMaster)
                        .orElseThrow(() -> new IllegalStateException("실행 기록이 존재하지 않는 테스크입니다.")));

        List<LabelListResponse> labelListResponses = taskMaster.getTaskLabels().stream()
                .map(taskLabel -> LabelListResponse.from(taskLabel.getLabel()))
                .toList();

        return TaskResponse.of(taskMaster,activeInstance, labelListResponses);
    }

    @Transactional
    @CheckOwnership(type="TASK")
    public TaskResponse toggleCompletion(Long taskInstanceId, Long memberId){
        TaskInstance taskInstance = taskInstanceRepository.findById(taskInstanceId)
                 .orElseThrow();

        TaskMaster taskMaster = taskInstance.getTaskMaster();

        boolean nextCompletion = !taskInstance.isCompleted();
        taskInstance.updateCompleted(nextCompletion);

        if(taskMaster.isRecurring() && nextCompletion){
            LocalDate nextDueDate = calculateNextInstanceDate(taskInstance.getDueDate(),taskMaster);

            TaskInstance nextParentInstance = TaskInstance.builder()
                    .taskMaster(taskMaster)
                    .dueDate(nextDueDate)
                    .isCompleted(false)
                    .build();
            taskInstanceRepository.save(nextParentInstance);

            applicationEventPublisher.publishEvent(new TaskChangedEvent(
                    taskInstanceId,
                    memberId,
                    TargetType.TASK,
                    ActivityType.COMPLETED,
                    "당신이 테스크 "+ taskMaster.getName()+"을(를) 완료했습니다"
            ));
        }else{
            ActivityType activityType = nextCompletion ? ActivityType.COMPLETED : ActivityType.UNCOMPLETED;
            String statusText = nextCompletion ? "완료했습니다":"미완료했습니다";

            applicationEventPublisher.publishEvent(new TaskChangedEvent(
                    taskMaster.getId(),
                    memberId,
                    TargetType.TASK,
                    activityType,
                    "당신이 테스크 "+ taskMaster.getName()+"을(를) "+statusText
            ));
        }


        List<LabelListResponse> labelListResponses = taskMaster.getTaskLabels().stream()
                .map(taskLabel -> LabelListResponse.from(taskLabel.getLabel()))
                .toList();
        return TaskResponse.of(taskMaster,taskInstance, labelListResponses);
    }

    public long getTaskCount(TaskFilterType taskFilterType, Long memberId){
        return taskMasterRepository.countTasksByCondition(taskFilterType, memberId);
    }

    private LocalDate calculateNextInstanceDate(LocalDate currentDueDate, TaskMaster taskMaster){
        int interval = taskMaster.getRecurrenceInterval() > 0 ? taskMaster.getRecurrenceInterval() : 1;

        switch(taskMaster.getRecurrenceRule()){
            case "DAILY":
                //매일 또는 interval일 마다
                return currentDueDate.plusDays(interval);
            case "WEEKLY":
                // 매주 특정 요일 (예: MON, WED, FRI)
               if(taskMaster.getRecurrenceDays() != null && !taskMaster.getRecurrenceDays().isEmpty()){
                   LocalDate nextDay = currentDueDate.plusDays(1);
                   for(int i=0;i<7;i++){
                       String dayName = nextDay.getDayOfWeek().name().substring(0,3);
                       if(taskMaster.getRecurrenceDays().contains(dayName)){
                           return nextDay;
                       }
                       nextDay = nextDay.plusDays(1);
                   }
               }
               return currentDueDate.plusWeeks(interval);
            case "MONTHLY":
                //매월 특정 일(예: 매월 11일)
                LocalDate nextMonth = currentDueDate.plusMonths(interval);
                if(taskMaster.getRecurrenceDayOfMonth() != null){
                    int targetDay = taskMaster.getRecurrenceDayOfMonth();
                    int maxDayInMonth = nextMonth.lengthOfMonth();
                    return nextMonth.withDayOfMonth(Math.min(targetDay, maxDayInMonth));
                }
                return nextMonth;
            default:
                return currentDueDate.plusDays(1);
        }
    }

    @Transactional
    @CheckOwnership(type="TASK")
    @SuppressWarnings("unused")
    public TaskResponse updateTaskDueDate(Long taskInstanceId, TaskUpdateDueDateRequest taskUpdateDueDateRequest, Long memberId){
        TaskInstance taskInstance = taskInstanceRepository.findById(taskInstanceId)
                .orElseThrow();

        TaskMaster taskMaster = taskInstance.getTaskMaster();

        taskInstance.updateDueDate(taskUpdateDueDateRequest.getDueDate());
        taskMaster.updateIsRecurring(taskUpdateDueDateRequest.isRecurring());
        taskMaster.updateRecurrenceInterval(taskUpdateDueDateRequest.getRecurrenceInterval());
        taskMaster.updateRecurrenceDays(taskUpdateDueDateRequest.getRecurrenceDays());
        taskMaster.updateRecurrenceDayOfMonth(taskUpdateDueDateRequest.getRecurrenceDayOfMonth());
        taskMaster.updateRecurrenceRule(taskUpdateDueDateRequest.getRecurrenceRule());

        applicationEventPublisher.publishEvent(new TaskChangedEvent(
                taskMaster.getId(),
                memberId,
                TargetType.TASK,
                ActivityType.UPDATED,
                "당신이 테스크 "+ taskMaster.getName()+"의 날짜를 "+ taskInstance.getDueDate()+"으로 변경했습니다."
        ));

        List<LabelListResponse> labelListResponses = taskMaster.getTaskLabels()
                .stream()
                .map(taskLabel -> LabelListResponse.from(taskLabel.getLabel()))
                .toList();
        return TaskResponse.of(taskMaster,taskInstance, labelListResponses);
    }

    @Transactional
    @CheckOwnership(type="TASK")
    @SuppressWarnings("unused")
    public TaskResponse updatePriority(Long taskId, TaskPriorityType taskPriorityType, Long memberId){
        TaskMaster taskMaster = taskMasterRepository.findById(taskId)
                .orElseThrow();
        taskMaster.updatePriorityType(taskPriorityType);

        applicationEventPublisher.publishEvent(new TaskChangedEvent(
                taskMaster.getId(),
                memberId,
                TargetType.TASK,
                ActivityType.UPDATED,
                "당신이 테스크 "+ taskMaster.getName()+"의 우선순위를 "+ taskMaster.getTaskPriorityType().name()+"으로 변경했습니다."
        ));

        TaskInstance activeInstance = taskInstanceRepository.findByTaskMasterAndIsCompletedFalse(taskMaster)
                .orElseGet(() -> taskInstanceRepository.findTopByTaskMasterOrderByDueDateDesc(taskMaster)
                        .orElseThrow(() -> new IllegalStateException("실행 기록이 존재하지 않는 테스크입니다.")));


        List<LabelListResponse> labelListResponses = taskMaster.getTaskLabels()
                .stream()
                .map(taskLabel -> LabelListResponse.from(taskLabel.getLabel()))
                .toList();
        return TaskResponse.of(taskMaster,activeInstance, labelListResponses);
    }

    @Transactional
    @CheckOwnership(type="TASK")
    @SuppressWarnings("unused")
    public TaskResponse updateTaskLabels(Long taskId, List<Long> labelIds, Long memberId){
        TaskMaster taskMaster = taskMasterRepository.findById(taskId)
                .orElseThrow();

        TaskInstance activeInstance = taskInstanceRepository.findByTaskMasterAndIsCompletedFalse(taskMaster)
                .orElseGet(() -> taskInstanceRepository.findTopByTaskMasterOrderByDueDateDesc(taskMaster)
                        .orElseThrow(() -> new IllegalStateException("실행 기록이 존재하지 않는 테스크입니다.")));

        if(labelIds == null || labelIds.isEmpty()){
            taskMaster.getSubTaskMasters().clear();
            return TaskResponse.of(taskMaster,activeInstance, new ArrayList<>());
        }

        List<Label> realLabels  =labelRepository.findAllById(labelIds);

        if(realLabels.size() != labelIds.size()){
            throw new IllegalArgumentException("존재하지 않는 라벨이 포함되어 있습니다.");
        }

        taskMaster.getTaskLabels().clear();

        realLabels.forEach(label-> taskMaster.addTaskLabel(TaskLabel.builder().label(label).build()));

        applicationEventPublisher.publishEvent(new TaskChangedEvent(
                taskMaster.getId(),
                memberId,
                TargetType.TASK,
                ActivityType.UPDATED,
                "당신이 테스크 "+ taskMaster.getName()+"의 라벨을 "+realLabels.stream().map(Label::getName)
                        .collect(Collectors.joining(", "))+"으로 변경했습니다."
        ));


        List<LabelListResponse> labelListResponses = taskMaster.getTaskLabels()
                .stream()
                .map(taskLabel -> LabelListResponse.from(taskLabel.getLabel()))
                .toList();
        return TaskResponse.of(taskMaster,activeInstance, labelListResponses);
    }

    @Transactional
    @CheckOwnership(type="TASK")
    @CheckOwnership(type="PROJECT")
    @SuppressWarnings("unused")
    public TaskResponse updateProject(Long taskId, Long projectId, Long memberId){
        TaskMaster taskMaster = taskMasterRepository.findById(taskId)
                .orElseThrow();

        TaskInstance activeInstance = taskInstanceRepository.findByTaskMasterAndIsCompletedFalse(taskMaster)
                .orElseGet(() -> taskInstanceRepository.findTopByTaskMasterOrderByDueDateDesc(taskMaster)
                        .orElseThrow(() -> new IllegalStateException("실행 기록이 존재하지 않는 테스크입니다.")));

        Project project = projectRepository.findById(projectId)
                        .orElseThrow();
        taskMaster.updateProject(project);

        applicationEventPublisher.publishEvent(new TaskChangedEvent(
                taskMaster.getId(),
                memberId,
                TargetType.TASK,
                ActivityType.MOVED,
                "당신이 테스크 "+ taskMaster.getName()+"를 "+ taskMaster.getProject().getName()+"으로 이동시켰습니다."
        ));
        List<LabelListResponse> labelListResponses = taskMaster.getTaskLabels()
                .stream()
                .map(taskLabel -> LabelListResponse.from(taskLabel.getLabel()))
                .toList();
        return TaskResponse.of(taskMaster,activeInstance, labelListResponses);
    }

}
