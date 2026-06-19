package io.streak.habitflow.domain.project.event;

public record ProjectAcceptEvent(
        Long ProjectId,
        String projectName,
        Long InviteeId,
        String InviteeName,
        Long InviterId,
        String InviterName
) {
}
