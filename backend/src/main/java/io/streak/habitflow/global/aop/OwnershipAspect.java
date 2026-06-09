package io.streak.habitflow.global.aop;

import io.streak.habitflow.domain.task.entity.Task;
import io.streak.habitflow.domain.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;


@Aspect
@Component
@RequiredArgsConstructor
public class OwnershipAspect {

    private final TaskRepository taskRepository;

    @Before(value = "@annotation(checkOwnership) && args(taskId, userDetails, ..)", argNames = "joinPoint,checkOwnership,taskId,userDetails")
    public void validateOwnership(JoinPoint joinPoint, CheckOwnership checkOwnership, Long taskId, UserDetails userDetails) throws AccessDeniedException {
        String currentEmail = userDetails.getUsername();
        String domainType = checkOwnership.type();

        if("TASK".equals(domainType)){
            Task task = taskRepository.findById(taskId)
                    .orElseThrow(()->new IllegalArgumentException("존재하지 않는 테스크입니다."));

            if(!task.getMember().getEmail().equals(currentEmail)){
                throw new AccessDeniedException("해당 자원에 대한 권한이 없습니다.");
            }
        }
    }
}
