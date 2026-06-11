package io.streak.habitflow.domain.task.dto.request;

import io.streak.habitflow.domain.task.type.TaskPriorityType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "테스크 생성 요청 DTO")
public class TaskCreateRequest {

    @NotBlank(message = "제목은 필수 입력 항목입니다.")
    @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.")
    @Schema(description = "테스크 제목 (최대 100자)",requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "테스크 상세 설명")
    private String description;

    @Schema(description = "마감 기한 일시")
    private LocalDate dueDate;

    @Schema(description = "테스크 우선 순위 (P1~P4)")
    private TaskPriorityType taskPriorityType;

    @Schema(description = "소속 프로젝트 ID (소속 없을 시에 null 넘기면 인박스로 진입)")
    private Long projectId;

    @Schema(description = "부모 테스크 ID (하위 테스크 생성 시에 필수 채움)")
    private Long parentId;

    @Builder.Default
    @Schema(description = "매핑할 라벨 ID 리스트")
    private List<Long> labelIds = new ArrayList<>();

    private boolean recurring;
    private String recurrenceRule;
    private int recurrenceInterval;
    private String recurrenceDays;
    private Integer recurrenceDayOfMonth;
}
