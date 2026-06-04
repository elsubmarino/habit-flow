package io.streak.habitflow.common.security;

import io.streak.habitflow.common.security.annotation.CheckOwnership;
import io.streak.habitflow.domain.task.entity.Task;
import io.streak.habitflow.domain.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.file.AccessDeniedException;

@Aspect
@Component
@RequiredArgsConstructor
public class OwnershipAspect {

    private final TaskRepository taskRepository;

    @Before(value = "@annotation(checkOwnership) && args(id, userDetails, ..)", argNames = "joinPoint,checkOwnership,id,userDetails")
    public void validateOwnership(JoinPoint joinPoint, CheckOwnership checkOwnership, Long id, UserDetails userDetails) throws AccessDeniedException {
        String currentEmail = userDetails.getUsername();
        String domainType = checkOwnership.type();

        if("TASK".equals(domainType)){
            Task task = taskRepository.findById(id)
                    .orElseThrow(()->new IllegalArgumentException("존재하지 않는 테스크입니다."));

            if(!task.getMember().getEmail().equals(currentEmail)){
                throw new AccessDeniedException("해당 자원에 대한 권한이 없습니다.");
            }
        }
    }
}
