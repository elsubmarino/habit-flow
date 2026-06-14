//package io.streak.habitflow.domain.task.repository;
//
//import io.streak.habitflow.domain.comment.entity.Comment;
//import io.streak.habitflow.domain.comment.repository.CommentRepository;
//import io.streak.habitflow.domain.member.entity.Member;
//import io.streak.habitflow.domain.member.repository.MemberRepository;
//import io.streak.habitflow.domain.member.type.Role;
//import io.streak.habitflow.domain.task.dto.request.TaskUpdateRequest;
//import io.streak.habitflow.domain.task.entity.TaskMaster;
//import io.streak.habitflow.global.config.JpaAuditingConfig;
//import io.streak.habitflow.global.config.QuerydslConfig;
//import jakarta.persistence.EntityManager;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import org.springframework.context.annotation.Import;
//
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
//
//
//@DataJpaTest
//@Import({QuerydslConfig.class, JpaAuditingConfig.class})
//class TaskMasterRepositoryCustomImplTest {
//
//    @Autowired private TaskMasterMasterRepository taskMasterRepository;
//    @Autowired private MemberRepository memberRepository;
//    @Autowired private CommentRepository commentRepository;
//
//    @Autowired private EntityManager em;
//
//    private TaskMaster testTaskMaster;
//
//    @BeforeEach
//    void setUp(){
//        Member testMember = Member.builder().email("test@test.com")
//                .password("1234")
//                .role(Role.USER)
//                .build();
//        memberRepository.save(testMember);
//
//        testTaskMaster = TaskMaster.builder().name("스프링 부트 복습").description("Query DSL").member(testMember).build();
//
//        taskMasterRepository.save(testTaskMaster);
//        taskMasterRepository.save(TaskMaster.builder().name("리액트 복습").description("프론트엔드").member(testMember).parent(testTaskMaster).build());
//        taskMasterRepository.save(TaskMaster.builder().name("운동하기").description("헬스장가기").member(testMember).parent(testTaskMaster).build());
//
//        for(int i=0;i<3;i++){
//            commentRepository.save(Comment.builder()
//                    .content("코멘트"+i)
//                    .task(testTaskMaster)
//                    .member(testMember)
//                    .build());
//        }
//
//        em.flush();
//        em.clear();
//
//    }
//
//    @Test
//    @DisplayName("서브 테스크 등록")
//    void create_SubTask(){
//        Long targetId = testTaskMaster.getId();
//
//        Optional<TaskMaster> resultOpt = taskMasterRepository.searchTaskInfo(targetId);
//
//        assertThat(resultOpt).isPresent();
//        TaskMaster result = resultOpt.orElseThrow(()->new AssertionError("테스크 결과가 존재하지 않습니다."));
//        TaskMaster subTaskMaster = TaskMaster.builder()
//                .name("서브테스크")
//                .parent(result)
//                .build();
//
//        taskMasterRepository.save(subTaskMaster);
//
//    }
//
//
//
//    @Test
//    @DisplayName("코멘트 불러들임")
//    void searchTaskInfo_WithComments(){
//        Long targetId = testTaskMaster.getId();
//
//        Optional<TaskMaster> resultOpt = taskMasterRepository.searchTaskInfo(targetId);
//
//        assertThat(resultOpt).isPresent();
//        TaskMaster result = resultOpt.orElseThrow(()->new AssertionError("테스크 결과가 존재하지 않습니다."));
//        assertThat(result.getName()).isEqualTo("스프링 부트 복습");
//        assertThat(result.getDescription()).isEqualTo("Query DSL");
//
//        assertThat(result.getComments()).hasSize(3);
//        assertThat(result.getComments().get(0).getContent()).contains("코멘트");
//
//        assertThat(result.getSubTaskMasters()).hasSize(2);
//    }
//
//    @Test
//    @DisplayName("제목에 복습인 애들만 검색한다.")
//    void searchTasksByTitle(){
//        TaskUpdateRequest taskUpdateRequest = TaskUpdateRequest.builder()
//                .name("복습").build();
//
//        List<TaskMaster> result = taskMasterRepository.searchTasks(taskUpdateRequest,"test@test.com");
//
//        assertThat(result).hasSize(2);
//        assertThat(result).extracting("name")
//                .containsExactlyInAnyOrder("스프링 부트 복습","리액트 복습");
//    }
//
//}