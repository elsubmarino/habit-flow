package io.streak.habitflow.domain.search.service;

import io.streak.habitflow.domain.label.dto.query.LabelSummaryQuery;
import io.streak.habitflow.domain.label.dto.response.LabelResponse;
import io.streak.habitflow.domain.label.repository.LabelRepository;
import io.streak.habitflow.domain.project.dto.query.ProjectSearchSummaryQuery;
import io.streak.habitflow.domain.project.dto.response.ProjectResponse;
import io.streak.habitflow.domain.project.repository.ProjectRepository;
import io.streak.habitflow.domain.search.dto.response.SearchResponse;
import io.streak.habitflow.domain.task.dto.query.TaskSearchSummaryQuery;
import io.streak.habitflow.domain.task.dto.response.TaskResponse;
import io.streak.habitflow.domain.task.repository.TaskRepository;
import io.streak.habitflow.global.util.HashidsProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IntegratedSearchService {
    private final ProjectRepository projectRepository;
    private final LabelRepository labelRepository;
    private final TaskRepository taskRepository;
    private final HashidsProvider hashidsProvider;

    public SearchResponse.SearchResult searchAll(String keyword, Long memberId, Pageable pageable) {

        List<ProjectSearchSummaryQuery> projectListResponses = projectRepository.searchByKeyword(keyword, memberId, pageable);
        List<TaskSearchSummaryQuery> taskResponses = taskRepository.searchByKeyword(keyword, memberId, pageable);
        List<LabelSummaryQuery> labelListResponses = labelRepository.searchByKeyword(keyword,memberId, pageable);
        List<LabelResponse.Summary> summaries = labelListResponses.stream()
                .map(label->{
                    return LabelResponse.Summary.of(label,label.publicId().toString());
                })
                .toList();

        return SearchResponse.SearchResult.builder()
                .projects(projectListResponses.stream().map(response->{
                    return ProjectResponse.Summary.ofSearch(response,response.publicId().toString());
                }).toList())
                .tasks(taskResponses.stream().map(response->{
                    return TaskResponse.Summary.of(response,response.publicId().toString());
                }).toList())
                .labels(summaries)
                .build();
    }
}
