package io.streak.habitflow.domain.task.service;

import io.streak.habitflow.common.security.annotation.CheckOwnership;
import io.streak.habitflow.domain.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskService {
    private final TaskRepository taskRepository;

    @CheckOwnership(type="TASK")
    public void deleteTask(Long id, UserDetails userDetails){
        taskRepository.deleteById(id);
    }
}
