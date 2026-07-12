package io.streak.habitflow.domain.project.event;

public record ProjectAcceptedEvent(
        Long projectId,
        String projectName,
        Long inviteeId,
        String inviteeName,
        Long inviterId,
        String inviterName
) {
}
