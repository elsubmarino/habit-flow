package io.streak.habitflow.domain.search.service;

import io.streak.habitflow.domain.label.dto.response.LabelResponse;
import io.streak.habitflow.domain.label.repository.LabelRepository;
import io.streak.habitflow.domain.project.dto.response.ProjectResponse;
import io.streak.habitflow.domain.project.repository.ProjectRepository;
import io.streak.habitflow.domain.search.dto.IntegratedSearchResponse;
import io.streak.habitflow.domain.task.dto.TaskResponse;
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

        List<ProjectResponse> projectResponses = projectRepository.searchKeyword(keyword, userDetails.getUsername());
        List<TaskResponse> taskResponses = taskRepository.searchKeyword(keyword, userDetails.getUsername());
        List<LabelResponse> labelResponses = labelRepository.searchKeyword(keyword, userDetails.getUsername());

        return IntegratedSearchResponse.builder()
                .projects(projectResponses)
                .tasks(taskResponses)
                .labels(labelResponses)
                .build();
    }
}
