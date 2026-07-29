package io.streak.habitflow.global.security.auth;

import io.streak.habitflow.domain.task.repository.TaskRepository;
import io.streak.habitflow.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("taskAuth")
@RequiredArgsConstructor
public class TaskAuth {
    private final TaskRepository taskRepository;

    public boolean canAccess(UUID publicTaskId){
        Long memberId = SecurityUtils.currentMemberId();
        return taskRepository.existsByPublicIdAndHasAccess(publicTaskId, memberId);
    }
}
