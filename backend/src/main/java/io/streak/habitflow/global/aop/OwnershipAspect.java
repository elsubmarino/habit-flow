package io.streak.habitflow.global.aop;

import io.streak.habitflow.domain.task.entity.Task;
import io.streak.habitflow.domain.task.repository.TaskRepository;
import io.streak.habitflow.global.security.dto.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;


@Aspect
@Component
@RequiredArgsConstructor
public class OwnershipAspect {

    private final TaskRepository taskRepository;

    @Before(value = "@annotation(checkOwnership) && args(taskId, userPrincipal, ..)", argNames = "joinPoint,checkOwnership,taskId,userPrincipal")
    public void validateOwnership(JoinPoint joinPoint, CheckOwnership checkOwnership, Long taskId, UserPrincipal userPrincipal) throws AccessDeniedException {
        String domainType = checkOwnership.type();

        if("TASK".equals(domainType)){
            Task task = taskRepository.findById(taskId)
                    .orElseThrow(()->new IllegalArgumentException("존재하지 않는 테스크입니다."));

            if(!task.getMember().getId().equals(userPrincipal.getMemberId())){
                throw new AccessDeniedException("해당 자원에 대한 권한이 없습니다.");
            }
        }
    }
}
