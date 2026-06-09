package io.streak.habitflow.domain.task.service;

import io.streak.habitflow.domain.activitylog.dto.request.ActivityLogRequest;
import io.streak.habitflow.domain.activitylog.service.ActivityLogService;
import io.streak.habitflow.domain.attachment.entity.Attachment;
import io.streak.habitflow.domain.comment.entity.Comment;
import io.streak.habitflow.domain.label.dto.response.LabelListResponse;
import io.streak.habitflow.domain.label.entity.Label;
import io.streak.habitflow.domain.label.repository.LabelRepository;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.project.entity.Project;
import io.streak.habitflow.domain.project.repository.ProjectRepository;
import io.streak.habitflow.domain.project.repository.ProjectMemberRepository;
import io.streak.habitflow.domain.task.dto.request.TaskCreateRequest;
import io.streak.habitflow.domain.task.dto.request.TaskSearchCondition;
import io.streak.habitflow.domain.task.dto.request.TaskUpdateRequest;
import io.streak.habitflow.domain.task.dto.response.TaskListResponse;
import io.streak.habitflow.domain.task.dto.response.TaskResponse;
import io.streak.habitflow.domain.task.entity.Task;
import io.streak.habitflow.domain.task.entity.TaskLabel;
import io.streak.habitflow.domain.task.repository.TaskRepository;
import io.streak.habitflow.domain.task.type.ActivityType;
import io.streak.habitflow.global.aop.CheckOwnership;
import io.streak.habitflow.global.common.dto.ScrollResponse;
import io.streak.habitflow.global.infra.file.FileDto;
import io.streak.habitflow.global.infra.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {
    private final TaskRepository taskRepository;
    private final MemberRepository memberRepository;
    private final ProjectRepository projectRepository;
    private final ActivityLogService activityLogService;
    private final FileStorageService fileStorageService;
    private final LabelRepository labelRepository;
    private final ProjectMemberRepository projectMemberRepository;

    @Transactional
    @CheckOwnership(type="TASK")
    public void deleteTask(Long taskId, UserDetails userDetails){
        Task task = taskRepository.findById(taskId)
                        .orElseThrow(()->new IllegalArgumentException("조회된 테스크가 없습니다."));
        if(!task.getMember().getEmail().equals(userDetails.getUsername())){
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }
        taskRepository.deleteById(taskId);
    }

    @Transactional
    public TaskResponse createTask(TaskCreateRequest taskCreateRequest, MultipartFile file, UserDetails userDetails){
        String email = userDetails.getUsername();

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(()->new IllegalArgumentException("존재하지 않는 회원입니다."));

        Task parentTask = null;
        if(taskCreateRequest.getParentId() != null){
            parentTask = taskRepository.findById(taskCreateRequest.getParentId())
                    .orElseThrow(()-> new IllegalArgumentException("존재하지 않는 테스크입니다."));
            validateOwner(parentTask,email);
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
                .priorityType(taskCreateRequest.getPriorityType())
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

        List<LabelListResponse> labelListResponses = savedTask.getTaskLabels()
                .stream()
                .map(taskLabel -> {
                    Label realLabel = taskLabel.getLabel();
                    return LabelListResponse.from(realLabel);
                })
                .toList();

        return TaskResponse.from(savedTask, labelListResponses);
    }

    public TaskResponse getTaskById(Long taskId, UserDetails userDetails){
        Task task = taskRepository.searchTaskInfo(taskId)
                .orElseThrow(()->new IllegalArgumentException("해당 테스크가 존재하지 않습니다."));

        if(!task.getMember().getEmail().equals(userDetails.getUsername())){
            throw new IllegalStateException("조회 권한이 없습니다.");
        }

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

    public ScrollResponse<TaskListResponse> getTasks(TaskSearchCondition taskSearchCondition, UserDetails userDetails){
        String email = userDetails.getUsername();
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(()->new IllegalArgumentException("존재하지 않는 회원입니다."));

        int pageSize = 20;

        List<Task> tasks = taskRepository.searchTasksByCondition(taskSearchCondition, member.getId());

        boolean hasNext = false;
        Long nextCursor = null;

        if(tasks.size() > pageSize){
            hasNext = true;
            tasks = tasks.subList(0, pageSize);
        }

        if(!tasks.isEmpty()){
            nextCursor = tasks.get(tasks.size() - 1).getId();
        }

        List<TaskListResponse> taskListResponses =  tasks.stream()
                .map(task-> {
                    List<LabelListResponse> labelListResponses = task.getTaskLabels().stream()
                            .map(taskLabel -> LabelListResponse.from(taskLabel.getLabel()))
                            .toList();
                    return TaskListResponse.from(task,labelListResponses);
                })
                .toList();

        return ScrollResponse.<TaskListResponse>builder()
                .content(taskListResponses)
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .build();

    }

    @Transactional
    public TaskResponse updateTask(Long taskId, TaskUpdateRequest taskUpdateRequest, UserDetails userDetails){
        Task task = taskRepository.findById(taskId)
                .orElseThrow(()->new IllegalArgumentException("TASK가 존재하지 않습니다."));
        if(!task.getMember().getEmail().equals(userDetails.getUsername())){
            throw new IllegalStateException("수정 권한이 없습니다.");
        }

        if(taskUpdateRequest.getName()!=null){
            task.updateName(taskUpdateRequest.getName());
        }

        if(taskUpdateRequest.getDescription()!=null){
            task.updateDescription(taskUpdateRequest.getDescription());
        }

        if(taskUpdateRequest.getDueDate()!=null){
            task.updateDueDate(taskUpdateRequest.getDueDate());
        }

        if(taskUpdateRequest.getPriorityType()!=null){
            task.updatePriorityType(taskUpdateRequest.getPriorityType());
        }

        if(taskUpdateRequest.getProjectId()!=null){
            Project project= projectRepository.findById(taskUpdateRequest.getProjectId())
                    .orElseThrow(()->new IllegalArgumentException("존재하지 않는 프로젝트입니다."));
            task.changeProject(project);
        }

        if(taskUpdateRequest.getLabelIds() != null){
            task.getTaskLabels().clear();
            for(Long labelId : taskUpdateRequest.getLabelIds()){
                Label label = labelRepository.findById(labelId)
                        .orElseThrow(()->new IllegalArgumentException("라벨이 존재하지 않습니다."));
                task.addTaskLabel(TaskLabel.builder().label(label).build());
            }
        }

        List<LabelListResponse> labelListResponses = task.getTaskLabels().stream()
                .map(taskLabel -> LabelListResponse.from(taskLabel.getLabel()))
                .toList();

        return TaskResponse.from(task,labelListResponses);
    }

    @Transactional
    public TaskResponse toggleCompletion(Long taskId, UserDetails userDetails){
        Task task = taskRepository.findById(taskId)
                .orElseThrow(()->new IllegalArgumentException("TASK가 존재하지 않습니다."));

        validateOwner(task, userDetails.getUsername());

        boolean nextCompletion = !task.isCompleted();
        task.updateCompleted(nextCompletion);


        if(nextCompletion){
            ActivityLogRequest activityLogRequest = ActivityLogRequest.builder()
                    .activityType(ActivityType.COMPLETED)
                    .taskId(taskId)
                    .build();
            activityLogService.create(activityLogRequest,userDetails);
        }

        List<LabelListResponse> labelListResponses = task.getTaskLabels().stream()
                .map(taskLabel -> LabelListResponse.from(taskLabel.getLabel()))
                .toList();
        return TaskResponse.from(task,labelListResponses);
    }

    public void validateOwner(Task task, String email){
        if(!task.getMember().getEmail().equals(email)){
            throw new IllegalStateException("해당 테스크에 대한 접근 권한이 없습니다.");
        }
    }

}
