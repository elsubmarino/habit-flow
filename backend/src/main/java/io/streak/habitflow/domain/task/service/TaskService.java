package io.streak.habitflow.domain.task.service;

import io.streak.habitflow.domain.activitylog.dto.ChangeSet;
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
import io.streak.habitflow.domain.task.dto.query.TaskSummaryQuery;
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
import jakarta.persistence.EntityNotFoundException;
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
import java.util.*;

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
                task.getName(),
                Collections.emptyList()
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
                    .orElseThrow(()->new EntityNotFoundException("존재하지 않는 프로젝트입니다."));
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
                throw new EntityNotFoundException("존재하지 않는 라벨이 있습니다.");
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
                savedTask.getName(),
                Collections.emptyList()
        ));

        List<LabelResponse.Summary> labelSummaryRespons = savedTask.getTaskLabels()
                .stream()
                .map(taskLabel -> {
                    Label realLabel = taskLabel.getLabel();
                    return LabelResponse.Summary.from(realLabel);
                })
                .toList();

        return TaskResponse.Detail.of(savedTask, labelSummaryRespons);
    }

    private Task getTask(TaskRequest.Create request, Task parentTask) {
        if(request.parentId() != null){
            parentTask = taskRepository.findById(request.parentId())
                    .orElseThrow(()-> new EntityNotFoundException("존재하지 않는 테스크입니다."));

            if(parentTask.getSubTasks().size() >= 4){
                throw new IllegalStateException("하위 테스크는 최대 4개 까지만 생성할 수 있습니다.");
            }
        }
        return parentTask;
    }

    public TaskResponse.Detail getTaskById(Long taskId, Long memberId){
        Task task = taskRepository.findByIdWithProject(taskId)
                .orElseThrow(()->new EntityNotFoundException("존재하지 않는 테스크입니다."));

        if(!task.getMember().getId().equals(memberId)){
            throw new AccessDeniedException("해당 자원에 대한 권한이 없습니다.");
        }

        List<LabelResponse.Summary> labelSummaryRespons = task.getTaskLabels().stream()
                .map(taskLabel -> LabelResponse.Summary.from(taskLabel.getLabel()))
                .toList();

        return TaskResponse.Detail.of(task, labelSummaryRespons);
    }

    @CheckOwnership(type="PROJECT")
    public Slice<TaskResponse.Summary> getTasksByProject(Long projectId, Long memberId, Pageable pageable){
        int pageSize = pageable.getPageSize();

        List<TaskSummaryQuery> tasks = taskRepository.findTasksByProject(projectId,memberId,pageable);

        boolean hasNext = false;
        if(tasks.size() > pageSize){
            tasks.remove(pageSize);
            hasNext = true;
        }
        List<TaskResponse.Summary> taskSummaryRespons =  tasks.stream()
                .map(task-> TaskResponse.Summary.of(task,new ArrayList<>()))
                .toList();
        return new SliceImpl<>(taskSummaryRespons,pageable,hasNext);
    }

    public TaskResponse.SummarySlice getTasks(TaskRequest.SearchCondition searchCondition,
                                              TaskRequest.Cursor cursor, Long memberId, Pageable pageable){
        int pageSize = pageable.getPageSize();
        List<TaskSummaryQuery> tasks = taskRepository.searchTasksByCondition(searchCondition, cursor, memberId, pageable);

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

        List<Long> taskIds = tasks.stream().map(TaskSummaryQuery::id).toList();
        Map<Long, List<LabelResponse.Summary>> labelMap = labelRepository.findLabelsByTaskIds(taskIds);

        List<TaskResponse.Summary> taskSummaryRespons = tasks.stream()
                .map(task-> TaskResponse.Summary.of(task,
                        labelMap.getOrDefault(task.id(),new ArrayList<>())))
                .toList();

        TaskRequest.Cursor nextCursor = null;
        TaskRequest.Cursor prevCursor = null;

        if(!tasks.isEmpty()){
            TaskSummaryQuery first = tasks.get(0);
            TaskSummaryQuery last = tasks.get(tasks.size()-1);

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

        return new TaskResponse.SummarySlice(taskSummaryRespons,hasNext,hasPrev,nextCursor,prevCursor);
    }

    @Transactional
    @CheckOwnership(type="TASK")
    public void updateTask(Long taskId, TaskRequest.Update request, Long memberId){
        Task task = taskRepository.getOrThrow(taskId);

        List<ChangeSet> changes = new ArrayList<>();

        if(request.name() != null && !task.getName().equals(request.name())){
            changes.add(new ChangeSet("name",task.getName(),request.name()));
            task.updateName(request.name());
        }

        if(request.description() != null && !task.getDescription().equals(request.description())){
            changes.add(new ChangeSet("description",null,null));
            task.updateDescription(request.description());
        }

        if(!changes.isEmpty()) {
            applicationEventPublisher.publishEvent(new TaskChangedEvent(
                    task.getId(),
                    memberId,
                    TargetType.TASK,
                    ActivityType.UPDATED,
                    task.getName(),
                    changes
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
                    task.getName(),
                    Collections.emptyList()));
        }else{
            ActivityType activityType = nextCompletion ? ActivityType.COMPLETED : ActivityType.UNCOMPLETED;

            applicationEventPublisher.publishEvent(new TaskChangedEvent(
                    task.getId(),
                    memberId,
                    TargetType.TASK,
                    activityType,
                    task.getName(),
                    Collections.emptyList()
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
        LocalDateTime oldDueDate = task.getDueDate();
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
        if(!Objects.equals(oldDueDate, request.dueDate())){
            List<ChangeSet> changeSets = new ArrayList<>();
            String fromDate = (oldDueDate != null)? oldDueDate.toString():null;
            String toDate = (request.dueDate() != null)? request.dueDate().toString():null;
            changeSets.add(new ChangeSet("dueDate",fromDate,toDate));
            applicationEventPublisher.publishEvent(new TaskChangedEvent(
                    task.getId(),
                    memberId,
                    TargetType.TASK,
                    ActivityType.UPDATED,
                    task.getName(),
                    changeSets
            ));
        }

    }

    @Transactional
    @CheckOwnership(type="TASK")
    @SuppressWarnings("unused")
    public void updatePriority(Long taskId, TaskPriorityType taskPriorityType, Long memberId){
        Task task = taskRepository.findById(taskId)
                .orElseThrow();
        TaskPriorityType oldTaskPriorityType = task.getTaskPriorityType();
        task.updatePriorityType(taskPriorityType);

        List<ChangeSet>  changeSets = new ArrayList<>();
        changeSets.add(new ChangeSet("priority",oldTaskPriorityType.name(), taskPriorityType.name()));

        applicationEventPublisher.publishEvent(new TaskChangedEvent(
                task.getId(),
                memberId,
                TargetType.TASK,
                ActivityType.UPDATED,
                task.getName(),
                changeSets
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
            throw new EntityNotFoundException("존재하지 않는 라벨이 포함되어 있습니다.");
        }

        task.getTaskLabels().clear();

        realLabels.forEach(label-> task.addTaskLabel(TaskLabel.builder().label(label).build()));
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
            String oldProjectName = project.getName();
            task.updateProject(project);

            List<ChangeSet>  changeSets = new ArrayList<>();
            changeSets.add(new ChangeSet("projectName",oldProjectName,project.getName()));

            applicationEventPublisher.publishEvent(new TaskChangedEvent(
                    task.getId(),
                    memberId,
                    TargetType.TASK,
                    ActivityType.MOVED,
                    task.getName(),
                    changeSets
            ));
        }

    }

    public List<TaskResponse.UpcomingDateCount> getUpcomingDateCounts(Long memberId, LocalDateTime fromDate, LocalDateTime toDate){
        return taskRepository.countUpcomingTasksByDate(memberId, fromDate, toDate);
    }

    @Transactional
    public void updateSortOrder(Long taskId, TaskRequest.UpdateSortOrder updateSortOrder, Long memberId){
        Task task = taskRepository.getReferenceById(taskId);
        task.updateSortOrder(updateSortOrder.sortOrder());
    }
}
