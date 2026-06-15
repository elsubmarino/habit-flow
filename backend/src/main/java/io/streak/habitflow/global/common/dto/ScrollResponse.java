package io.streak.habitflow.global.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.function.Function;

@Getter
public class ScrollResponse<T> {
    private final List<T> content;
    private final boolean hasNext;
    private final Long nextCursor;

    public ScrollResponse(List<T> content, boolean hasNext, Long nextCursor) {
        this.content= content;
        this.hasNext = hasNext;
        this.nextCursor = nextCursor;
    }

    public static <T> ScrollResponse<T> of(List<T> content, int pageSize, Function<T, Long> idExtractor) {
        boolean hasNext = false;
        List<T> slicedContent = content;

        if(slicedContent.size() > pageSize){
            hasNext = true;
            slicedContent = content.subList(0, pageSize);
        }

        Long nextCursor = null;
        if(!slicedContent.isEmpty() && idExtractor != null){
            nextCursor = idExtractor.apply(slicedContent.get(slicedContent.size()-1));
        }
        return new ScrollResponse<>(slicedContent, hasNext,nextCursor);
    }
}
