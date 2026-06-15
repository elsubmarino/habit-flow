package io.streak.habitflow.domain.project.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectInviteRequest {
    private Long id;

    @Builder.Default
    private List<String> emails = new ArrayList<>();
}
