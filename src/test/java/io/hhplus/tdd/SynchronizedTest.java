package io.hhplus.tdd;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointServiceImpl;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class SynchronizedTest {

    @Autowired
    private PointServiceImpl pointService;
    @Autowired
    private UserPointTable userPointTable;
    @Autowired
    private PointHistoryTable pointHistoryTable;

    @DisplayName("동시성 충전 및 사용 테스트")
    @Test
    public void 동시성_충전_및_사용_테스트() throws InterruptedException {
        // 사용 할 유저(스레드) 의 수
        int userCount = 10;
        // 2개의 excutor.submit 을 사용하기에 countdown에 용이하도록 usercount x 2 를 진행
        CountDownLatch latch = new CountDownLatch(userCount * 2);
        // 스레드를 생성 후 대기를 1만큼 하도록 생성 후 countdown 시 동시
        CountDownLatch inLatch = new CountDownLatch(1);
        // 초기 포인트 적립
        pointService.addPoint(1, 1000);
        // 10개의 스레드를 생성 후 사용
        ExecutorService executor = Executors.newFixedThreadPool(userCount);
        for (int i = 0; i < userCount; i++) {
            executor.submit(() -> {
                try {
                    // inlatch 대기하도록 설정
                    inLatch.await();
                    // 대기된 상태에서 포인트 사용
                    UserPoint firstUserUsePoint = pointService.usePoint(1, 50);
                    System.out.println("포인트 사용이 되었습니다. 현재 유저의 잔여 포인트 = " + firstUserUsePoint.point() + " userId = " + 1 + " 현재시간 = " + firstUserUsePoint.updateMillis());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            });
            executor.submit(() -> {
                try {
                    // inlatch 대기하도록 설정
                    inLatch.await();
                    // 대기된 상테에서 포인트 충전
                    UserPoint addPoint = pointService.addPoint(1, 100);
                    System.out.println("충전이 되었습니다. 현재 유저의 충전된 포인트 = " + addPoint.point() + " userId = " + 1 + " 현재시간 = " + addPoint.updateMillis());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            });
        }
        // 대기상태를 해제
        inLatch.countDown();
        //latch 대기상태로 변경
        latch.await();
        // 사용자의 포인트 조회
        UserPoint point = pointService.getPoint(1);

        // 결과 검증
        // point 를 더하고 뺀값의 최종값을 예상
        assertEquals(1500, point.point());
        // 사용 내역이 20개가 되어야함(충전 + 사용) + 초기의 한개 더한 값
        assertEquals(21, pointHistoryTable.selectAllByUserId(1).size());
    }
}