package io.streak.habitflow.domain.project.api;

import io.streak.habitflow.domain.project.dto.ProjectRequest;
import io.streak.habitflow.domain.project.dto.ProjectResponse;
import io.streak.habitflow.domain.project.entity.Project;
import io.streak.habitflow.domain.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
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
     * @param projectRequest
     * @param userDetails
     * @return
     */
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getProjects(@RequestBody ProjectRequest projectRequest,
                                                             @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(projectService.getProjects(projectRequest,userDetails));
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
}
