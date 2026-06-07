package io.streak.habitflow.domain.activity.dto.request;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogSearchCondition {
    private List<Long> projectIds;
    private List<Long> userIds;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate targetDate;

}
