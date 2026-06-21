package io.streak.habitflow.domain.project.dto.response;

import io.streak.habitflow.domain.project.dto.query.ProjectSearchSummaryQuery;
import io.streak.habitflow.domain.project.dto.query.ProjectSummaryQuery;
import io.streak.habitflow.domain.project.entity.Project;
import io.streak.habitflow.domain.project.entity.ProjectMember;
import io.streak.habitflow.domain.project.type.AccessType;
import io.streak.habitflow.domain.project.type.LayoutType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public final class ProjectResponse {
    @Builder
    public record Detail(
            @Schema(description = "프로젝트 ID")//TODO
            String id,

            @Schema(description = "프로젝트명 (100자 이하)")
            String name,

            @Schema(description = "색상(헥사코드로 입력)",example = "#123456")
            String color,

            @Schema(description = "즐겨찾기 여부")
            boolean favorite,

            @Schema(description = "프로젝트의 상위 프로젝트 ID", example = "1")
            Long parentId,

            @Schema(description = "프로젝트의 상위 프로젝트의 명칭")
            String parentName,

            @Schema(description = "접근 제어자 (PUBLIC/PRIVATE)",examples = {"PUBLIC","PRIVATE"})
            AccessType accessType,

            @Schema(description = "레이아웃 (리스트형, 보드형, 달력형)",examples = {"LIST","BOARD","CALENDAR"})
            LayoutType layoutType
    ){
        public static Detail of(Project project, boolean favorite, String encodedId) {
            DetailBuilder builder = Detail.builder()
                    .id(encodedId)
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
            @Schema(description = "프로젝트 ID",example="추후업데이트") //TODO
            String id,

            @Schema(description = "프로젝트명 (100자 이하)")
            String name,

            @Schema(description = "색상(헥사코드로 입력)",example = "#123456")
            String color,

            @Schema(description = "해당 프로젝트에 속한 테스크의 수")
            long taskCount
    ){
//        public static Summary from(Project project) {
//            return Summary.builder()
//                    .id(project.getId())
//                    .name(project.getName())
//                    .color(project.getColor())
//                    .build();
//        }

//        public static Summary from(ProjectSummaryQuery projectSummaryQuery) {
//            return Summary.builder()
//                    .id(projectSummaryQuery.id())
//                    .name(projectSummaryQuery.name())
//                    .color(projectSummaryQuery.color())
//                    .build();
//        }

        public static Summary ofSearch(ProjectSearchSummaryQuery projectSummaryQuery, String encodedId) {
            return Summary.builder()
                    .id(encodedId)
                    .name(projectSummaryQuery.name())
                    .color(projectSummaryQuery.color())
                    .build();
        }

        public static Summary of(ProjectSummaryQuery projectSummaryQuery, String encodedId) {
            return Summary.builder()
                    .id(encodedId)
                    .name(projectSummaryQuery.name())
                    .color(projectSummaryQuery.color())
                    .build();
        }
    }

    @Builder
    public record Member(
            @Schema(description = "프로젝트에 속한 멤버의 이름")
            String memberName,
            @Schema(description = "프로젝트에 속한 멤버의 이메일주소")
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
