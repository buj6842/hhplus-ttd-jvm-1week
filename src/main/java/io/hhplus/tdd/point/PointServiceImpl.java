package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

    @Override
    public UserPoint getPoint(long id) {
        // id 로 유저 포인트 조회
        // UserPointTable 의 selectById 진행 시 getOrDefault를 이용하기에 null 의 경우는 생각하지 않았음.
        return userPointTable.selectById(id);
    }

    @Override
    public UserPoint usePoint(long id, long amount) {
        Lock lock = lockMap.computeIfAbsent(id, userId -> new ReentrantLock());
        lock.lock();

        try {
            // 유저의 포인트 조회
            UserPoint userPoint = userPointTable.selectById(id);
            // 유저의 포인트 사용을 위한 검증
            UserPoint afterPoint = userPoint.validateUsage(amount);
            // 포인트 테이블에 저장
            userPointTable.insertOrUpdate(id, afterPoint.point());
            // 포인트 사용 내역 테이블에 내역 저장
            pointHistoryTable.insert(id, amount, TransactionType.USE, System.currentTimeMillis());
            return afterPoint;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<PointHistory> getHistory(long id) {
        return pointHistoryTable.selectAllByUserId(id);
    }


}
