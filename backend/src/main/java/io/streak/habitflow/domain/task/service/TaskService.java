package io.streak.habitflow.domain.task.service;

import io.streak.habitflow.domain.attachment.entity.Attachment;
import io.streak.habitflow.domain.comment.entity.Comment;
import io.streak.habitflow.domain.label.dto.response.LabelResponse;
import io.streak.habitflow.domain.label.entity.Label;
import io.streak.habitflow.domain.label.repository.LabelRepository;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.project.entity.Project;
import io.streak.habitflow.domain.project.repository.ProjectMemberRepository;
import io.streak.habitflow.domain.project.repository.ProjectRepository;
import io.streak.habitflow.domain.task.dto.query.TaskListQuery;
import io.streak.habitflow.domain.task.dto.request.TaskRequest;
import io.streak.habitflow.domain.task.dto.response.TaskResponse;
import io.streak.habitflow.domain.task.entity.Task;
import io.streak.habitflow.domain.task.entity.TaskLabel;
import io.streak.habitflow.domain.task.event.TaskChangedEvent;
import io.streak.habitflow.domain.task.repository.TaskRepository;
import io.streak.habitflow.domain.task.type.ActivityType;
import io.streak.habitflow.domain.task.type.CursorDirection;
import io.streak.habitflow.domain.task.type.TargetType;
import io.streak.habitflow.domain.task.type.TaskPriorityType;
import io.streak.habitflow.global.aop.CheckOwnership;
import io.streak.habitflow.global.aop.DistributedLock;
import io.streak.habitflow.global.infra.file.FileDto;
import io.streak.habitflow.global.infra.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        Task task = taskRepository.getOrThrow(taskId);
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
    public TaskResponse.Detail createTask(TaskRequest.Create request, MultipartFile file, Long memberId){

        Member member = memberRepository.getReferenceById(memberId);

        Task parentTask = null;
        parentTask = getTask(request, parentTask);

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
                .timeSpecified(request.timeSpecified())
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
                    .originalFileName(fileDto.originalFileName())
                    .savedFileName(fileDto.savedFileName())
                    .fileUrl(fileDto.fileUrl())
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

        List<LabelResponse.List> labelListResponses = savedTask.getTaskLabels()
                .stream()
                .map(taskLabel -> {
                    Label realLabel = taskLabel.getLabel();
                    return LabelResponse.List.from(realLabel);
                })
                .toList();

        return TaskResponse.Detail.of(savedTask, labelListResponses);
    }

    private Task getTask(TaskRequest.Create request, Task parentTask) {
        if(request.parentId() != null){
            parentTask = taskRepository.findById(request.parentId())
                    .orElseThrow(()-> new IllegalArgumentException("존재하지 않는 테스크입니다."));

            if(parentTask.getSubTasks().size() >= 4){
                throw new IllegalStateException("하위 테스크는 최대 4개 까지만 생성할 수 있습니다.");
            }
        }
        return parentTask;
    }

    public TaskResponse.Detail getTaskById(Long taskId, Long memberId){
        Task task = taskRepository.findByIdWithProject(taskId)
                .orElseThrow(()->new IllegalArgumentException("존재하지 않는 테스크입니다."));

        if(!task.getMember().getId().equals(memberId)){
            throw new AccessDeniedException("해당 자원에 대한 권한이 없습니다.");
        }

        List<LabelResponse.List> labelListResponses = task.getTaskLabels().stream()
                .map(taskLabel -> LabelResponse.List.from(taskLabel.getLabel()))
                .toList();

        return TaskResponse.Detail.of(task, labelListResponses);
    }

    @CheckOwnership(type="PROJECT")
    public Slice<TaskResponse.List> getTasksByProject(Long projectId, Long memberId, Pageable pageable){
        int pageSize = pageable.getPageSize();

        List<TaskListQuery> tasks = taskRepository.findTasksByProject(projectId,memberId,pageable);

        boolean hasNext = false;
        if(tasks.size() > pageSize){
            tasks.remove(pageSize);
            hasNext = true;
        }
        List<TaskResponse.List> taskListResponses =  tasks.stream()
                .map(task->TaskResponse.List.of(task,new ArrayList<>()))
                .toList();
        return new SliceImpl<>(taskListResponses,pageable,hasNext);
    }

    public TaskResponse.ListSlice getTasks(TaskRequest.SearchCondition searchCondition,
                                             TaskRequest.Cursor cursor, Long memberId, Pageable pageable){
        int pageSize = pageable.getPageSize();
        List<TaskListQuery> tasks = taskRepository.searchTasksByCondition(searchCondition, cursor, memberId, pageable);

        boolean hasNext;
        boolean hasPrev;

        if(cursor != null && cursor.direction() == CursorDirection.PREV){
            hasPrev = tasks.size() > pageSize;
            if(hasPrev){
                tasks = tasks.subList(tasks.size() - pageSize, tasks.size());
            }
            hasNext = cursor.lastTaskId() != null;
        }else{
            hasNext=  tasks.size() > pageSize;
            if(hasNext){
                tasks=tasks.subList(0, pageSize);
            }
            hasPrev = cursor != null && cursor.lastTaskId() != null;
        }

        List<Long> taskIds = tasks.stream().map(TaskListQuery::id).toList();
        Map<Long, List<LabelResponse.List>> labelMap = labelRepository.findLabelsByTaskIds(taskIds);

        List<TaskResponse.List> taskListResponses = tasks.stream()
                .map(task->TaskResponse.List.of(task,
                        labelMap.getOrDefault(task.id(),new ArrayList<>())))
                .toList();

        TaskRequest.Cursor nextCursor = null;
        TaskRequest.Cursor prevCursor = null;

        if(!tasks.isEmpty()){
            TaskListQuery first = tasks.get(0);
            TaskListQuery last = tasks.get(tasks.size()-1);

            if(hasNext){
                nextCursor = TaskRequest.Cursor.next(
                        last.dueDate(),
                        last.taskPriorityType(),
                        last.sortOrder(),
                        last.id()
                );
            }
            if(hasPrev){
                prevCursor = TaskRequest.Cursor.prev(
                        first.dueDate(),
                        first.taskPriorityType(),
                        first.sortOrder(),
                        first.id()
                );
            }
        }

        return new TaskResponse.ListSlice(taskListResponses,hasNext,hasPrev,nextCursor,prevCursor);
    }

    @Transactional
    @CheckOwnership(type="TASK")
    public void updateTask(Long taskId, TaskRequest.Update request, Long memberId){
        Task task = taskRepository.getOrThrow(taskId);

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
    }

    @Transactional
    @CheckOwnership(type="TASK")
    @DistributedLock(key = "#taskId")
    public void toggleCompletion(Long taskId, Long memberId){
        Task task = taskRepository.findById(taskId)
                 .orElseThrow();

        boolean nextCompletion = !task.isCompleted();
        task.updateCompleted(nextCompletion);

        if(task.isRecurring() && nextCompletion){
            LocalDateTime nextDueDate = calculateNextInstanceDate(task.getDueDate(), task);

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
    }

    public TaskResponse.SidebarTasksCount getSidebarTaskCount(Long memberId){
        return taskRepository.countSidebarTasks(memberId);
    }

    private LocalDateTime calculateNextInstanceDate(LocalDateTime currentDueDate, Task task){
        int interval = task.getRecurrenceInterval() > 0 ? task.getRecurrenceInterval() : 1;

        switch(task.getRecurrenceRule()){
            case "DAILY":
                //매일 또는 interval일 마다
                return currentDueDate.plusDays(interval);
            case "WEEKLY":
                // 매주 특정 요일 (예: MON, WED, FRI)
               if(task.getRecurrenceDays() != null && !task.getRecurrenceDays().isEmpty()){
                   LocalDateTime nextDay = currentDueDate.plusDays(1);
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
                LocalDateTime nextMonth = currentDueDate.plusMonths(interval);
                if(task.getRecurrenceDayOfMonth() != null){
                    int targetDay = task.getRecurrenceDayOfMonth();
                    int maxDayInMonth = nextMonth.toLocalDate().lengthOfMonth();
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
    public void updateTaskDueDate(Long taskId, TaskRequest.UpdateDueDate request, Long memberId){
        Task task = taskRepository.findById(taskId)
                .orElseThrow();
        boolean isChanged = task.updateSchedule(
                request.dueDate(),
                request.recurring(),
                request.recurrenceRule(),
                request.recurrenceInterval(),
                request.recurrenceDays(),
                request.recurrenceDayOfMonth(),
                request.timeSpecified()
        );

        if(!isChanged){
            return;
        }

        applicationEventPublisher.publishEvent(new TaskChangedEvent(
                task.getId(),
                memberId,
                TargetType.TASK,
                ActivityType.UPDATED,
                "당신이 테스크 "+ task.getName()+"의 날짜를 "+ task.getDueDate()+"으로 변경했습니다."
        ));
    }

    @Transactional
    @CheckOwnership(type="TASK")
    @SuppressWarnings("unused")
    public void updatePriority(Long taskId, TaskPriorityType taskPriorityType, Long memberId){
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
    }

    @Transactional
    @CheckOwnership(type="TASK")
    @SuppressWarnings("unused")
    public void updateTaskLabels(Long taskId, List<Long> labelIds, Long memberId){
        Task task = taskRepository.findById(taskId)
                .orElseThrow();

        if(labelIds == null || labelIds.isEmpty()){
            task.getSubTasks().clear();
            return;
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
    }

    @Transactional
    @CheckOwnership(type="TASK")
    @CheckOwnership(type="PROJECT")
    @SuppressWarnings("unused")
    public void updateProject(Long taskId, Long projectId, Long memberId){
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
    }

    public List<TaskResponse.UpcomingDateCount> getUpcomingDateCounts(Long memberId, LocalDateTime fromDate, LocalDateTime toDate){
        return taskRepository.countUpcomingTasksByDate(memberId, fromDate, toDate);
    }

}
