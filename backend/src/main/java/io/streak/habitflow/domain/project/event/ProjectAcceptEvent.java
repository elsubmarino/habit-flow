package io.streak.habitflow.domain.project.event;

public record ProjectAcceptEvent(
        Long projectId,
        String projectName,
        Long inviteeId,
        String inviteeName,
        Long inviterId,
        String inviterName
) {
}
