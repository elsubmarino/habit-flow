package io.streak.habitflow.domain.activity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogSearchCondition {
    private List<Long> projectIds;
    private List<Long> userIds;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate targetDate;

}
