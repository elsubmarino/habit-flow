package io.streak.habitflow.global.common;

public record RoutingId(Long value) {
    public static RoutingId of(Long value){
        return new RoutingId(value);
    }
}
