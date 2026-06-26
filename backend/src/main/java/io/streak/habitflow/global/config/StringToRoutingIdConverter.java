package io.streak.habitflow.global.config;

import io.streak.habitflow.global.common.RoutingId;
import io.streak.habitflow.global.util.HashidsProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StringToRoutingIdConverter implements Converter<String, RoutingId> {
    private final HashidsProvider hashidsProvider;
    @Override
    public RoutingId convert(String s) {
        Long decode = hashidsProvider.decode(s);
        return RoutingId.of(decode);
    }
}
