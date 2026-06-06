package io.streak.habitflow.domain.task.service;

import io.streak.habitflow.domain.comment.entity.Comment;
import io.streak.habitflow.domain.comment.repository.CommentRepository;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.task.dto.TaskCreateRequest;
import io.streak.habitflow.domain.task.dto.TaskResponse;
import io.streak.habitflow.domain.task.entity.Task;
import io.streak.habitflow.domain.task.repository.TaskRepository;
import io.streak.habitflow.global.infra.file.FileDto;
import io.streak.habitflow.global.infra.file.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
    @InjectMocks
    private TaskService taskService;

    @Mock private TaskRepository taskRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private FileStorageService fileStorageService;

    private Member testMember;
    private UserDetails userDetails;


    @BeforeEach
    public void setup() {
        testMember = Member.builder().email("test@test.com")
                .password("1234")
                .build();
        userDetails = new User("test@test.com", "password", Collections.emptyList());
    }

    @Test
    @DisplayName("TASK를 제대로 읽어들인다.")
    void readTask_Success(){
        Long taskId = 1L;
        Task testTask = Task.builder()
                .title("조회할 업무")
                .description("내용")
                .member(testMember)
                .build();

        given(taskRepository.searchTaskInfo(taskId)).willReturn(Optional.of(testTask));

        TaskResponse response = taskService.readTask(taskId, userDetails);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("조회할 업무");

        verify(taskRepository).searchTaskInfo(taskId);
    }

    @Test
    @DisplayName("존재하지 않는 테스크 ID로 조회하면 예외가 발생한다.")
    void readTask_Fail_TaskNotFound() {
        Long invalidTaskId = 999L;

        given(taskRepository.searchTaskInfo(invalidTaskId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.readTask(invalidTaskId, userDetails))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 테스크가 존재하지 않습니다.");

        verify(taskRepository).searchTaskInfo(invalidTaskId);
    }

    @Test
    @DisplayName("부모 ID가 주어지면 서브테스크가 성공적으로 생성되고 연결된다.")
    void createSubTask_Success() {
        // given: 상황 셋업
        Long parentId = 1L;
        TaskCreateRequest request = TaskCreateRequest.builder()
                .title("하위 업무 추가")
                .parentId(parentId)
                .build();

        Task parentTask = Task.builder()
                .title("부모 업무")
                .member(testMember)
                .build();

        given(memberRepository.findByEmail("test@test.com")).willReturn(Optional.of(testMember));
        given(taskRepository.findById(parentId)).willReturn(Optional.of(parentTask));

        Task savedTask = Task.builder().title("하위 업무 추가").parent(parentTask).member(testMember).build();
        given(taskRepository.save(any(Task.class))).willReturn(savedTask);

        TaskResponse response = taskService.createTask(request, null, userDetails);

        assertThat(response).isNotNull();

        verify(taskRepository).findById(parentId);
        verify(taskRepository).save(any(Task.class));

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(taskCaptor.capture());

        Task capturedTask = taskCaptor.getValue();
        assertThat(capturedTask.getParent()).isNotNull(); // 부모가 잘 세팅되었는지 검증!
        assertThat(capturedTask.getParent().getTitle()).isEqualTo("부모 업무");
    }

    @Test
    @DisplayName("파일 없이 일반 테스크를 성공적으로 생성한다.")
    void createTask_Success_WithoutFile() {
        // given (상황 셋업)
        TaskCreateRequest request = TaskCreateRequest.builder()
                .title("테스트 업무")
                .description("내용")
                .build();

        given(memberRepository.findByEmail("test@test.com")).willReturn(Optional.of(testMember));

        Task savedTask = Task.builder().title("테스트 업무").member(testMember).build();
        given(taskRepository.save(any(Task.class))).willReturn(savedTask);

        TaskResponse response = taskService.createTask(request, null, userDetails);

        assertThat(response).isNotNull();
        verify(taskRepository).save(any(Task.class));
        verify(fileStorageService, never()).upload(any());
        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("첨부파일을 포함하여 테스크를 생성하면 코멘트도 함께 저장된다.")
    void createTask_Success_WithFile() {
        // given
        TaskCreateRequest request = TaskCreateRequest.builder()
                .title("파일 첨부 업무")
                .build();

        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "test.png", "image/png", "test data".getBytes()
        );

        FileDto mockFileDto = FileDto.builder()
                .originalFileName("test.png")
                .savedFileName("uuid-test.png")
                .fileUrl("/uploads/uuid-test.png")
                .build();

        given(memberRepository.findByEmail("test@test.com")).willReturn(Optional.of(testMember));

        Task savedTask = Task.builder().title("파일 첨부 업무").member(testMember).build();
        given(taskRepository.save(any(Task.class))).willReturn(savedTask);

        given(fileStorageService.upload(mockFile)).willReturn(mockFileDto);

        TaskResponse response = taskService.createTask(request, mockFile, userDetails);

        assertThat(response).isNotNull();
        verify(taskRepository).save(any(Task.class));

        verify(fileStorageService).upload(mockFile);
        verify(commentRepository).save(any(Comment.class));
    }
}