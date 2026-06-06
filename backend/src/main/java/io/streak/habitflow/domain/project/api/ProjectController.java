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
@RequestMapping("/api/project")
public class ProjectController {
    private final ProjectService projectService;
    private final TaskService taskService;

    /**
     * 프로젝트 생성
     * @param projectRequest
     * @return
     */
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@RequestBody ProjectRequest projectRequest){
        return ResponseEntity.ok(projectService.createProject(projectRequest));
    }

    /**
     * 프로젝트 다건 조회
     * @param userDetails
     * @return
     */
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getProjects(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(projectService.getProjects(userDetails));
    }

    /**
     * 프로젝트 업데이트
     * @param projectRequest
     * @param id
     * @param userDetails
     * @return
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(@RequestBody ProjectRequest projectRequest,@PathVariable Long id,
                                                         @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(projectService.updateProject(projectRequest,id,userDetails));
    }

    /**
     * 프로젝트 삭제
     * @param id
     * @param userDetails
     * @return
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id,
                                              @AuthenticationPrincipal UserDetails userDetails) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 프로젝트 안에 종속된 테스크 다건 조회
     * @param userDetails
     * @param projectId
     * @return
     */
    @GetMapping("/{id}/task")
    public ResponseEntity<List<TaskResponse>> getTasksByProject(@AuthenticationPrincipal UserDetails userDetails,
                                                                @PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTasksByProject(id,userDetails));
    }
}
