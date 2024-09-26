package io.hhplus.tdd;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PointTest {

    private UserPoint defaultUserPoint;
    private UserPointTable userPointTable;

    @BeforeEach
    public void setUp() {
        userPointTable = new UserPointTable();
        defaultUserPoint = new UserPoint(1, 5000, System.currentTimeMillis());
    }

    @DisplayName("포인트가 최대값을 초과하는 경우")
    @Test
    public void 포인트_최대값_초과_테스트() {
        // when & then
        // 포인트 최대값이 넘치게 충전하도록 유도하여 예외 발생 유도
        assertThrows(IllegalArgumentException.class, () -> defaultUserPoint.validate(996000), "포인트는 1,000,000(백만) 포인트를 초과할 수 없습니다.");
    }


    @DisplayName("정상적인 포인트 충전인 경우")
    @Test
    public void 포인트_정상_충전_테스트() {
        // when
        UserPoint updatedUserPoint = defaultUserPoint.validate(10000);

        // then
        // 정상적인 충전으로 5000 + 10000 = 15000 의 결과 예상
        assertEquals(15000, updatedUserPoint.point());
    }

    @DisplayName("포인트가 부족한 경우")
    @Test
    public void 포인트_부족_테스트() {
        // when & then
        // 포인트가 부족한 상황 연출하여 예외 발생하도록 유도
        assertThrows(IllegalArgumentException.class, () -> defaultUserPoint.validateUsage(10000), "포인트가 부족합니다.");
    }

    @DisplayName("사용할 포인트가 0보다 작은 경우")
    @Test
    public void 포인트_음수_사용_테스트() {
        // when & then
        // 음수 포인트 사용시 예외발생하도록 유도
        assertThrows(IllegalArgumentException.class, () -> defaultUserPoint.validateUsage(-10000), "사용할 포인트는 0보다 커야 합니다.");
    }

    @DisplayName("정상적인 포인트 사용인 경우")
    @Test
    public void 포인트_정상_사용_테스트() {
        // when
        // 3000점 포인트 사용 하도록 유도 (2000의 값이 나와야함)
        UserPoint updatedUserPoint = defaultUserPoint.validateUsage(3000);

        // then
        assertEquals(2000, updatedUserPoint.point());
    }

    @DisplayName("사용자_포인트_조회")
    @Test
    public void 사용자_포인트_조회() {
        // given
        // 2번 사용자 포인트 1000점 생성 후 저장
        UserPoint userPoint = new UserPoint(2L, 1000, System.currentTimeMillis());
        userPointTable.insertOrUpdate(userPoint.id(), userPoint.point());

        // when
        // 사용자 id 를 통해 조회
        UserPoint selectUserPoint = userPointTable.selectById(2L);

        // then
        // 생성 된 사용자와 조회해 온 사용자의 id, point 가 일치한지 검증
        assertEquals(userPoint.id(), selectUserPoint.id());
        assertEquals(userPoint.point(), selectUserPoint.point());

    }
}