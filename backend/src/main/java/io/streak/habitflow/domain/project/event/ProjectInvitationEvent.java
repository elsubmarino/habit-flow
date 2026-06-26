package io.streak.habitflow.domain.project.event;

import java.util.List;

public record ProjectInvitationEvent(
        Long projectId,
        String projectName,
        Long inviterId,
        String inviterName,
        List<MemberInfo> invitees
) {
    public record MemberInfo(Long id, String name){

    }
}
