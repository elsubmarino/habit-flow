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
        ProjectResponse response = projectService.createProject(projectRequest);
        return ResponseEntity.ok(response);
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
        List<ProjectResponse> projectResponses = projectService.getProjects(projectRequest,userDetails);
        return ResponseEntity.ok(projectResponses);
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
        ProjectResponse projectResponse = projectService.updateProject(projectRequest,id,userDetails);
        return ResponseEntity.ok(projectResponse);
    }
}
