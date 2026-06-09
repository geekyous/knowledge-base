package com.geekyous.kb.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Token 黑名单服务 — 管理已吊销的 JWT Token。
 *
 * <p>登出时将 Token 的 jti 存入 Redis，TTL 设为 Token 剩余有效期，自动过期清理。</p>
 *
 * @author Geekyous Guo
 */
@Service
public class TokenBlacklistService {

    private static final Logger logger = LoggerFactory.getLogger(TokenBlacklistService.class);

    private static final String BLACKLIST_KEY_PREFIX = "token:blacklist:";

    private final StringRedisTemplate redisTemplate;

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 将 Token 加入黑名单（登出时调用）。
     *
     * @param jti Token 唯一标识
     * @param remainingMillis Token 剩余有效时间（毫秒）
     */
    public void blacklist(String jti, long remainingMillis) {
        if (remainingMillis <= 0) {
            // Token 已过期，无需加入黑名单
            return;
        }
        String key = BLACKLIST_KEY_PREFIX + jti;
        redisTemplate.opsForValue().set(key, "1", remainingMillis, TimeUnit.MILLISECONDS);
        logger.info("Token 已加入黑名单: jti={}, 剩余有效期={}秒", jti, remainingMillis / 1000);
    }

    /**
     * 检查 Token 是否在黑名单中。
     *
     * @param jti Token 唯一标识
     * @return true 表示已被吊销
     */
    public boolean isBlacklisted(String jti) {
        String key = BLACKLIST_KEY_PREFIX + jti;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
