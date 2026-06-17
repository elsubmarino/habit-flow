package io.streak.habitflow.domain.project.event;

import java.util.List;

public record ProjectInvitationEvent(
        Long ProjectId,
        String projectName,
        Long InviterId,
        String InviterName,
        List<MemberInfo> invitees
) {
    public record MemberInfo(Long id, String name){

    }
}
