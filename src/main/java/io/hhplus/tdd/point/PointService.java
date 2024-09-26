package io.hhplus.tdd.point;

import java.util.List;

public interface PointService {

    //포인트 충전
    UserPoint addPoint(long id, long amount);

    //포인트 조회
    UserPoint getPoint(long id);

    //포인트 사용
    UserPoint usePoint(long id, long amount);

    //포인트 충전/사용내역 조회
    List<PointHistory> getHistory(long id);
}
