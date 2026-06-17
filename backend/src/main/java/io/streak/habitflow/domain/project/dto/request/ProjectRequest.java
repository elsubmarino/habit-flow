package io.streak.habitflow.domain.project.dto.request;

import io.streak.habitflow.domain.project.type.AccessType;
import io.streak.habitflow.domain.project.type.LayoutType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ProjectRequest {
    public record Create(
            String name,
            String color,
            Long parentId,
            AccessType accessType,
            boolean favorite,
            LayoutType layoutType
    ){}

    public record Invite(
            Long id,
            List<String>emails
    ){
        public Invite{
            emails = Objects.requireNonNullElse(emails, new ArrayList<>());
        }
    }
}
