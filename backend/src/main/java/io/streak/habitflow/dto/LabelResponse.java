package io.streak.habitflow.dto;

import io.streak.habitflow.entity.Label;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LabelResponse {
    private Long id;
    private String name;
    private long sortOrder;

    private String userId;
    private String userName;

    public static LabelResponse from(Label label){
        LabelResponseBuilder labelResponseBuilder = LabelResponse.builder()
                .id(label.getId())
                .name(label.getName())
                .sortOrder(label.getSortOrder());

        if(label.getUser() != null){
            labelResponseBuilder.userId(label.getUser().getUserId())
                    .userName(label.getUser().getUserName());
        }

        return labelResponseBuilder.build();
    }
}
