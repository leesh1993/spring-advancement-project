package hello.advanced.app.v1;

import hello.advanced.trace.TraceStatus;
import hello.advanced.trace.hellotrace.HelloTraceV1;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service// 컴포넌트 스캔의 대상이 된다
@RequiredArgsConstructor//초기화 되지않은 final 필드나, @NonNull 이 붙은 필드에 대해 생성자를 생성한다.
public class OrderServiceV1 {

    private final OrderRepositoryV1 orderRepository;
    private final HelloTraceV1 trace;

    public void orderItem(String itemId) {
        //업무 로직 생략..

        TraceStatus status = null;
        try {
            status = trace.begin("OrderService.orderItem()");
            orderRepository.save(itemId);
            trace.end(status);
        }catch (Exception e){
            trace.exception(status, e);
            throw e;// 예외를 꼭 다시 던져주어야 한다.
        }


    }
}