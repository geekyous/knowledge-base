package com.geekyous.kb.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 登录防护服务 — 追踪登录失败次数，超过阈值临时锁定账号。
 *
 * <p>使用 Redis 存储失败计数，支持分布式场景。
 * 锁定时间到后自动解除，无需人工干预。</p>
 *
 * @author Geekyous Guo
 */
@Service
public class LoginProtectionService {

    private static final Logger logger = LoggerFactory.getLogger(LoginProtectionService.class);

    private static final String FAIL_KEY_PREFIX = "login_fail:";
    private static final String LOCK_KEY_PREFIX = "login_lock:";

    /** 连续登录失败触发锁定的阈值 */
    private static final int MAX_FAIL_COUNT = 5;

    /** 失败计数的时间窗口（秒） */
    private static final int FAIL_WINDOW_SECONDS = 300;

    /** 账号锁定时长（秒） */
    private static final int LOCK_DURATION_SECONDS = 900;

    private final StringRedisTemplate redisTemplate;

    public LoginProtectionService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 检查账号是否已被锁定。
     *
     * @param username 用户名
     * @return true 表示已锁定，应拒绝登录
     */
    public boolean isLocked(String username) {
        String lockKey = LOCK_KEY_PREFIX + username;
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
    }

    /**
     * 获取账号剩余锁定时间（秒）。
     *
     * @param username 用户名
     * @return 剩余秒数，未锁定返回 0
     */
    public long getRemainingLockTime(String username) {
        String lockKey = LOCK_KEY_PREFIX + username;
        Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
        return ttl != null && ttl > 0 ? ttl : 0;
    }

    /**
     * 记录一次登录失败。超过阈值则触发锁定。
     *
     * @param username 用户名
     * @return 当前失败次数
     */
    public int recordFailure(String username) {
        String failKey = FAIL_KEY_PREFIX + username;
        Long count = redisTemplate.opsForValue().increment(failKey);

        if (count != null && count == 1) {
            redisTemplate.expire(failKey, FAIL_WINDOW_SECONDS, TimeUnit.SECONDS);
        }

        if (count != null && count >= MAX_FAIL_COUNT) {
            // 触发锁定
            String lockKey = LOCK_KEY_PREFIX + username;
            redisTemplate.opsForValue().set(lockKey, "1", LOCK_DURATION_SECONDS, TimeUnit.SECONDS);
            // 清除失败计数，锁定解除后重新计数
            redisTemplate.delete(failKey);
            logger.warn("账号已锁定: username={}, 连续失败{}次, 锁定{}秒", username, count, LOCK_DURATION_SECONDS);
        }

        return count != null ? count.intValue() : 1;
    }

    /**
     * 登录成功，清除失败计数。
     *
     * @param username 用户名
     */
    public void recordSuccess(String username) {
        redisTemplate.delete(FAIL_KEY_PREFIX + username);
    }
}
