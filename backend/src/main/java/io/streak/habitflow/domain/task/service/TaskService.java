package io.streak.habitflow.domain.task.service;

import io.streak.habitflow.domain.attachment.entity.Attachment;
import io.streak.habitflow.domain.comment.entity.Comment;
import io.streak.habitflow.domain.comment.repository.CommentRepository;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.project.entity.Project;
import io.streak.habitflow.domain.project.repository.ProjectRepository;
import io.streak.habitflow.domain.task.dto.TaskCreateRequest;
import io.streak.habitflow.domain.task.dto.TaskRequest;
import io.streak.habitflow.domain.task.dto.TaskResponse;
import io.streak.habitflow.domain.task.entity.Task;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {
    private final TaskRepository taskRepository;
    private final MemberRepository memberRepository;
    private final ProjectRepository projectRepository;
    private final CommentRepository commentRepository;
    private final FileStorageService fileStorageService;

    /**
     * 테스크 삭제
     * @param id 테스크 ID
     * @param userDetails 인증된 사용자 정보
     */
    @Transactional
    @CheckOwnership(type="TASK")
    public void deleteTask(Long id, UserDetails userDetails){
        taskRepository.deleteById(id);
    }


    /**
     * 테스크 생성
     * @param taskCreateRequest 테스크 생성 요청 정보 DTO
     * @param file 첨부파일 (선택)
     * @param userDetails 인증된 사용자 정보
     * @return 생성된 테스크 생성 응답 정보 DTO
     */
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

        Task savedTask = taskRepository.save(task);

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
        return TaskResponse.from(savedTask, new ArrayList<>());
    }

    /**
     * 테스크 단건 조회
     * @param id 테스크 ID
     * @param userDetails 인증된 사용자 정보
     * @return 조회된 테스크 응답 정보 DTO
     */
    public TaskResponse readTask(Long id, UserDetails userDetails){
        Task info = taskRepository.searchTaskInfo(id)
                .orElseThrow(()->new IllegalArgumentException("해당 테스크가 존재하지 않습니다."));

        return TaskResponse.from(info,new ArrayList<>());
    }

    /**
     * 프로젝트 안의 테스크 다건 조회
     * @param ProjectId 프로젝트 ID
     * @param userDetails 인증된 사용자 정보
     * @return 조회된 테스크 응답 정보 DTO
     */
    public List<TaskResponse> getTasksByProject(Long ProjectId, UserDetails userDetails){
        List<Task> tasks = taskRepository.findByProjectId(ProjectId);
        return tasks.stream()
                .map(task -> TaskResponse.from(task,new ArrayList<>()))
                .collect(Collectors.toList());
    }

    /**
     * 테스크 업데이트
     * @param taskId 테스크 ID
     * @param taskRequest 요청된 테스크 요청 정보 DTO
     * @param userDetails 인증된 사용자 정보
     * @return 업데이트 된 테스크 응답 정보 DTO
     */
    @Transactional
    public TaskResponse updateTask(Long taskId, TaskRequest taskRequest,UserDetails userDetails){
        Task task = taskRepository.findById(taskId)
                .orElseThrow(()->new IllegalArgumentException("TASK가 존재하지 않습니다."));
        if(!task.getMember().getEmail().equals(userDetails.getUsername())){
            throw new IllegalStateException("수정 권한이 없습니다.");
        }
        task.updateTask(taskRequest.getTitle(),taskRequest.getDescription());
        return TaskResponse.from(task,new ArrayList<>());
    }

}
