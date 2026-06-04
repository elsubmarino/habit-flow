package io.streak.habitflow.domain.member.dto;

import io.streak.habitflow.domain.label.dto.LabelResponse;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.task.dto.TaskResponse;
import io.streak.habitflow.domain.task.entity.Task;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class MemberResponse {
    private Long id;
    private String userId;
    private String userName;
    private String email;
    private String role;

    public static MemberResponse from(Member member) {
        MemberResponseBuilder builder = MemberResponse.builder()
                .userId(member.getUserId())
                .userName(member.getUserName())
                .email(member.getEmail());
        return builder.build();
    };
}
