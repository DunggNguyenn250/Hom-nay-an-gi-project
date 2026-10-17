package org.example.homnayangi.helper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.homnayangi.exception.AppException;
import org.example.homnayangi.exception.ErrorCode;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RedisLockHelper {

    private static final int LEASE_SECONDS = 10;
    private static final int RENEW_INTERVAL_SECONDS = 3;

    private static final String RENEW_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('expire', KEYS[1], ARGV[2]) " +
            "else " +
            "  return 0 " +
            "end";

    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('del', KEYS[1]) " +
            "else " +
            "  return 0 " +
            "end";

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
            Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, Duration.ofSeconds(LEASE_SECONDS));
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

        // Tự động gia hạn thời gian khóa (Lease Renewal / Watchdog) định kỳ khi transaction đang chạy
        ScheduledExecutorService renewalScheduler = Executors.newSingleThreadScheduledExecutor();
        ScheduledFuture<?> renewalTask = renewalScheduler.scheduleAtFixedRate(() -> {
            try {
                redisTemplate.execute(
                        new DefaultRedisScript<>(RENEW_SCRIPT, Long.class),
                        Collections.singletonList(lockKey),
                        lockValue,
                        String.valueOf(LEASE_SECONDS)
                );
            } catch (Exception ignored) {
            }
        }, RENEW_INTERVAL_SECONDS, RENEW_INTERVAL_SECONDS, TimeUnit.SECONDS);

        try {
            T result = supplier.get();

            // Kiểm tra quyền sở hữu khóa trước khi hoàn tất
            Object currentLock = redisTemplate.opsForValue().get(lockKey);
            if (!lockValue.equals(currentLock)) {
                throw new AppException(ErrorCode.ROOM_BUSY);
            }

            return result;
        } finally {
            renewalTask.cancel(true);
            renewalScheduler.shutdown();

            // Nhả khóa an toàn bằng Lua Script (chỉ xóa nếu đúng lockValue)
            try {
                redisTemplate.execute(
                        new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class),
                        Collections.singletonList(lockKey),
                        lockValue
                );
            } catch (Exception ignored) {
            }
        }
    }
}
