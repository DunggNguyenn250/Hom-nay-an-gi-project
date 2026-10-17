package org.example.homnayangi.helper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.homnayangi.exception.AppException;
import org.example.homnayangi.exception.ErrorCode;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RedisLockHelper {

    RedisTemplate<String, Object> redisTemplate;

    public void executeWithRoomLock(UUID roomId, Runnable action) {
        executeWithRoomLock(roomId, () -> {
            action.run();
            return null;
        });
    }

    public <T> T executeWithRoomLock(UUID roomId, Supplier<T> supplier) {
        String lockKey = "lock:room:" + roomId;
        String lockValue = UUID.randomUUID().toString();
        boolean acquired = false;
        int retries = 5;

        while (retries > 0) {
            Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, Duration.ofSeconds(5));
            if (Boolean.TRUE.equals(success)) {
                acquired = true;
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Thread interrupted while waiting for room lock", e);
            }
            retries--;
        }

        if (!acquired) {
            throw new AppException(ErrorCode.ROOM_BUSY);
        }

        try {
            return supplier.get();
        } finally {
            Object currentLockValue = redisTemplate.opsForValue().get(lockKey);
            if (lockValue.equals(currentLockValue)) {
                redisTemplate.delete(lockKey);
            }
        }
    }
}
