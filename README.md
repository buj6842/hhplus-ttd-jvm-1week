# 동시성 제어 방식에 대한 분석 및 보고서

## 1. 동시성을 어떻게 바라보고 해결하였는가
동시성이라는 단어만 보았을때 처음에 직렬이란 키워드와 같은 의미가 아닌가 하는 생각이 먼저들었었다.

과제의 Step1에는 동시에 여러 요청이 들어오더라도 순서대로 혹은 한번의 하나의 요청되도록 제어할 수 있게 리팩토링 이 핵심 키워드인데.

이 과정에서 Synchronized 키워드를 제일 먼저 떠올려 그냥 붙이면 된다 생각을했었지만.

과제에 제시된 UserPointTable 클래스 즉 DB 와 같은개념으로 사용하는 클래스에서

Map을 이용하기 때문에 동시에 같은 id에 대한 요청이 들어온다면 synchronized 키워드를 사용하더라도

완벽히 제어가 가능할까? 라는 생각이 들어 Lock을 사용하는 방법을 선택하게되었다.

## 2. 코드 작성 및 테스트 방식

#### 2.1 코드

```
@Service
public class PointServiceImpl implements PointService{
    private final PointHistoryTable pointHistoryTable;
    private final UserPointTable userPointTable;
    private final ConcurrentHashMap<Long, Lock> lockMap = new ConcurrentHashMap<>();


    public PointServiceImpl(PointHistoryTable pointHistoryTable, UserPointTable userPointTable) {
        this.pointHistoryTable = pointHistoryTable;
        this.userPointTable = userPointTable;
    }

    @Override
    public UserPoint addPoint(long id, long amount) {
        Lock lock = lockMap.computeIfAbsent(id, userId -> new ReentrantLock());
        lock.lock();

        try {

            // 유저의 포인트 조회
            UserPoint userPoint = userPointTable.selectById(id);
            // 포인트 충전을 하기 위한 검증
            UserPoint afterPoint = userPoint.validate(amount);
            userPointTable.insertOrUpdate(afterPoint.id(), afterPoint.point());
            // 포인트 사용 내역 테이블에 내역 저장
            pointHistoryTable.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());

            return afterPoint;
        } finally {
            lock.unlock();
        }
    }
```

ConcurrentHashMap 를 이용한 lockMap 이라는 객체를 선언하여 
computeIfAbsent(id, userId -> new ReentrantLock());
부분을 통해 Map에 id라는 값이 존재하지 않을 때 ReentrantLock 객체를 새로만들고, 이미 존재하는 id 의경우에는 기존 lock을 이용하도록 유도하여 설계를하였다.

#### 2.2 테스트 코드


```

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
```

테스트 케이스는 한번에 inlatch 들이 countdown 이 되며 깨어났을 때 동시성에 대한 이슈가 없이 처리 되는지가? 에 대해 작성하게되었다.

## 3 테스트 결과내용
동시성 제어 적용 하기 이전 테스트
```
충전이 되었습니다. 현재 유저의 충전된 포인트 = 1100 userId = 1 현재시간 = 1727395176809
충전이 되었습니다. 현재 유저의 충전된 포인트 = 1200 userId = 1 현재시간 = 1727395176884
포인트 사용이 되었습니다. 현재 유저의 잔여 포인트 = 950 userId = 1 현재시간 = 1727395176796
포인트 사용이 되었습니다. 현재 유저의 잔여 포인트 = 900 userId = 1 현재시간 = 1727395176926
충전이 되었습니다. 현재 유저의 충전된 포인트 = 1050 userId = 1 현재시간 = 1727395176913
충전이 되었습니다. 현재 유저의 충전된 포인트 = 1100 userId = 1 현재시간 = 1727395176799
포인트 사용이 되었습니다. 현재 유저의 잔여 포인트 = 900 userId = 1 현재시간 = 1727395176932
충전이 되었습니다. 현재 유저의 충전된 포인트 = 1200 userId = 1 현재시간 = 1727395176865
포인트 사용이 되었습니다. 현재 유저의 잔여 포인트 = 1050 userId = 1 현재시간 = 1727395176861
포인트 사용이 되었습니다. 현재 유저의 잔여 포인트 = 900 userId = 1 현재시간 = 1727395176957
포인트 사용이 되었습니다. 현재 유저의 잔여 포인트 = 850 userId = 1 현재시간 = 1727395177260
충전이 되었습니다. 현재 유저의 충전된 포인트 = 1000 userId = 1 현재시간 = 1727395177420
포인트 사용이 되었습니다. 현재 유저의 잔여 포인트 = 1000 userId = 1 현재시간 = 1727395177211
포인트 사용이 되었습니다. 현재 유저의 잔여 포인트 = 850 userId = 1 현재시간 = 1727395177465
포인트 사용이 되었습니다. 현재 유저의 잔여 포인트 = 850 userId = 1 현재시간 = 1727395177279
충전이 되었습니다. 현재 유저의 충전된 포인트 = 1000 userId = 1 현재시간 = 1727395177381
충전이 되었습니다. 현재 유저의 충전된 포인트 = 950 userId = 1 현재시간 = 1727395177517
충전이 되었습니다. 현재 유저의 충전된 포인트 = 1000 userId = 1 현재시간 = 1727395177381
포인트 사용이 되었습니다. 현재 유저의 잔여 포인트 = 850 userId = 1 현재시간 = 1727395177365
충전이 되었습니다. 현재 유저의 충전된 포인트 = 1000 userId = 1 현재시간 = 1727395177345
```

동시성 제어를 진행하였을 경우(service 에 동시성 제어 코드 삽입 후 테스트 진행)
```
충전이 되었습니다. 현재 유저의 충전된 포인트 = 1100 userId = 1 현재시간 = 1727395065651
충전이 되었습니다. 현재 유저의 충전된 포인트 = 1200 userId = 1 현재시간 = 1727395066271
충전이 되었습니다. 현재 유저의 충전된 포인트 = 1300 userId = 1 현재시간 = 1727395066943
충전이 되었습니다. 현재 유저의 충전된 포인트 = 1400 userId = 1 현재시간 = 1727395067284
충전이 되었습니다. 현재 유저의 충전된 포인트 = 1500 userId = 1 현재시간 = 1727395067644
포인트 사용이 되었습니다. 현재 유저의 잔여 포인트 = 1450 userId = 1 현재시간 = 1727395067886
포인트 사용이 되었습니다. 현재 유저의 잔여 포인트 = 1400 userId = 1 현재시간 = 1727395068084
포인트 사용이 되었습니다. 현재 유저의 잔여 포인트 = 1350 userId = 1 현재시간 = 1727395068643
포인트 사용이 되었습니다. 현재 유저의 잔여 포인트 = 1300 userId = 1 현재시간 = 1727395068988
포인트 사용이 되었습니다. 현재 유저의 잔여 포인트 = 1250 userId = 1 현재시간 = 1727395069293
충전이 되었습니다. 현재 유저의 충전된 포인트 = 1350 userId = 1 현재시간 = 1727395069868
포인트 사용이 되었습니다. 현재 유저의 잔여 포인트 = 1300 userId = 1 현재시간 = 1727395070319
포인트 사용이 되었습니다. 현재 유저의 잔여 포인트 = 1250 userId = 1 현재시간 = 1727395070711
충전이 되었습니다. 현재 유저의 충전된 포인트 = 1350 userId = 1 현재시간 = 1727395070847
포인트 사용이 되었습니다. 현재 유저의 잔여 포인트 = 1300 userId = 1 현재시간 = 1727395071309
충전이 되었습니다. 현재 유저의 충전된 포인트 = 1400 userId = 1 현재시간 = 1727395071500
포인트 사용이 되었습니다. 현재 유저의 잔여 포인트 = 1350 userId = 1 현재시간 = 1727395071637
충전이 되었습니다. 현재 유저의 충전된 포인트 = 1450 userId = 1 현재시간 = 1727395071957
포인트 사용이 되었습니다. 현재 유저의 잔여 포인트 = 1400 userId = 1 현재시간 = 1727395072478
충전이 되었습니다. 현재 유저의 충전된 포인트 = 1500 userId = 1 현재시간 = 1727395072877
```

디버깅 이외 눈으로 결과를 확인하고 싶어 작성한 로깅부분을 결과로 가져왔다

가장 큰 차이점을 비교하자면 제어코드 작성 이전 코드는
3번째 줄에서 포인트 사용이 되었는데 잔액이 1150이 아닌 950 으로 저장이 되었으며
결과를 볼 수록 충전 - 사용을 계속 진행하였을때 해당 순서에대해 처리가 되는게 아닌
사용을 했을때 User의 포인트를 가져온 시점에서 차감이 이루어지고 해당 결과값을 저장하게 되어
예상하는 값(1500) 까지 도출이 되지 않았다는 점이다.

실제 테스트코드의 검증 단계에서 적용전 에서는 test fail, 적용 후에는 test success라는 결과가 나오게 되었다.

## 4.결론
테스트 환경이 우선 Map이라는 데이터 이기에 직접적으로 내가 좀 더 동시성을 구현하기 위해 Lock을 채용하였으며
그과정에서 ConcurrentHashMap 를 사용하여 userId 에 초점을 맞춰 진행하니 어느정도의 순서나 동시성이 처리된것을 확인할 수 있었다.
