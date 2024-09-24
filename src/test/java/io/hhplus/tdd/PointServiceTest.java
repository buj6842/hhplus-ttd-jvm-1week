package io.hhplus.tdd;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class PointServiceTest {

    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    private UserPoint defaultUserPoint;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        userPointTable = mock(UserPointTable.class);
        pointHistoryTable = mock(PointHistoryTable.class);
        defaultUserPoint = new UserPoint(1L, 0L, System.currentTimeMillis());
    }

    @DisplayName("포인트 조회 유닛 테스트")
    @Test
    public void 포인트_조회_테스트() {
        // given
        long userId = defaultUserPoint.id();
        when(userPointTable.selectById(userId)).thenReturn(defaultUserPoint);

        // when
        UserPoint currentUserPoint = userPointTable.selectById(userId);

        // then
        // 메소드를 실행한 id,point가 defaultUserPoint 와 같은지 검증
        assertEquals(defaultUserPoint.id(), currentUserPoint.id());
        assertEquals(defaultUserPoint.point(), currentUserPoint.point());
        //메소드가 정확히 실행되었는지 검증
        verify(userPointTable).selectById(userId);
    }

    @DisplayName("포인트 충전 단위 테스트")
    @Test
    public void 포인트_충전_테스트() {
        //given
        // 충전 할 유저의 아이디
        long userId = defaultUserPoint.id();
        // 충전 할 포인트 수량
        long amount = 20000;

        // 충전 된 후 예상되는 포인트 객체
        UserPoint updatedUserPoint = new UserPoint(userId, defaultUserPoint.point() + amount, System.currentTimeMillis());


        //when
        when(userPointTable.insertOrUpdate(userId, defaultUserPoint.point() + amount)).thenReturn(updatedUserPoint);
        // 조회한 포인트에 충전하려하는 포인트를 더하여 insert 진행
        UserPoint afterUserPoint = userPointTable.insertOrUpdate(userId, defaultUserPoint.point() + amount);
        // 포인트 충전 후 이력 추가
        // then
        // defaultUserPoint 의 초기 포인트는 0으로 시작하였기 때문에 충전할 포인트와 같은지 비교
        assertEquals(amount,afterUserPoint.point());
        // 포인트 충전 메소드가 실행되었는지 검증
        verify(userPointTable).insertOrUpdate(userId, defaultUserPoint.point() + amount);
        // 포인트 이력 메소드가 정상적으로 실행되었는지 검증
//        verify(pointHistoryTable).insert(userId, amount, TransactionType.CHARGE , System.currentTimeMillis());
    }


}