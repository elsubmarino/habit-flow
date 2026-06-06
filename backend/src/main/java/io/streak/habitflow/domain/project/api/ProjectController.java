package io.streak.habitflow.domain.project.api;

import io.streak.habitflow.domain.project.dto.ProjectRequest;
import io.streak.habitflow.domain.project.dto.ProjectResponse;
import io.streak.habitflow.domain.project.service.ProjectService;
import io.streak.habitflow.domain.task.dto.TaskResponse;
import io.streak.habitflow.domain.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService projectService;
    private final TaskService taskService;

    /**
     * 프로젝트 생성
     * @param projectRequest 프로젝트 요청 정보 DTO
     * @return 프로젝트 응답 정보 DTO
     */
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@RequestBody ProjectRequest projectRequest){
        return ResponseEntity.ok(projectService.createProject(projectRequest));
    }

    /**
     * 프로젝트 다건 조회
     * @param userDetails 인증된 사용자 정보
     * @return 프로젝트 응답 정보 DTO
     */
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getProjectsByMember(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(projectService.getProjectsByMember(userDetails));
    }

    /**
     * 프로젝트 업데이트
     * @param projectRequest 프로젝트 요청 정보 DTO
     * @param id 프로젝트 ID
     * @param userDetails 인증된 사용자 정보
     * @return 프로젝트 응답 정보 DTO
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(@RequestBody ProjectRequest projectRequest,@PathVariable Long id,
                                                         @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(projectService.updateProject(projectRequest,id,userDetails));
    }

    /**
     * 프로젝트 삭제
     * @param id 프로젝트 ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 프로젝트 안에 종속된 테스크 다건 조회
     * @param id 프로젝트 ID
     * @return 테스크 응답 정보 DTO
     */
    @GetMapping("/{id}/tasks")
    public ResponseEntity<List<TaskResponse>> getTasksByProject(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTasksByProject(id));
    }

    /**
     * 프로젝트 검색
     * @param userDetails 인증된 사용자 정보
     * @param keyword 검색 키워드
     * @return 프로젝트 응답 정보 DTO
     */
    @GetMapping("/search")
    public ResponseEntity<List<ProjectResponse>> searchProjects(@AuthenticationPrincipal UserDetails userDetails,
                                                            @RequestParam("keyword") String keyword){
        return ResponseEntity.ok(projectService.searchProjects(keyword,userDetails));
    }
}
