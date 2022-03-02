package hello.advanced.trace.logtrace;

import hello.advanced.trace.TraceId;
import hello.advanced.trace.TraceStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
public class FieldLogTrace implements LogTrace {

    //로그의 레벨을 나타낸다.
    private static final String START_PREFIX = "-->";
    private static final String COMPLETE_PREFIX = "<--";
    private static final String EX_PREFIX = "<X-";

    private TraceId traceIdHolder; //traceId 동기화, 동시성 이슈 발생..

    @Override
    public TraceStatus begin(String message) {
        syncTraceId();
        TraceId traceId = traceIdHolder;
        
        //시작시간
        Long startTimeMs = System.currentTimeMillis();

        //트랜잭션 ID, 트랜잭션 lever, 실행중인 메서드
        log.info("[{}] {}{}", traceId.getId(), addSpace(START_PREFIX, traceId.getLevel()), message);

        //로그 출력(상태 반환)
        return new TraceStatus(traceId, startTimeMs, message);
    }

    @Override
    public void end(TraceStatus status) {
        complete(status, null);
    }

    @Override
    public void exception(TraceStatus status, Exception e) {
        complete(status, e);
    }

    //end(), exception()의 요청 흐름을 한곳에서 편리하게 처리한다.
    //실행 시간을 측정하고 로그를 남긴다.
    private void complete(TraceStatus status, Exception e) {
        Long stopTimeMs = System.currentTimeMillis();
        long resultTimeMs = stopTimeMs - status.getStartTimeMs();
        TraceId traceId = status.getTraceId();
        if (e == null) {
            log.info("[{}] {}{} time={}ms", traceId.getId(), addSpace(COMPLETE_PREFIX, traceId.getLevel()), status.getMessage(), resultTimeMs);
        } else {
            log.info("[{}] {}{} time={}ms ex={}", traceId.getId(), addSpace(EX_PREFIX, traceId.getLevel()), status.getMessage(), resultTimeMs, e.toString());
        }
        
        releaseTraceId();
    }

    //하나의 트랜잭션의 레벨을 나타내기 위해 사용된다.
    private static String addSpace(String prefix, int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) {
            sb.append( (i == level - 1) ? "|" + prefix : "| ");
        }
        return sb.toString();
    }

    //traceId를 새로 만들거나 앞선 로그의 TraceId를 참고해서 동기화하고, level도 증가한다.
    private void syncTraceId() {
        //최초 호출이면 새로 생성(동시성 문제 : 객체 생성 전 접근 가능)
        if(traceIdHolder == null) {
            traceIdHolder = new TraceId();
        } else {
            //이미 있다면 level 증가
            traceIdHolder = traceIdHolder.createNextId();
        }
    }

    //메서드 호출 후 level 감소
    private void releaseTraceId() {
        // 0level일 경우 traceId 제거
        if(traceIdHolder.isFirstLever()) {
            traceIdHolder = null; //destroy;
        } else {
            traceIdHolder = traceIdHolder.createPreviousId();
        }
    }
}
