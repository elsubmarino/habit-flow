package io.streak.habitflow.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LabelRequest {
    private String name;
}
