package io.streak.habitflow.domain.project.entity;

import io.streak.habitflow.common.jpa.BaseTimeEntity;
import io.streak.habitflow.domain.user.dto.UserResponse;
import io.streak.habitflow.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Project  extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String color;
    private long sortOrder;
}
