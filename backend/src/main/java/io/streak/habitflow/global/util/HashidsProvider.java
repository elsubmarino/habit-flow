package io.streak.habitflow.global.util;

import lombok.RequiredArgsConstructor;
import org.hashids.Hashids;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HashidsProvider {
    private final Hashids hashids;

    public String encode(Long id){
        if(id==null) return null;
        return hashids.encode(id);
    }

    public Long decode(String hash){
        if(hash==null|| hash.isBlank()) return null;
        long[] decoded = hashids.decode(hash);
        if(decoded.length == 0){
            throw new IllegalArgumentException("유요하지 앟은 식별자 포맷입니다.");
        }
        return decoded[0];
    }
}
