package io.streak.habitflow.domain.search.service;

import io.streak.habitflow.domain.label.dto.query.LabelSummaryQuery;
import io.streak.habitflow.domain.label.dto.response.LabelResponse;
import io.streak.habitflow.domain.label.repository.LabelRepository;
import io.streak.habitflow.domain.project.dto.query.ProjectSummaryQuery;
import io.streak.habitflow.domain.project.dto.response.ProjectResponse;
import io.streak.habitflow.domain.project.repository.ProjectRepository;
import io.streak.habitflow.domain.search.dto.response.IntegratedResponse;
import io.streak.habitflow.domain.task.dto.query.TaskSummaryQuery;
import io.streak.habitflow.domain.task.dto.response.TaskResponse;
import io.streak.habitflow.domain.task.repository.TaskRepository;
import io.streak.habitflow.global.util.HashidsProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IntegratedSearchService {
    private final ProjectRepository projectRepository;
    private final LabelRepository labelRepository;
    private final TaskRepository taskRepository;
    private final HashidsProvider hashidsProvider;

    public IntegratedResponse.Search searchAll(String keyword, Long memberId, Pageable pageable) {

        List<ProjectSummaryQuery> projectListResponses = projectRepository.searchKeyword(keyword, memberId, pageable);
        List<TaskSummaryQuery> taskResponses = taskRepository.searchKeyword(keyword, memberId, pageable);
        List<LabelSummaryQuery> labelListResponses = labelRepository.searchKeyword(keyword,memberId, pageable);
        List<LabelResponse.Summary> summaries = labelListResponses.stream()
                .map(label->{
                    String encodedId = hashidsProvider.encode(label.id());
                    return LabelResponse.Summary.of(label,encodedId);
                })
                .toList();

        return IntegratedResponse.Search.builder()
                .projects(projectListResponses.stream().map(response->{
                    String encodedId = hashidsProvider.encode(response.id());
                    return ProjectResponse.Summary.of(response,encodedId);
                }).toList())
                .tasks(taskResponses.stream().map(response->{
                    String encodedId = hashidsProvider.encode(response.id());
                    return TaskResponse.Summary.of(response,encodedId, new ArrayList<>());
                }).toList())
                .labels(summaries)
                .build();
    }
}
