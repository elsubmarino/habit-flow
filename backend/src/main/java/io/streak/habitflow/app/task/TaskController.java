package io.streak.habitflow.app.task;

import io.streak.habitflow.domain.task.dto.TaskRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    public ResponseEntity<Void> createTask(@AuthenticationPrincipal UserDetails userDetails,
                                           @RequestBody TaskRequest taskRequest){
        String email = userDetails.getUsername();
        return ResponseEntity.ok().build();
    }
}
