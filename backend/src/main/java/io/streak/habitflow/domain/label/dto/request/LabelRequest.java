package io.streak.habitflow.domain.label.dto.request;


public final class LabelRequest {
    public record Create(
            String name,
            String color,
            boolean favorite
    ){}

    public record Delete(
            Long id,
            String name,
            String color
    ){}

    public record Search(
            Long lastLabelId
    ){}

    public record Update(
            Long id,
            String name,
            String color,
            boolean favorite
    ){}
}
