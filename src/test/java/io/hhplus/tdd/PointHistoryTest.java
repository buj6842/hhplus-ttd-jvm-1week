package io.hhplus.tdd;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PointHistoryTest {

    @Mock
    private PointHistoryTable pointHistoryTable;

    @BeforeEach
    public void setUp() {
        pointHistoryTable = new PointHistoryTable();
    }

    @DisplayName("포인트 충전 및 사용 이력 저장 테스트")
    @Test
    public void 포인트_충전_사용이력_저장_테스트() {
        // given
        long userId = 1;
        // 충전할 포인트
        long chargeAmount = 1000;
        // 사용할 포인트
        long useAmount = 500;


        // when
        // 충전 이력 저장
        PointHistory addPointHistory = pointHistoryTable.insert(userId, chargeAmount, TransactionType.CHARGE, System.currentTimeMillis());
        // 사용 이력 저장
        PointHistory useHistory = pointHistoryTable.insert(userId, useAmount, TransactionType.USE, System.currentTimeMillis());

        // then
        // 저장된 이력과 입력한 이력의 내용들이 같은지 검증 (충전시)
        assertEquals(userId, addPointHistory.userId());
        assertEquals(chargeAmount, addPointHistory.amount());
        assertEquals(TransactionType.CHARGE, addPointHistory.type());

        // 저장된 이력과 입력한 이력의 내용들이 같은지 검증 (사용시)
        assertEquals(userId, useHistory.userId());
        assertEquals(useAmount, useHistory.amount());
        assertEquals(TransactionType.USE, useHistory.type());

    }

    public void 포인트_충전_이력_조회_테스트() {
        // given
        // 사용자의 포인트 이력을 만들기
        PointHistory firstHistory = pointHistoryTable.insert(1, 1000, TransactionType.CHARGE, System.currentTimeMillis());
        PointHistory secondHistory = pointHistoryTable.insert(1, 2000, TransactionType.CHARGE, System.currentTimeMillis());
        // 포인트 이력을 pointHistoryTable 에서 조회 해오는 값과 같도록 세팅
        List<PointHistory> sampleHistory = new ArrayList<>();
        sampleHistory.add(firstHistory);
        sampleHistory.add(secondHistory);
        // when
        List<PointHistory> pointHistories = pointHistoryTable.selectAllByUserId(1);

        // then
        // 포인트 이력의 갯수(size) 가 같은지 확인
        assertEquals(sampleHistory.size(),pointHistories.size());

    }

}
