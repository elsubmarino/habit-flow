package io.streak.habitflow.global.infra.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class SseEmitters {
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter add(Long memberId, SseEmitter emitter){
        // 덮어쓰기 전 이전 emitter를 받아서 명시적으로 종료
        SseEmitter old = this.emitters.put(memberId, emitter);
        if (old != null) {
            old.complete();
        }
        log.info("[SSE 연결 완료] -> memberId : {}, 현재 연결 수 : {}", memberId, emitters.size());

        emitter.onCompletion(()->{
            this.emitters.remove(memberId, emitter);
            log.info("[SSE 만료 청소] -> memberId: {}",memberId);
        });

        emitter.onTimeout(()->{
            emitter.complete();
            this.emitters.remove(memberId, emitter);
            log.info("[SSE 타임아웃 청소] -> memberId: {}",memberId);
        });

        emitter.onError((e)->{
            emitter.completeWithError(e);
            this.emitters.remove(memberId, emitter);
            log.info("[SSE 에러 발생] -> memberId: {}",memberId);
        });

        return emitter;
    }

    public void sendToMember(Long memberId, Object data){
        SseEmitter emitter = emitters.get(memberId);
        if(emitter != null){
            try{
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(data));
                log.info("[SSE 실시간 알림 푸시 성공] -> memberId: {}",memberId);
            }catch(Exception e){
                log.warn("[SSE 송신 실패로 인한 연결 해제] -> memberId: {}",memberId);
                emitters.remove(memberId);
            }
        }
    }
}
