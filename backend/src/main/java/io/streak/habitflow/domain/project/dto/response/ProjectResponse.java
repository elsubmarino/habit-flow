package io.streak.habitflow.domain.project.dto.response;

import io.streak.habitflow.domain.project.dto.query.ProjectSummaryQuery;
import io.streak.habitflow.domain.project.entity.Project;
import io.streak.habitflow.domain.project.entity.ProjectMember;
import io.streak.habitflow.domain.project.type.AccessType;
import io.streak.habitflow.domain.project.type.LayoutType;
import lombok.Builder;

public final class ProjectResponse {
    @Builder
    public record Detail(
            Long id,
            String name,
            String color,
            boolean favorite,
            Long parentId,
            String parentName,
            AccessType accessType,
            LayoutType layoutType
    ){
        public static Detail of(Project project, boolean favorite) {
            DetailBuilder builder = Detail.builder()
                    .id(project.getId())
                    .name(project.getName())
                    .color(project.getColor())
                    .layoutType(project.getLayoutType())
                    .accessType(project.getAccessType())
                    .favorite(favorite);
            if(project.getParent() != null){
                builder.parentId(project.getParent().getId());
                builder.parentName(project.getParent().getName());
            }
            return builder.build();
        }
    }

    @Builder
    public record Summary(
            Long id,
            String name,
            String color,
            long taskCount
    ){
        public static Summary from(Project project) {
            return Summary.builder()
                    .id(project.getId())
                    .name(project.getName())
                    .color(project.getColor())
                    .build();
        }

        public static Summary from(ProjectSummaryQuery projectSummaryQuery) {
            return Summary.builder()
                    .id(projectSummaryQuery.id())
                    .name(projectSummaryQuery.name())
                    .color(projectSummaryQuery.color())
                    .build();
        }
    }

    @Builder
    public record Member(
            String memberName,
            String email
    ){
        public static Member from(ProjectMember projectMember){
            return Member.builder()
                    .memberName(projectMember.getMember().getName())
                    .email(projectMember.getMember().getEmail())
                    .build();
        }
    }

}
