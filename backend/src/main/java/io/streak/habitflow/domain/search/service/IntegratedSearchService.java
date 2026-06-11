package io.streak.habitflow.domain.search.service;

import io.streak.habitflow.domain.label.dto.response.LabelListResponse;
import io.streak.habitflow.domain.label.repository.LabelRepository;
import io.streak.habitflow.domain.project.dto.response.ProjectListResponse;
import io.streak.habitflow.domain.project.repository.ProjectRepository;
import io.streak.habitflow.domain.search.dto.response.IntegratedSearchResponse;
import io.streak.habitflow.domain.task.dto.response.TaskResponse;
import io.streak.habitflow.domain.task.repository.TaskMasterMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IntegratedSearchService {
    private final ProjectRepository projectRepository;
    private final LabelRepository labelRepository;
    private final TaskMasterMasterRepository taskMasterRepository;

    public IntegratedSearchResponse searchAll(String keyword, Long memberId, Pageable pageable) {

        List<ProjectListResponse> projectListResponses = projectRepository.searchKeyword(keyword, memberId, pageable);
        List<TaskResponse> taskResponses = taskMasterRepository.searchKeyword(keyword, memberId, pageable);
        List<LabelListResponse> labelListResponses = labelRepository.searchKeyword(keyword,memberId, pageable);

        return IntegratedSearchResponse.builder()
                .projects(projectListResponses)
                .tasks(taskResponses)
                .labels(labelListResponses)
                .build();
    }
}
