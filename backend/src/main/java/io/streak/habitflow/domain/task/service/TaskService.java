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
import io.streak.habitflow.domain.task.dto.request.TaskRequest;
import io.streak.habitflow.domain.task.dto.response.TaskListQuery;
import io.streak.habitflow.domain.task.dto.response.TaskListResponse;
import io.streak.habitflow.domain.task.dto.response.TaskResponse;
import io.streak.habitflow.domain.task.entity.Task;
import io.streak.habitflow.domain.task.entity.TaskLabel;
import io.streak.habitflow.domain.task.event.TaskChangedEvent;
import io.streak.habitflow.domain.task.repository.TaskRepository;
import io.streak.habitflow.domain.task.type.ActivityType;
import io.streak.habitflow.domain.task.type.TargetType;
import io.streak.habitflow.domain.task.type.TaskFilterType;
import io.streak.habitflow.domain.task.type.TaskPriorityType;
import io.streak.habitflow.global.aop.CheckOwnership;
import io.streak.habitflow.global.aop.DistributedLock;
import io.streak.habitflow.global.common.dto.ScrollResponse;
import io.streak.habitflow.global.infra.file.FileDto;
import io.streak.habitflow.global.infra.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
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
    private final TaskRepository taskRepository;
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
        Task task = taskRepository.findById(taskId).orElseThrow();
        taskRepository.deleteById(taskId);
        applicationEventPublisher.publishEvent(new TaskChangedEvent(
                taskId,
                memberId,
                TargetType.TASK,
                ActivityType.DELETED,
                "당신이 테스크 "+ task.getName()+"을(를) 삭제했습니다"
        ));
    }

    @Transactional
    @CheckOwnership(type="SUB_TASK")
    public TaskResponse createTask(TaskRequest.Create request, MultipartFile file, Long memberId){

        Member member = memberRepository.getReferenceById(memberId);

        Task parentTask = null;
        if(request.parentId() != null){
            parentTask = taskRepository.findById(request.parentId())
                    .orElseThrow(()-> new IllegalArgumentException("존재하지 않는 테스크입니다."));

            if(parentTask.getSubTasks().size() >= 4){
                throw new IllegalStateException("하위 테스크는 최대 4개 까지만 생성할 수 있습니다.");
            }
        }

        Project project = null;
        if(request.projectId() != null){
            project = projectRepository.findById(request.projectId())
                    .orElseThrow(()->new IllegalArgumentException("존재하지 않는 프로젝트입니다."));
            boolean isMember = projectMemberRepository.existsByProjectAndMember(project, member);
            if(!isMember){
                throw new IllegalStateException("해당 프로젝트에 대한 접근 권한이 없습니다.");
            }
        }else if(parentTask != null){
            project = parentTask.getProject();
        }

        Task task = Task.builder()
                .name(request.name())
                .description(request.description())
                .taskPriorityType(request.taskPriorityType())
                .member(member)
                .project(project)
                .parent(parentTask)
                .recurring(request.recurring())
                .recurrenceRule(request.recurrenceRule())
                .recurrenceInterval(request.recurrenceInterval())
                .recurrenceDays(request.recurrenceDays())
                .recurrenceDayOfMonth(request.recurrenceDayOfMonth())
                .subTasks(new ArrayList<>())
                .taskLabels(new ArrayList<>())
                .dueDate(request.dueDate())
                .comments(new ArrayList<>())
                .build();


        if(parentTask != null){
            parentTask.getSubTasks().add(task);
        }

        if(request.labelIds() != null && !request.labelIds().isEmpty()){
            List<Label> labels = labelRepository.findAllById(request.labelIds());
            if(labels.size() != request.labelIds().size()){
                throw new IllegalArgumentException("존재하지 않는 라벨이 있습니다.");
            }
            List<TaskLabel> taskLabels = labels.stream()
                    .map(label -> TaskLabel.builder()
                            .label(label)
                            .task(task)
                            .build())
                    .toList();
            taskLabels.forEach(task::addTaskLabel);
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
            task.addComment(comment);
        }

        Task savedTask = taskRepository.save(task);

        applicationEventPublisher.publishEvent(new TaskChangedEvent(
                savedTask.getId(),
                memberId,
                TargetType.TASK,
                ActivityType.ADDED,
                "당신이 테스크 "+ savedTask.getName()+"을(를) 추가했습니다"
        ));

        List<LabelListResponse> labelListResponses = savedTask.getTaskLabels()
                .stream()
                .map(taskLabel -> {
                    Label realLabel = taskLabel.getLabel();
                    return LabelListResponse.from(realLabel);
                })
                .toList();

        return TaskResponse.of(savedTask, labelListResponses);
    }

    public TaskResponse getTaskById(Long taskId, Long memberId){
        Task task = taskRepository.findByIdWithProject(taskId)
                .orElseThrow(()->new IllegalArgumentException("존재하지 않는 테스크입니다."));

        if(!task.getMember().getId().equals(memberId)){
            throw new AccessDeniedException("해당 자원에 대한 권한이 없습니다.");
        }

        List<LabelListResponse> labelListResponses = task.getTaskLabels().stream()
                .map(taskLabel -> LabelListResponse.from(taskLabel.getLabel()))
                .toList();

        return TaskResponse.of(task, labelListResponses);
    }

    public List<TaskListResponse> getTasksByProject(Long ProjectId){
        List<Task> tasks = taskRepository.findByProjectId(ProjectId);


        return tasks.stream()
                .map(task -> TaskListResponse.of(task,new ArrayList<>()))
                .toList();
    }

    public ScrollResponse<TaskListResponse> getTasks(TaskRequest.SearchCondition searchCondition, Long memberId, Pageable pageable){

        List<TaskListQuery> tasks = taskRepository.searchTasksByCondition(searchCondition, memberId, pageable);

        boolean hasNext = false;

        if(tasks.size() > pageable.getPageSize()){
            hasNext = true;
            tasks = tasks.subList(0, pageable.getPageSize());
        }

        List<TaskListResponse> taskListResponses = tasks.stream()
                .map(task->TaskListResponse.of(task,new ArrayList<>()))
                .toList();

        return ScrollResponse.<TaskListResponse>builder()
                .content(taskListResponses)
                .hasNext(hasNext)
                .build();

    }

    @Transactional
    @CheckOwnership(type="TASK")
    public TaskResponse updateTask(Long taskId, TaskRequest.Update request, Long memberId){
        Task task = taskRepository.findById(taskId)
                .orElseThrow();

        boolean isNameChanged = false;
        boolean isDescriptionChanged = false;

        if(request.name()!=null){
            if(!task.getName().equals(request.name())){
                task.updateName(request.name());
                isNameChanged = true;
            }
        }

        if(request.description()!=null){
            if(!task.getDescription().equals(request.description())){
                task.updateDescription(request.description());
                isDescriptionChanged = true;
            }
        }


        if(isNameChanged || isDescriptionChanged){
            StringBuilder sb = new StringBuilder();
            sb.append("당신이").append(" 테스크 ").append(task.getName()).append("의");
            if(isNameChanged && isDescriptionChanged) sb.append("이름과 설명을");
            else if(isNameChanged) sb.append("이름을");
            else sb.append("설명을");

            sb.append(" 변경했습니다.");

            applicationEventPublisher.publishEvent(new TaskChangedEvent(
                    task.getId(),
                    memberId,
                    TargetType.TASK,
                    ActivityType.UPDATED,
                    sb.toString()
            ));
        }

        List<LabelListResponse> labelListResponses = task.getTaskLabels().stream()
                .map(taskLabel -> LabelListResponse.from(taskLabel.getLabel()))
                .toList();

        return TaskResponse.of(task, labelListResponses);
    }

    @Transactional
    @CheckOwnership(type="TASK")
    @DistributedLock(key = "#taskId")
    public TaskResponse toggleCompletion(Long taskId, Long memberId){
        Task task = taskRepository.findById(taskId)
                 .orElseThrow();

        boolean nextCompletion = !task.isCompleted();
        task.updateCompleted(nextCompletion);

        if(task.isRecurring() && nextCompletion){
            LocalDate nextDueDate = calculateNextInstanceDate(task.getDueDate(), task);

            task.updateDueDate(nextDueDate);

            applicationEventPublisher.publishEvent(new TaskChangedEvent(
                    taskId,
                    memberId,
                    TargetType.TASK,
                    ActivityType.COMPLETED,
                    "당신이 테스크 "+ task.getName()+"을(를) 완료했습니다"
            ));
        }else{
            ActivityType activityType = nextCompletion ? ActivityType.COMPLETED : ActivityType.UNCOMPLETED;
            String statusText = nextCompletion ? "완료했습니다":"미완료했습니다";

            applicationEventPublisher.publishEvent(new TaskChangedEvent(
                    task.getId(),
                    memberId,
                    TargetType.TASK,
                    activityType,
                    "당신이 테스크 "+ task.getName()+"을(를) "+statusText
            ));
        }


        List<LabelListResponse> labelListResponses = task.getTaskLabels().stream()
                .map(taskLabel -> LabelListResponse.from(taskLabel.getLabel()))
                .toList();

        return TaskResponse.of(task,labelListResponses);
    }

    public long getTaskCount(TaskFilterType taskFilterType, Long memberId){
        return taskRepository.countTasksByCondition(taskFilterType, memberId);
    }

    private LocalDate calculateNextInstanceDate(LocalDate currentDueDate, Task task){
        int interval = task.getRecurrenceInterval() > 0 ? task.getRecurrenceInterval() : 1;

        switch(task.getRecurrenceRule()){
            case "DAILY":
                //매일 또는 interval일 마다
                return currentDueDate.plusDays(interval);
            case "WEEKLY":
                // 매주 특정 요일 (예: MON, WED, FRI)
               if(task.getRecurrenceDays() != null && !task.getRecurrenceDays().isEmpty()){
                   LocalDate nextDay = currentDueDate.plusDays(1);
                   for(int i=0;i<7;i++){
                       String dayName = nextDay.getDayOfWeek().name().substring(0,3);
                       if(task.getRecurrenceDays().contains(dayName)){
                           return nextDay;
                       }
                       nextDay = nextDay.plusDays(1);
                   }
               }
               return currentDueDate.plusWeeks(interval);
            case "MONTHLY":
                //매월 특정 일(예: 매월 11일)
                LocalDate nextMonth = currentDueDate.plusMonths(interval);
                if(task.getRecurrenceDayOfMonth() != null){
                    int targetDay = task.getRecurrenceDayOfMonth();
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
    public TaskResponse updateTaskDueDate(Long taskId, TaskRequest.UpdateDueDate request, Long memberId){
        Task task = taskRepository.findById(taskId)
                .orElseThrow();

        task.updateDueDate(request.dueDate());
        task.updateRecurring(request.recurring());
        task.updateRecurrenceInterval(request.recurrenceInterval());
        task.updateRecurrenceDays(request.recurrenceDays());
        task.updateRecurrenceDayOfMonth(request.recurrenceDayOfMonth());
        task.updateRecurrenceRule(request.recurrenceRule());

        applicationEventPublisher.publishEvent(new TaskChangedEvent(
                task.getId(),
                memberId,
                TargetType.TASK,
                ActivityType.UPDATED,
                "당신이 테스크 "+ task.getName()+"의 날짜를 "+ task.getDueDate()+"으로 변경했습니다."
        ));

        List<LabelListResponse> labelListResponses = task.getTaskLabels()
                .stream()
                .map(taskLabel -> LabelListResponse.from(taskLabel.getLabel()))
                .toList();
        return TaskResponse.of(task, labelListResponses);
    }

    @Transactional
    @CheckOwnership(type="TASK")
    @SuppressWarnings("unused")
    public TaskResponse updatePriority(Long taskId, TaskPriorityType taskPriorityType, Long memberId){
        Task task = taskRepository.findById(taskId)
                .orElseThrow();
        task.updatePriorityType(taskPriorityType);

        applicationEventPublisher.publishEvent(new TaskChangedEvent(
                task.getId(),
                memberId,
                TargetType.TASK,
                ActivityType.UPDATED,
                "당신이 테스크 "+ task.getName()+"의 우선순위를 "+ task.getTaskPriorityType().name()+"으로 변경했습니다."
        ));


        List<LabelListResponse> labelListResponses = task.getTaskLabels()
                .stream()
                .map(taskLabel -> LabelListResponse.from(taskLabel.getLabel()))
                .toList();
        return TaskResponse.of(task,labelListResponses);
    }

    @Transactional
    @CheckOwnership(type="TASK")
    @SuppressWarnings("unused")
    public TaskResponse updateTaskLabels(Long taskId, List<Long> labelIds, Long memberId){
        Task task = taskRepository.findById(taskId)
                .orElseThrow();

        if(labelIds == null || labelIds.isEmpty()){
            task.getSubTasks().clear();
            return TaskResponse.of(task, new ArrayList<>());
        }

        List<Label> realLabels  =labelRepository.findAllById(labelIds);

        if(realLabels.size() != labelIds.size()){
            throw new IllegalArgumentException("존재하지 않는 라벨이 포함되어 있습니다.");
        }

        task.getTaskLabels().clear();

        realLabels.forEach(label-> task.addTaskLabel(TaskLabel.builder().label(label).build()));

        applicationEventPublisher.publishEvent(new TaskChangedEvent(
                task.getId(),
                memberId,
                TargetType.TASK,
                ActivityType.UPDATED,
                "당신이 테스크 "+ task.getName()+"의 라벨을 "+realLabels.stream().map(Label::getName)
                        .collect(Collectors.joining(", "))+"으로 변경했습니다."
        ));


        List<LabelListResponse> labelListResponses = task.getTaskLabels()
                .stream()
                .map(taskLabel -> LabelListResponse.from(taskLabel.getLabel()))
                .toList();
        return TaskResponse.of(task, labelListResponses);
    }

    @Transactional
    @CheckOwnership(type="TASK")
    @CheckOwnership(type="PROJECT")
    @SuppressWarnings("unused")
    public TaskResponse updateProject(Long taskId, Long projectId, Long memberId){
        Task task = taskRepository.findById(taskId)
                .orElseThrow();

        Project project = null;
        if(projectId != null) {
            project = projectRepository.findById(projectId)
                    .orElseThrow();
        }
        task.updateProject(project);

        applicationEventPublisher.publishEvent(new TaskChangedEvent(
                task.getId(),
                memberId,
                TargetType.TASK,
                ActivityType.MOVED,
                "당신이 테스크 "+ task.getName()+"를 "+
                        ((task.getProject() != null) ? task.getProject().getName() : "관리함") +"으로 이동시켰습니다."
        ));
        List<LabelListResponse> labelListResponses = task.getTaskLabels()
                .stream()
                .map(taskLabel -> LabelListResponse.from(taskLabel.getLabel()))
                .toList();
        return TaskResponse.of(task, labelListResponses);
    }

}
