package io.streak.habitflow.domain.task.repository;

import io.streak.habitflow.domain.comment.entity.Comment;
import io.streak.habitflow.domain.comment.repository.CommentRepository;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.task.dto.TaskRequest;
import io.streak.habitflow.domain.task.entity.Task;
import io.streak.habitflow.global.config.JpaAuditingConfig;
import io.streak.habitflow.global.config.QuerydslConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


@DataJpaTest
@Import({QuerydslConfig.class, JpaAuditingConfig.class})
class TaskRepositoryCustomImplTest {

    @Autowired private TaskRepository taskRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private CommentRepository commentRepository;

    @Autowired private EntityManager em;

    private Task testTask;

    @BeforeEach
    void setUp(){
        Member testMember = Member.builder().email("test@test.com")
                .password("1234")
                .build();
        memberRepository.save(testMember);

        testTask = Task.builder().title("스프링 부트 복습").description("Query DSL").member(testMember).build();

        taskRepository.save(testTask);
        taskRepository.save(Task.builder().title("리액트 복습").description("프론트엔드").member(testMember).parent(testTask).build());
        taskRepository.save(Task.builder().title("운동하기").description("헬스장가기").member(testMember).parent(testTask).build());

        for(int i=0;i<3;i++){
            commentRepository.save(Comment.builder()
                    .content("코멘트"+i)
                    .task(testTask)
                    .member(testMember)
                    .build());
        }

        em.flush();
        em.clear();

    }

    @Test
    @DisplayName("서브 테스크 등록")
    void create_SubTask(){
        Long targetId = testTask.getId();

        Optional<Task> resultOpt = taskRepository.searchTaskInfo(targetId);

        assertThat(resultOpt).isPresent();
        Task result = resultOpt.orElseThrow(()->new AssertionError("테스크 결과가 존재하지 않습니다."));
        Task subTask = Task.builder()
                .title("서브테스크")
                .parent(result)
                .build();

        taskRepository.save(subTask);

    }



    @Test
    @DisplayName("코멘트 불러들임")
    void searchTaskInfo_WithComments(){
        Long targetId = testTask.getId();

        Optional<Task> resultOpt = taskRepository.searchTaskInfo(targetId);

        assertThat(resultOpt).isPresent();
        Task result = resultOpt.orElseThrow(()->new AssertionError("테스크 결과가 존재하지 않습니다."));
        assertThat(result.getTitle()).isEqualTo("스프링 부트 복습");
        assertThat(result.getDescription()).isEqualTo("Query DSL");

        assertThat(result.getComments()).hasSize(3);
        assertThat(result.getComments().get(0).getContent()).contains("코멘트");

        assertThat(result.getSubTasks()).hasSize(2);
    }

    @Test
    @DisplayName("제목에 복습인 애들만 검색한다.")
    void searchTasksByTitle(){
        TaskRequest taskRequest = TaskRequest.builder()
                .title("복습").build();

        List<Task> result = taskRepository.searchTasks(taskRequest);

        assertThat(result).hasSize(2);
        assertThat(result).extracting("title")
                .containsExactlyInAnyOrder("스프링 부트 복습","리액트 복습");
    }

}