package io.streak.habitflow.domain.notification.listener;

import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.notification.dto.response.NotificationResponse;
import io.streak.habitflow.domain.notification.entity.Notification;
import io.streak.habitflow.domain.notification.repository.NotificationRepository;
import io.streak.habitflow.domain.notification.type.NotificationType;
import io.streak.habitflow.domain.project.event.ProjectAcceptEvent;
import io.streak.habitflow.domain.project.event.ProjectInvitationEvent;
import io.streak.habitflow.domain.task.type.ActivityType;
import io.streak.habitflow.global.infra.sse.SseEmitters;
import io.streak.habitflow.global.util.HashidsProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {
    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    private final SseEmitters sseEmitters;
    private final HashidsProvider hashidsProvider;

    @Async("activityLogExecutor")
    @Transactional
    @EventListener
    public void handleProjectInvitation(ProjectInvitationEvent projectInvitationEvent){
        Member inviter = memberRepository.getReferenceById(projectInvitationEvent.inviterId());
        List<Notification> notificationBatch = new ArrayList<>();
        for(ProjectInvitationEvent.MemberInfo memberInfo: projectInvitationEvent.invitees()){
            Member receiver = memberRepository.getReferenceById(memberInfo.id());

            notificationBatch.add(Notification.builder()
                    .receiver(receiver)
                    .actor(inviter)
                    .targetId(projectInvitationEvent.projectId())
                    .notificationType(NotificationType.PROJECT)
                    .activityType(ActivityType.INVITED)
                    .customMessage(inviter.getName()+" 님이 ["+projectInvitationEvent.projectName()+"] 프로젝트에 당신을 초대했습니다.")
                    .isConfirmed(false)
                    .build()
            );
        }

        List<Notification> notifications = notificationRepository.saveAll(notificationBatch);


        for(Notification notification: notifications){
            if(notification.getActivityType() == ActivityType.INVITED){
                sseEmitters.sendToMember(notification.getReceiver().getId()
                        , NotificationResponse.Summary.of(notification,hashidsProvider.encode(notification.getId())
                                ,null,null,null));
            }
        }

    }

    @Async("activityLogExecutor")
    @Transactional
    @EventListener
    public void handleProjectAccept(ProjectAcceptEvent projectAcceptEvent){
        Member inviter = memberRepository.getReferenceById(projectAcceptEvent.inviterId());
        Member invitee = memberRepository.getReferenceById(projectAcceptEvent.inviteeId());

        Notification notification = Notification.builder()
                .receiver(inviter)
                .actor(invitee)
                .targetId(projectAcceptEvent.projectId())
                .notificationType(NotificationType.PROJECT)
                .activityType(ActivityType.JOINED)
                .customMessage(projectAcceptEvent.inviteeName()+" 님이 ["+projectAcceptEvent.projectName()+"] 프로젝트에 합류했습니다")
                .isConfirmed(false)
                .build();

        notificationRepository.save(notification);

        if(notification.getActivityType() == ActivityType.JOINED){
            sseEmitters.sendToMember(notification.getReceiver().getId()
                    , NotificationResponse.Summary.of(notification,hashidsProvider.encode(notification.getId()),null,null,null));
        }

    }
}
