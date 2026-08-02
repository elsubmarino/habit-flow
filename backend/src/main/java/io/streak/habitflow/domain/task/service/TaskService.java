package io.streak.habitflow.domain.task.service;

import io.streak.habitflow.domain.activitylog.event.ActivitiesRecordedEvent;
import io.streak.habitflow.domain.activitylog.event.ActivityRecordedEvent;
import io.streak.habitflow.domain.activitylog.vo.ChangeSet;
import io.streak.habitflow.domain.comment.entity.Attachment;
import io.streak.habitflow.domain.comment.entity.Comment;
import io.streak.habitflow.domain.label.dto.query.LabelSummaryQuery;
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
import io.streak.habitflow.domain.task.mapper.TaskMapper;
import io.streak.habitflow.domain.task.repository.TaskRepository;
import io.streak.habitflow.domain.task.type.CursorDirection;
import io.streak.habitflow.domain.task.type.TaskFilterType;
import io.streak.habitflow.domain.task.type.TaskPriorityType;
import io.streak.habitflow.global.aop.DistributedLock;
import io.streak.habitflow.global.common.type.ActivityType;
import io.streak.habitflow.global.common.type.TargetType;
import io.streak.habitflow.global.error.ErrorCode;
import io.streak.habitflow.global.error.exception.BusinessException;
import io.streak.habitflow.global.infra.file.FileStorageService;
import io.streak.habitflow.global.infra.file.StoredFile;
import io.streak.habitflow.global.util.HashidsProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final RedisTemplate<String, Object> redisTemplate;
    private final HashidsProvider hashidsProvider;
    private final TaskMapper taskMapper;

    @Transactional
    @PreAuthorize("@taskAuthorization(#publicTaskId)")
    public void deleteTask(UUID publicTaskId, Long loginMemberId){
        Task task = taskRepository.getOrThrowByPublicId(publicTaskId);
        taskRepository.deleteById(task.getId());
        applicationEventPublisher.publishEvent(new ActivityRecordedEvent(
                task.getId(),
                loginMemberId,
                TargetType.TASK,
                ActivityType.DELETED,
                task.getName(),
                Collections.emptyList()
        ));
    }

    @Transactional
    @PreAuthorize("@taskAuthorization(#request.publicParentId)")
    public TaskResponse.Detail createTask(TaskRequest.Create request, StoredFile storedFile, Long loginMemberId){

        Member member = memberRepository.getReferenceById(loginMemberId);

        Task parentTask = null;
        parentTask = resolveParentTask(request, parentTask);

        Project project = null;
        Long maxSortOrder = 0L;
        if(request.publicProjectId() != null){
            project = projectRepository.findByPublicId(request.publicProjectId())
                    .orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
            long projectCount = taskRepository.countByProject(project);
            if (projectCount >= 500) {
                throw new BusinessException(ErrorCode.TASK_LIMIT_EXCEEDED);
            }

            boolean isMember = projectMemberRepository.existsByProjectAndMember(project, member);
            if(!isMember){
                throw new BusinessException(ErrorCode.ACCESS_DENIED);
            }
            maxSortOrder = projectRepository.findMaxSortOrder(loginMemberId, project.getId());
        }else if(parentTask != null){
            project = parentTask.getProject();
        }else {
            //관리함인 경우 유저별 500개까지
            long inboxCount = taskRepository.countByProjectIsNullAndMember(member);
            if (inboxCount > 500) {
                throw new BusinessException(ErrorCode.TASK_LIMIT_EXCEEDED);
            }
        }

        //String redisKey="TASK_MAX_SORT:"+request.publicProjectId();
        //Long nextSortOrder = redisTemplate.opsForValue().increment(redisKey, 1024L);
        
        Long nextOrderOrder = maxSortOrder + 1024L;

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
                .sortOrder(nextOrderOrder)
                .build();

        task.validateRecurrence();

        if(parentTask != null){
            parentTask.getSubTasks().add(task);
        }

        if(request.labelIds() != null && !request.labelIds().isEmpty()){
            List<Long> realLabelIds = request.labelIds().stream()
                    .map(hashidsProvider::decode)
                    .toList();
            List<Label> labels = labelRepository.findAllById(realLabelIds);
            if(labels.size() != request.labelIds().size()){
                throw new BusinessException(ErrorCode.INVALID_LABEL);
            }
            List<TaskLabel> taskLabels = labels.stream()
                    .map(label -> TaskLabel.builder()
                            .label(label)
                            .task(task)
                            .build())
                    .toList();
            taskLabels.forEach(task::addTaskLabel);
        }

        if(storedFile != null){
            Comment comment = Comment.builder()
                    .content("첨부파일이 등록되었습니다.")
                    .member(member)
                    .attachments(new ArrayList<>())
                    .build();

            Attachment attachment = Attachment.builder()
                    .originalFileName(storedFile.originalFileName())
                    .savedFileName(storedFile.savedFileName())
                    .fileUrl(storedFile.fileUrl())
                    .build();

            comment.addAttachment(attachment);
            task.addComment(comment);
        }

        Task savedTask = taskRepository.save(task);

        applicationEventPublisher.publishEvent(new ActivityRecordedEvent(
                savedTask.getId(),
                loginMemberId,
                TargetType.TASK,
                ActivityType.ADDED,
                savedTask.getName(),
                Collections.emptyList()
        ));

        List<LabelResponse.Summary> labelSummaryResponses = savedTask.getTaskLabels()
                .stream()
                .map(taskLabel -> {
                    Label realLabel = taskLabel.getLabel();
                    return LabelResponse.Summary.of(realLabel, realLabel.getPublicId().toString());
                })
                .toList();
        return taskMapper.toDetail(savedTask, savedTask.getPublicId().toString(), labelSummaryResponses);
    }

    private Task resolveParentTask(TaskRequest.Create request, Task parentTask) {
        if(request.publicParentId() != null){
            parentTask = taskRepository.findByPublicId(request.publicParentId())
                    .orElseThrow(()-> new BusinessException(ErrorCode.NOT_FOUND));

            if(parentTask.getSubTasks().size() >= 4){
                throw new BusinessException(ErrorCode.SUB_TASK_LIMIT_EXCEEDED);
            }
        }
        return parentTask;
    }

    @PreAuthorize("@taskAuthorization.canAccess(#publicTaskId)")
    public TaskResponse.Detail getTaskByPublicId(UUID publicTaskId, Long loginMemberId){
        Task task = taskRepository.findByPublicId(publicTaskId)
                .orElseThrow(()-> new BusinessException(ErrorCode.NOT_FOUND));

        List<LabelResponse.Summary> labelSummaryResponses =
                labelRepository.findLabelSummariesByTaskId(task.getId()).stream()
                        .map(l -> LabelResponse.Summary.of(l, l.publicId().toString()))
                        .toList();

        return taskMapper.toDetail(task, task.getPublicId().toString(), labelSummaryResponses);
    }

    @PreAuthorize("@projectAuthorization(#publicProjectId)")
    public Slice<TaskResponse.Summary> getTasksByProject(UUID publicProjectId, Long loginMemberId, Pageable pageable){
        int pageSize = pageable.getPageSize();

        List<TaskSummaryQuery> tasks = taskRepository.findTaskSummariesByProjectPublicId(publicProjectId,loginMemberId,pageable);

        boolean hasNext = false;
        if(tasks.size() > pageSize){
            tasks.remove(pageSize);
            hasNext = true;
        }
        List<TaskResponse.Summary> taskSummaryResponses =  tasks.stream()
                .map(task-> TaskResponse.Summary.of(task,
                        task.publicId().toString(), new ArrayList<>()))
                .toList();
        return new SliceImpl<>(taskSummaryResponses,pageable,hasNext);
    }

    public TaskResponse.SummarySlice searchTasks(TaskRequest.SearchCondition searchCondition,
                                              TaskRequest.Cursor cursor, Long memberId, Pageable pageable){
        List<TaskSummaryQuery> tasks = searchCondition.taskFilterType() == TaskFilterType.INBOX
                ? taskRepository.searchInboxTasks(searchCondition, cursor, memberId, pageable)
                : taskRepository.searchTasksByCondition(searchCondition, cursor, memberId, pageable);
        int pageSize = pageable.getPageSize();

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
        Map<Long, List<LabelResponse.Summary>> labelMap = labelRepository.findLabelSummariesByTaskIds(taskIds);

        List<TaskResponse.Summary> taskSummaryResponses = tasks.stream()
                .map(task-> {
                            return TaskResponse.Summary.of(task,task.publicId().toString(),
                                    labelMap.getOrDefault(task.id(), new ArrayList<>()));
                        }
                )
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
                        last.publicId().toString()
                );
            }
            if(hasPrev){
                prevCursor = TaskRequest.Cursor.prev(
                        first.dueDate(),
                        first.taskPriorityType(),
                        first.sortOrder(),
                        first.publicId().toString()
                );
            }
        }

        return new TaskResponse.SummarySlice(taskSummaryResponses,hasNext,hasPrev,nextCursor,prevCursor);
    }

    @Transactional
    @PreAuthorize("@taskAuthorization(#publicTaskId)")
    public TaskResponse.Detail updateTask(UUID publicTaskId, TaskRequest.Update request, Long loginMemberId){
        Task task = taskRepository.getOrThrowByPublicId(publicTaskId);

        List<LabelSummaryQuery> taskLabels = labelRepository.findLabelSummariesByTaskId(task.getId());
        List<LabelResponse.Summary> summaries = taskLabels.stream().map(label->{
            return LabelResponse.Summary.of(label,label.publicId().toString());
        }).toList();

        if (Objects.equals(request.name(), task.getName()) &&
                Objects.equals(request.description(), task.getDescription())) {
            return taskMapper.toDetail(task, task.getPublicId().toString(), summaries);
        }

        List<ChangeSet> changes = new ArrayList<>();

        if(request.name() != null && !task.getName().equals(request.name())){
            changes.add(new ChangeSet("name",task.getName(),request.name()));
            task.updateName(request.name());
        }

        if (request.description() != null && !Objects.equals(task.getDescription(), request.description())) {
            changes.add(new ChangeSet("description",null,null));
            task.updateDescription(request.description());
        }

        if(!changes.isEmpty()) {
            applicationEventPublisher.publishEvent(new ActivityRecordedEvent(
                    task.getId(),
                    loginMemberId,
                    TargetType.TASK,
                    ActivityType.UPDATED,
                    task.getName(),
                    changes
            ));
        }


        return taskMapper.toDetail(task,task.getPublicId().toString(),summaries);
    }

    @Transactional
    @PreAuthorize("@taskAuthorization(#publicTaskId)")
    @DistributedLock(key = "#publicTaskId")
    public TaskResponse.Summary toggleCompletion(UUID publicTaskId, Long loginMemberId){
        Task task = taskRepository.getOrThrowByPublicId(publicTaskId);

        boolean nextCompletion = !task.isCompleted();
        task.updateCompleted(nextCompletion);

        if(task.isRecurring() && nextCompletion){
            LocalDateTime nextDueDate = calculateNextInstanceDate(task.getDueDate(), task);

            task.updateDueDate(nextDueDate);

            applicationEventPublisher.publishEvent(new ActivityRecordedEvent(
                    task.getId(),
                    loginMemberId,
                    TargetType.TASK,
                    ActivityType.COMPLETED,
                    task.getName(),
                    Collections.emptyList()));
        }else{
            ActivityType activityType = nextCompletion ? ActivityType.COMPLETED : ActivityType.UNCOMPLETED;

            applicationEventPublisher.publishEvent(new ActivityRecordedEvent(
                    task.getId(),
                    loginMemberId,
                    TargetType.TASK,
                    activityType,
                    task.getName(),
                    Collections.emptyList()
            ));
        }

        List<TaskSummaryQuery> taskSummaryQueries = taskRepository.findTaskSummariesByIds(List.of(task.getId()));
        List<LabelSummaryQuery> taskLabels = labelRepository.findLabelSummariesByTaskId(task.getId());
        List<LabelResponse.Summary> summaries = taskLabels.stream().map(label->{
            return LabelResponse.Summary.of(label,task.getPublicId().toString());
        }).toList();
        return TaskResponse.Summary.of(taskSummaryQueries.get(0),task.getPublicId().toString(),summaries);
    }

    public TaskResponse.SidebarTasksCount getSidebarTaskCounts(Long memberId){
        return taskRepository.findSidebarTaskCounts(memberId);
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
    @PreAuthorize("@taskAuthorization(#publicTaskId)")
    public TaskResponse.Detail updateTaskDueDate(UUID publicTaskId, TaskRequest.UpdateDueDate request, Long loginMemberId){
        Task task = taskRepository.getOrThrowByPublicId(publicTaskId);

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
        List<LabelSummaryQuery> taskLabels = labelRepository.findLabelSummariesByTaskId(task.getId());
        List<LabelResponse.Summary> summaries = taskLabels.stream().map(label->{
            return LabelResponse.Summary.of(label,task.getPublicId().toString());
        }).toList();
        if(!isChanged){
            return taskMapper.toDetail(task,task.getPublicId().toString(),summaries);
        }
        if(!Objects.equals(oldDueDate, request.dueDate())){
            List<ChangeSet> changeSets = new ArrayList<>();
            String fromDate = (oldDueDate != null)? oldDueDate.toString():null;
            String toDate = (request.dueDate() != null)? request.timeSpecified() ? request.dueDate().toString()
                    :request.dueDate().toLocalDate().toString():null;
            changeSets.add(new ChangeSet("dueDate",fromDate,toDate));
            applicationEventPublisher.publishEvent(new ActivityRecordedEvent(
                    task.getId(),
                    loginMemberId,
                    TargetType.TASK,
                    ActivityType.UPDATED,
                    task.getName(),
                    changeSets
            ));
        }

        return taskMapper.toDetail(task,task.getPublicId().toString(),summaries);
    }

    @Transactional
    @SuppressWarnings("unused")
    public List<TaskResponse.Detail> updateTaskDueDateBatch(TaskRequest.UpdateDueDateBatch request, Long memberId){
        List<Long> realTaskIds = taskRepository.findIdByPublicIdIn(request.taskIds());

        // 접근 검사: N회 쿼리 → 집합 쿼리 1회
        if (taskRepository.countAccessibleTasks(realTaskIds, memberId) != realTaskIds.size()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        List<Task> tasks = taskRepository.findAllById(realTaskIds);

        // 라벨 조회: 태스크당 1회 → 배치 1회
        Map<Long, List<LabelResponse.Summary>> labelMap =
                labelRepository.findLabelSummariesByTaskIds(realTaskIds);

        List<TaskResponse.Detail> responseList = new ArrayList<>();
        List<ActivityRecordedEvent> activityEvents = new ArrayList<>();

        for(Task task : tasks){
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

            if(isChanged && !Objects.equals(oldDueDate, request.dueDate())){
                List<ChangeSet> changeSets = new ArrayList<>();
                String fromDate = (oldDueDate != null)? oldDueDate.toString():null;
                String toDate = (request.dueDate() != null)? request.timeSpecified() ? request.dueDate().toString()
                        :request.dueDate().toLocalDate().toString():null;
                changeSets.add(new ChangeSet("dueDate",fromDate,toDate));

                // 루프 안에서 즉시 발행하지 않고 수집만
                activityEvents.add(new ActivityRecordedEvent(
                        task.getId(),
                        memberId,
                        TargetType.TASK,
                        ActivityType.UPDATED,
                        task.getName(),
                        changeSets
                ));
            }

            responseList.add(taskMapper.toDetail(task,task.getPublicId().toString(),
                    labelMap.getOrDefault(task.getId(), List.of())));
        }

        // 이벤트: 태스크당 1회 → 배치 1회 발행 (async 작업 1개만 생성됨)
        if(!activityEvents.isEmpty()){
            applicationEventPublisher.publishEvent(new ActivitiesRecordedEvent(activityEvents));
        }

        return responseList;
    }

    @Transactional
    @PreAuthorize("@taskAuthorization(#publicTaskId)")
    public TaskResponse.Detail updatePriority(UUID publicTaskId, TaskPriorityType taskPriorityType, Long loginMemberId){
        Task task = taskRepository.getOrThrowByPublicId(publicTaskId);
        List<LabelSummaryQuery> taskLabels = labelRepository.findLabelSummariesByTaskId(task.getId());
        List<LabelResponse.Summary> summaries = taskLabels.stream().map(label->{
            return LabelResponse.Summary.of(label,task.getPublicId().toString());
        }).toList();
        if(task.getTaskPriorityType() == taskPriorityType){
            return taskMapper.toDetail(task,task.getPublicId().toString(),summaries);
        }
        TaskPriorityType oldTaskPriorityType = task.getTaskPriorityType();
        task.updatePriorityType(taskPriorityType);

        List<ChangeSet>  changeSets = new ArrayList<>();
        changeSets.add(new ChangeSet("priority",oldTaskPriorityType.name(), taskPriorityType.name()));

        applicationEventPublisher.publishEvent(new ActivityRecordedEvent(
                task.getId(),
                loginMemberId,
                TargetType.TASK,
                ActivityType.UPDATED,
                task.getName(),
                changeSets
        ));

        return taskMapper.toDetail(task,task.getPublicId().toString(),summaries);
    }

    @Transactional
    @PreAuthorize("@taskAuthorization(#publicTaskId)")
    public TaskResponse.Detail updateTaskLabels(UUID publicTaskId, List<String> labelIds, Long loginMemberId){
        Task task = taskRepository.getOrThrowByPublicId(publicTaskId);
        // 빈 목록이면 라벨 전부 제거 후 빈 목록으로 응답
        if(labelIds.isEmpty()){
            task.getTaskLabels().clear();
            return taskMapper.toDetail(task, task.getPublicId().toString(), List.of());
        }
        List<Long> realLabelIds = labelIds.stream()
                .map(hashidsProvider::decode)
                .toList();
        List<Label> realLabels = labelRepository.findAllById(realLabelIds);
        task.getTaskLabels().clear();
        realLabels.forEach(label -> task.addTaskLabel(TaskLabel.builder().label(label).build()));
        // 교체된 라벨(새 상태)로 응답 구성
        List<LabelResponse.Summary> summaries = realLabels.stream()
                .map(label -> LabelResponse.Summary.of(label, label.getPublicId().toString()))
                .toList();
        return taskMapper.toDetail(task, task.getPublicId().toString(), summaries);
    }

    @Transactional
    @PreAuthorize("@taskAuthorization(#publicTaskId)")
    public TaskResponse.Detail moveTaskToProject(UUID publicTaskId, UUID publicProjectId, Long loginMemberId){
        Task task = taskRepository.getOrThrowByPublicId(publicTaskId);
        List<LabelSummaryQuery> taskLabels = labelRepository.findLabelSummariesByTaskId(task.getId());
        List<LabelResponse.Summary> summaries = taskLabels.stream().map(label->{
            return LabelResponse.Summary.of(label,task.getPublicId().toString());
        }).toList();

        Project project = null;
        if(publicProjectId != null) {
            project = projectRepository.getOrThrowByPublicId(publicProjectId);
            String oldProjectName = project.getName();
            task.updateProject(project);

            List<ChangeSet>  changeSets = new ArrayList<>();
            changeSets.add(new ChangeSet("projectName",oldProjectName,project.getName()));

            applicationEventPublisher.publishEvent(new ActivityRecordedEvent(
                    task.getId(),
                    loginMemberId,
                    TargetType.TASK,
                    ActivityType.MOVED,
                    task.getName(),
                    changeSets
            ));
        }

        return taskMapper.toDetail(task,task.getPublicId().toString(),summaries);
    }

    public List<TaskResponse.UpcomingDateCount> getUpcomingDateCounts(Long memberId, LocalDateTime fromDate, LocalDateTime toDate){
        return taskRepository.findUpcomingTaskCountsByDate(memberId, fromDate, toDate);
    }

    @Transactional
    @PreAuthorize("@taskAuthorization(#publicTaskId)")
    public TaskResponse.Summary updateSortOrder(UUID publicTaskId, TaskRequest.UpdateSortOrder updateSortOrder, Long loginMemberId){
        Task task = taskRepository.getOrThrowByPublicId(publicTaskId);
        List<LabelSummaryQuery> taskLabels = labelRepository.findLabelSummariesByTaskId(task.getId());
        List<LabelResponse.Summary> summaries = taskLabels.stream().map(label->{
            return LabelResponse.Summary.of(label,task.getPublicId().toString());
        }).toList();
        List<TaskSummaryQuery> taskSummaryQueries = taskRepository.findTaskSummariesByIds(List.of(task.getId()));

        if(Objects.equals(task.getSortOrder(), updateSortOrder.sortOrder())){
            return TaskResponse.Summary.of(taskSummaryQueries.get(0),task.getPublicId().toString(),summaries);
        }
        task.updateSortOrder(updateSortOrder.sortOrder());

        return TaskResponse.Summary.of(taskSummaryQueries.get(0),task.getPublicId().toString(),summaries);
    }

    public TaskResponse.SummarySlice getTasksByLabel(UUID publicLabelId, Long loginMemberId, Pageable pageable
                                                    , TaskRequest.Cursor cursor){
        int pageSize = pageable.getPageSize();
        List<TaskSummaryQuery> tasks = taskRepository.findTaskSummariesByLabelPublicId(publicLabelId,pageable,loginMemberId);

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
        Map<Long, List<LabelResponse.Summary>> labelMap = labelRepository.findLabelSummariesByTaskIds(taskIds);

        List<TaskResponse.Summary> taskSummaryResponses = tasks.stream()
                .map(task-> {
                            return TaskResponse.Summary.of(task,task.publicId().toString(),
                                    labelMap.getOrDefault(task.id(), new ArrayList<>()));
                        }
                )
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
                        last.publicId().toString()
                );
            }
            if(hasPrev){
                prevCursor = TaskRequest.Cursor.prev(
                        first.dueDate(),
                        first.taskPriorityType(),
                        first.sortOrder(),
                        first.publicId().toString()
                );
            }
        }

        return new TaskResponse.SummarySlice(taskSummaryResponses,hasNext,hasPrev,nextCursor,prevCursor);
    }
}
