package io.streak.habitflow.domain.search.service;

import io.streak.habitflow.domain.label.dto.response.LabelListResponse;
import io.streak.habitflow.domain.label.repository.LabelRepository;
import io.streak.habitflow.domain.project.dto.response.ProjectListResponse;
import io.streak.habitflow.domain.project.repository.ProjectRepository;
import io.streak.habitflow.domain.search.dto.response.IntegratedSearchResponse;
import io.streak.habitflow.domain.task.dto.response.TaskResponse;
import io.streak.habitflow.domain.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IntegratedSearchService {
    private final ProjectRepository projectRepository;
    private final LabelRepository labelRepository;
    private final TaskRepository taskRepository;

    public IntegratedSearchResponse searchAll(String keyword, UserDetails userDetails) {

        List<ProjectListResponse> projectListResponses = projectRepository.searchKeyword(keyword, userDetails.getUsername());
        List<TaskResponse> taskResponses = taskRepository.searchKeyword(keyword, userDetails.getUsername());
        List<LabelListResponse> labelListResponses = labelRepository.searchKeyword(keyword, userDetails.getUsername());

        return IntegratedSearchResponse.builder()
                .projects(projectListResponses)
                .tasks(taskResponses)
                .labels(labelListResponses)
                .build();
    }
}
