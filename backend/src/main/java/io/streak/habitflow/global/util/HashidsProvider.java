package io.streak.habitflow.global.util;

import io.streak.habitflow.global.error.ErrorCode;
import io.streak.habitflow.global.error.exception.BusinessException;
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
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        return decoded[0];
    }
}
