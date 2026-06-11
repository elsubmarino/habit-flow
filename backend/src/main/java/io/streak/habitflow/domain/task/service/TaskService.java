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
import io.streak.habitflow.domain.task.dto.request.TaskUpdateRequest;
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
import io.streak.habitflow.global.common.dto.ScrollResponse;
import io.streak.habitflow.global.infra.file.FileDto;
import io.streak.habitflow.global.infra.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
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
                "당신이 테스크 "+task.getName()+"을(를) 삭제했습니다"
        ));
    }

    @Transactional
    @CheckOwnership(type="SUB_TASK")
    public TaskResponse createTask(TaskCreateRequest taskCreateRequest, MultipartFile file, Long memberId){

        Member member = memberRepository.getReferenceById(memberId);

        Task parentTask = null;
        if(taskCreateRequest.getParentId() != null){
            parentTask = taskRepository.findById(taskCreateRequest.getParentId())
                    .orElseThrow(()-> new IllegalArgumentException("존재하지 않는 테스크입니다."));

            if(parentTask.getSubTasks().size() >= 4){
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
        }else if(parentTask != null){
            project = parentTask.getProject();
        }

        Task task = Task.builder()
                .name(taskCreateRequest.getName())
                .description(taskCreateRequest.getDescription())
                .dueDate(taskCreateRequest.getDueDate())
                .taskPriorityType(taskCreateRequest.getTaskPriorityType())
                .member(member)
                .project(project)
                .parent(parentTask)
                .subTasks(new ArrayList<>())
                .taskLabels(new ArrayList<>())
                .comments(new ArrayList<>())
                .build();

        if(parentTask != null){
            parentTask.getSubTasks().add(task);
        }

        if(taskCreateRequest.getLabelIds() != null && !taskCreateRequest.getLabelIds().isEmpty()){
            for(Long labelId : taskCreateRequest.getLabelIds()){
                Label label = labelRepository.findById(labelId)
                        .orElseThrow(()->new IllegalArgumentException("존재하지 않는 라벨입니다."));

                TaskLabel taskLabel = TaskLabel.builder()
                        .label(label)
                        .build();

                task.addTaskLabel(taskLabel);
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
            task.addComment(comment);
        }

        Task savedTask = taskRepository.save(task);

        applicationEventPublisher.publishEvent(new TaskChangedEvent(
                savedTask.getId(),
                memberId,
                TargetType.TASK,
                ActivityType.ADDED,
                "당신이 테스크 "+savedTask.getName()+"을(를) 추가했습니다"
        ));

        List<LabelListResponse> labelListResponses = savedTask.getTaskLabels()
                .stream()
                .map(taskLabel -> {
                    Label realLabel = taskLabel.getLabel();
                    return LabelListResponse.from(realLabel);
                })
                .toList();

        return TaskResponse.from(savedTask, labelListResponses);
    }

    @CheckOwnership(type="TASK")
    @SuppressWarnings("unused")
    public TaskResponse getTaskById(Long taskId, Long memberId){
        Task task = taskRepository.findById(taskId)
                .orElseThrow();

        List<LabelListResponse> labelListResponses = task.getTaskLabels().stream()
                .map(taskLabel -> LabelListResponse.from(taskLabel.getLabel()))
                .toList();

        return TaskResponse.from(task,labelListResponses);
    }

    public List<TaskListResponse> getTasksByProject(Long ProjectId){
        List<Task> tasks = taskRepository.findByProjectId(ProjectId);


        return tasks.stream()
                .map(task -> TaskListResponse.from(task,new ArrayList<>()))
                .toList();
    }

    public ScrollResponse<TaskListResponse> getTasks(TaskSearchCondition taskSearchCondition, Long memberId, Pageable pageable){

        List<TaskListResponse> taskListResponses = taskRepository.searchTasksByCondition(taskSearchCondition, memberId, pageable);

        boolean hasNext = false;
        Long nextCursor = null;

        if(taskListResponses.size() > pageable.getPageSize()){
            hasNext = true;
            taskListResponses = taskListResponses.subList(0, pageable.getPageSize());
        }

        if(!taskListResponses.isEmpty()){
            nextCursor = taskListResponses.get(taskListResponses.size() - 1).getId();
        }

        return ScrollResponse.<TaskListResponse>builder()
                .content(taskListResponses)
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .build();

    }

    @Transactional
    @CheckOwnership(type="TASK")
    @SuppressWarnings("unused")
    public TaskResponse updateTask(Long taskId, TaskUpdateRequest taskUpdateRequest, Long memberId){
        Task task = taskRepository.findById(taskId)
                .orElseThrow();

        boolean isNameChanged = false;
        boolean isDescriptionChanged = false;

        if(taskUpdateRequest.getName()!=null){
            if(!task.getName().equals(taskUpdateRequest.getName())){
                task.updateName(taskUpdateRequest.getName());
                isNameChanged = true;
            }
        }

        if(taskUpdateRequest.getDescription()!=null){
            if(!task.getDescription().equals(taskUpdateRequest.getDescription())){
                task.updateDescription(taskUpdateRequest.getDescription());
                isDescriptionChanged = true;
            }
        }

        if(taskUpdateRequest.getLabelIds() != null){
            task.getTaskLabels().clear();
            for(Long labelId : taskUpdateRequest.getLabelIds()){
                Label label = labelRepository.findById(labelId)
                        .orElseThrow(()->new IllegalArgumentException("라벨이 존재하지 않습니다."));
                task.addTaskLabel(TaskLabel.builder().label(label).build());
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

        return TaskResponse.from(task,labelListResponses);
    }

    @Transactional
    @CheckOwnership(type="TASK")
    public TaskResponse toggleCompletion(Long taskId, Long memberId){
        Task task = taskRepository.findById(taskId)
                 .orElseThrow(()->new IllegalArgumentException("TASK가 존재하지 않습니다."));

         boolean nextCompletion = !task.isCompleted();
        task.updateCompleted(nextCompletion);


        if(nextCompletion){
            applicationEventPublisher.publishEvent(new TaskChangedEvent(
                    taskId,
                    memberId,
                    TargetType.TASK,
                    ActivityType.COMPLETED,
                    "당신이 테스크 "+task.getName()+"을(를) 완료했습니다"
            ));
        }else{
            applicationEventPublisher.publishEvent(new TaskChangedEvent(
                    taskId,
                    memberId,
                    TargetType.TASK,
                    ActivityType.UNCOMPLETED,
                    "당신이 테스크 "+task.getName()+"을(를) 미완료했습니다"
            ));
        }

        List<LabelListResponse> labelListResponses = task.getTaskLabels().stream()
                .map(taskLabel -> LabelListResponse.from(taskLabel.getLabel()))
                .toList();
        return TaskResponse.from(task,labelListResponses);
    }

    public long getTaskCount(TaskFilterType taskFilterType, Long memberId){
        return taskRepository.countTasksByCondition(taskFilterType, memberId);
    }

    @Transactional
    @CheckOwnership(type="TASK")
    @SuppressWarnings("unused")
    public TaskResponse updateTaskDueDate(Long taskId, LocalDateTime dueDate, Long memberId){
        Task task = taskRepository.findById(taskId)
                .orElseThrow();
        task.updateDueDate(dueDate);

        applicationEventPublisher.publishEvent(new TaskChangedEvent(
                task.getId(),
                memberId,
                TargetType.TASK,
                ActivityType.UPDATED,
                "당신이 테스크 "+task.getName()+"의 날짜를 "+task.getDueDate()+"으로 변경했습니다."
        ));

        List<LabelListResponse> labelListResponses = task.getTaskLabels()
                .stream()
                .map(taskLabel -> LabelListResponse.from(taskLabel.getLabel()))
                .toList();
        return TaskResponse.from(task,labelListResponses);
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
                "당신이 테스크 "+task.getName()+"의 우선순위를 "+task.getTaskPriorityType().name()+"으로 변경했습니다."
        ));

        List<LabelListResponse> labelListResponses = task.getTaskLabels()
                .stream()
                .map(taskLabel -> LabelListResponse.from(taskLabel.getLabel()))
                .toList();
        return TaskResponse.from(task,labelListResponses);
    }

    @Transactional
    @CheckOwnership(type="TASK")
    @SuppressWarnings("unused")
    public TaskResponse updateTaskLabels(Long taskId, List<Long> labelIds, Long memberId){
        Task task = taskRepository.findById(taskId)
                .orElseThrow();

        if(labelIds == null || labelIds.isEmpty()){
            task.getSubTasks().clear();
            return TaskResponse.from(task,new ArrayList<>());
        }

        List<Label> realLabels  =labelRepository.findAllById(labelIds);

        if(realLabels.size() != labelIds.size()){
            throw new IllegalArgumentException("존재하지 않는 라벨이 포함되어 있습니다.");
        }

        task.getTaskLabels().clear();

        realLabels.forEach(label->task.addTaskLabel(TaskLabel.builder().label(label).build()));

        applicationEventPublisher.publishEvent(new TaskChangedEvent(
                task.getId(),
                memberId,
                TargetType.TASK,
                ActivityType.UPDATED,
                "당신이 테스크 "+task.getName()+"의 라벨을 "+realLabels.stream().map(Label::getName)
                        .collect(Collectors.joining(", "))+"으로 변경했습니다."
        ));


        List<LabelListResponse> labelListResponses = task.getTaskLabels()
                .stream()
                .map(taskLabel -> LabelListResponse.from(taskLabel.getLabel()))
                .toList();
        return TaskResponse.from(task,labelListResponses);
    }

    @Transactional
    @CheckOwnership(type="TASK")
    @CheckOwnership(type="PROJECT")
    @SuppressWarnings("unused")
    public TaskResponse updateProject(Long taskId, Long projectId, Long memberId){
        Task task = taskRepository.findById(taskId)
                .orElseThrow();
        Project project = projectRepository.findById(projectId)
                        .orElseThrow();
        task.updateProject(project);

        applicationEventPublisher.publishEvent(new TaskChangedEvent(
                task.getId(),
                memberId,
                TargetType.TASK,
                ActivityType.MOVED,
                "당신이 테스크 "+task.getName()+"를 "+task.getProject().getName()+"으로 이동시켰습니다."
        ));
        List<LabelListResponse> labelListResponses = task.getTaskLabels()
                .stream()
                .map(taskLabel -> LabelListResponse.from(taskLabel.getLabel()))
                .toList();
        return TaskResponse.from(task,labelListResponses);
    }

}
