package io.hhplus.tdd.point;

public record UserPoint(
        long id,
        long point,
        long updateMillis
) {

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }

    public UserPoint validate(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("충전할 포인트는 0보다 작을 수 없습니다.");
        }
        if (point + amount > 1000000) {
            throw new IllegalArgumentException("포인트는 1,000,000(백만) 포인트를 초과할 수 없습니다.");
        }
        return new UserPoint(id, point + amount, System.currentTimeMillis());
    }

    public UserPoint validateUsage(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("사용할 포인트는 0보다 커야 합니다.");
        }
        if (point < amount) {
            throw new IllegalArgumentException("포인트가 부족합니다.");
        }
        return new UserPoint(id, point - amount, System.currentTimeMillis());
    }
}