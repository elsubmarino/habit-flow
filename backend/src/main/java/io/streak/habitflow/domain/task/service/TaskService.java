package io.streak.habitflow.domain.task.service;

import io.streak.habitflow.domain.attachment.entity.Attachment;
import io.streak.habitflow.domain.comment.entity.Comment;
import io.streak.habitflow.domain.comment.repository.CommentRepository;
import io.streak.habitflow.domain.label.dto.LabelResponse;
import io.streak.habitflow.domain.label.entity.Label;
import io.streak.habitflow.domain.label.repository.LabelRepository;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.project.entity.Project;
import io.streak.habitflow.domain.project.repository.ProjectRepository;
import io.streak.habitflow.domain.task.dto.TaskCreateRequest;
import io.streak.habitflow.domain.task.dto.TaskRequest;
import io.streak.habitflow.domain.task.dto.TaskResponse;
import io.streak.habitflow.domain.task.entity.Task;
import io.streak.habitflow.domain.task.entity.TaskLabel;
import io.streak.habitflow.domain.task.repository.TaskRepository;
import io.streak.habitflow.global.aop.CheckOwnership;
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
    private final CommentRepository commentRepository;
    private final FileStorageService fileStorageService;
    private final LabelRepository labelRepository;

    @Transactional
    @CheckOwnership(type="TASK")
    public void deleteTask(Long id, UserDetails userDetails){
        Task task = taskRepository.findById(id)
                        .orElseThrow(()->new IllegalArgumentException("조회된 테스크가 없습니다."));
        if(!task.getMember().getEmail().equals(userDetails.getUsername())){
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }
        taskRepository.deleteById(id);
    }

    @Transactional
    public TaskResponse createTask(TaskCreateRequest taskCreateRequest, MultipartFile file, UserDetails userDetails){
        String email = userDetails.getUsername();

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(()->new IllegalArgumentException("존재하지 않는 회원입니다."));

        Project project = null;
        if(taskCreateRequest.getProjectId() != null){
            project = projectRepository.findById(taskCreateRequest.getProjectId())
                    .orElseThrow(()->new IllegalArgumentException("존재하지 않는 프로젝트입니다."));
        }

        Task parentTask = null;
        if(taskCreateRequest.getParentId() != null){
            parentTask = taskRepository.findById(taskCreateRequest.getParentId())
                    .orElseThrow(()-> new IllegalArgumentException("존재하지 않는 테스크입니다."));
        }

        Task task = Task.builder()
                .title(taskCreateRequest.getTitle())
                .description(taskCreateRequest.getDescription())
                .dueDate(taskCreateRequest.getDueDate())
                .priorityType(taskCreateRequest.getPriorityType())
                .member(member)
                .project(project)
                .parent(parentTask)
                .build();

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

        Task savedTask = taskRepository.save(task);

        List<LabelResponse> labelResponses = savedTask.getTaskLabels()
                .stream()
                .map(taskLabel -> {
                    Label realLabel = taskLabel.getLabel();
                    return LabelResponse.from(realLabel);
                })
                .toList();

        if(file != null && !file.isEmpty()){
            FileDto fileDto = fileStorageService.upload(file);

            Comment comment = Comment.builder()
                    .content("첨부파일이 등록되었습니다.")
                    .task(savedTask)
                    .member(member)
                    .build();

            Attachment attachment = Attachment.builder()
                    .originalFileName(fileDto.getOriginalFileName())
                    .savedFileName(fileDto.getSavedFileName())
                    .fileUrl(fileDto.getFileUrl())
                    .build();

            comment.addAttachment(attachment);

            commentRepository.save(comment);


        }
        return TaskResponse.from(savedTask, labelResponses);
    }

    public TaskResponse readTask(Long id, UserDetails userDetails){
        Task task = taskRepository.searchTaskInfo(id)
                .orElseThrow(()->new IllegalArgumentException("해당 테스크가 존재하지 않습니다."));

        if(!task.getMember().getEmail().equals(userDetails.getUsername())){
            throw new IllegalStateException("조회 권한이 없습니다.");
        }

        List<LabelResponse> labelResponses = task.getTaskLabels().stream()
                .map(taskLabel -> LabelResponse.from(taskLabel.getLabel()))
                .toList();

        return TaskResponse.from(task,labelResponses);
    }

    public List<TaskResponse> getTasksByProject(Long ProjectId){
        List<Task> tasks = taskRepository.findByProjectId(ProjectId);


        return tasks.stream()
                .map(task -> TaskResponse.from(task,new ArrayList<>()))
                .toList();
    }

    @Transactional
    public TaskResponse updateTask(Long taskId, TaskRequest taskRequest,UserDetails userDetails){
        Task task = taskRepository.findById(taskId)
                .orElseThrow(()->new IllegalArgumentException("TASK가 존재하지 않습니다."));
        if(!task.getMember().getEmail().equals(userDetails.getUsername())){
            throw new IllegalStateException("수정 권한이 없습니다.");
        }
        task.updateTask(taskRequest.getTitle(),taskRequest.getDescription());

        List<LabelResponse> labelResponses = task.getTaskLabels().stream()
                .map(taskLabel -> LabelResponse.from(taskLabel.getLabel()))
                .toList();

        return TaskResponse.from(task,labelResponses);
    }

}
