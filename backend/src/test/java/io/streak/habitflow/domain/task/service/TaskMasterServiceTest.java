package io.streak.habitflow.domain.task.service;

import io.streak.habitflow.domain.comment.entity.Comment;
import io.streak.habitflow.domain.comment.repository.CommentRepository;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.task.dto.request.TaskCreateRequest;
import io.streak.habitflow.domain.task.dto.response.TaskResponse;
import io.streak.habitflow.domain.task.entity.TaskMaster;
import io.streak.habitflow.domain.task.repository.TaskMasterMasterRepository;
import io.streak.habitflow.global.infra.file.FileDto;
import io.streak.habitflow.global.infra.file.FileStorageService;
import io.streak.habitflow.global.security.dto.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class TaskMasterServiceTest {
    @InjectMocks
    private TaskService taskService;

    @Mock private TaskMasterMasterRepository taskMasterRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private FileStorageService fileStorageService;

    private Member testMember;
    private UserPrincipal userPrincipal;
    private Long memberId;


    @BeforeEach
    public void setup() {
        testMember = Member.builder().email("test@test.com")
                .password("1234")
                .build();
        Member savedMember = memberRepository.save(testMember);
        this.memberId = savedMember.getId();
    }

    @Test
    @DisplayName("TASK를 제대로 읽어들인다.")
    void getTask_ById_Success(){
        Long taskId = 1L;
        TaskMaster testTaskMaster = TaskMaster.builder()
                .name("조회할 업무")
                .description("내용")
                .member(testMember)
                .build();

        given(taskMasterRepository.searchTaskInfo(taskId)).willReturn(Optional.of(testTaskMaster));

        TaskResponse response = taskService.getTaskById(taskId, memberId);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("조회할 업무");

        verify(taskMasterRepository).searchTaskInfo(taskId);
    }

    @Test
    @DisplayName("존재하지 않는 테스크 ID로 조회하면 예외가 발생한다.")
    void getTask_Fail_TaskByIdNotFound() {
        Long invalidTaskId = 999L;

        given(taskMasterRepository.searchTaskInfo(invalidTaskId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById(invalidTaskId, memberId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 테스크가 존재하지 않습니다.");

        verify(taskMasterRepository).searchTaskInfo(invalidTaskId);
    }

    @Test
    @DisplayName("부모 ID가 주어지면 서브테스크가 성공적으로 생성되고 연결된다.")
    void createSubTask_Success() {
        // given: 상황 셋업
        Long parentId = 1L;
        TaskCreateRequest request = TaskCreateRequest.builder()
                .name("하위 업무 추가")
                .parentId(parentId)
                .build();

        TaskMaster parentTaskMaster = TaskMaster.builder()
                .name("부모 업무")
                .member(testMember)
                .build();

        given(memberRepository.findById(memberId)).willReturn(Optional.of(testMember));
        given(taskMasterRepository.findById(parentId)).willReturn(Optional.of(parentTaskMaster));

        TaskMaster savedTaskMaster = TaskMaster.builder().name("하위 업무 추가").parent(parentTaskMaster).member(testMember).build();
        given(taskMasterRepository.save(any(TaskMaster.class))).willReturn(savedTaskMaster);

        TaskResponse response = taskService.createTask(request, null, memberId);

        assertThat(response).isNotNull();

        verify(taskMasterRepository).findById(parentId);
        verify(taskMasterRepository).save(any(TaskMaster.class));

        ArgumentCaptor<TaskMaster> taskCaptor = ArgumentCaptor.forClass(TaskMaster.class);
        verify(taskMasterRepository).save(taskCaptor.capture());

        TaskMaster capturedTaskMaster = taskCaptor.getValue();
        assertThat(capturedTaskMaster.getParent()).isNotNull(); // 부모가 잘 세팅되었는지 검증!
        assertThat(capturedTaskMaster.getParent().getName()).isEqualTo("부모 업무");
    }

    @Test
    @DisplayName("파일 없이 일반 테스크를 성공적으로 생성한다.")
    void createTask_Success_WithoutFile() {
        // given (상황 셋업)
        TaskCreateRequest request = TaskCreateRequest.builder()
                .name("테스트 업무")
                .description("내용")
                .build();

        given(memberRepository.findById(memberId)).willReturn(Optional.of(testMember));

        TaskMaster savedTaskMaster = TaskMaster.builder().name("테스트 업무").member(testMember).build();
        given(taskMasterRepository.save(any(TaskMaster.class))).willReturn(savedTaskMaster);

        TaskResponse response = taskService.createTask(request, null, memberId);

        assertThat(response).isNotNull();
        verify(taskMasterRepository).save(any(TaskMaster.class));
        verify(fileStorageService, never()).upload(any());
        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("첨부파일을 포함하여 테스크를 생성하면 코멘트도 함께 저장된다.")
    void createTask_Success_WithFile() {
        // given
        TaskCreateRequest request = TaskCreateRequest.builder()
                .name("파일 첨부 업무")
                .build();

        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "test.png", "image/png", "test data".getBytes()
        );

        FileDto mockFileDto = FileDto.builder()
                .originalFileName("test.png")
                .savedFileName("uuid-test.png")
                .fileUrl("/uploads/uuid-test.png")
                .build();

        given(memberRepository.findById(memberId)).willReturn(Optional.of(testMember));

        TaskMaster savedTaskMaster = TaskMaster.builder().name("파일 첨부 업무").member(testMember).build();
        given(taskMasterRepository.save(any(TaskMaster.class))).willReturn(savedTaskMaster);

        given(fileStorageService.upload(mockFile)).willReturn(mockFileDto);

        TaskResponse response = taskService.createTask(request, mockFile, memberId);

        assertThat(response).isNotNull();
        verify(taskMasterRepository).save(any(TaskMaster.class));

        verify(fileStorageService).upload(mockFile);
        //verify(commentRepository).save(any(Comment.class));
        verify(taskMasterRepository).save(argThat(task -> {
            boolean hasComment = !task.getComments().isEmpty();
            if (!hasComment) return false;

            Comment innerComment = task.getComments().get(0);
            boolean isContentMatch = "첨부파일이 등록되었습니다.".equals(innerComment.getContent());
            boolean hasAttachment = !innerComment.getAttachments().isEmpty();

            return isContentMatch && hasAttachment;
        }));
    }
}