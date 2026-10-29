package com.fenglin.springboottest.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 极简 token 服务：登录发放、请求校验、退出作废。
 *
 * 演示项目用内存 Map 存会话，重启即失效；生产环境请换成 Redis 或 JWT。
 */
@Service
public class TokenService {

    /** token 有效期（秒）：2 小时 */
    private static final long EXPIRE_SECONDS = 2 * 60 * 60;

    /** token -> 会话信息 */
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    /**
     * 会话信息
     *
     * @param userId   用户 ID（打工日常、小游戏要按用户存数据）
     * @param username 用户名
     * @param nickname 昵称
     * @param expireAt 过期时间戳（毫秒）
     */
    public record Session(Long userId, String username, String nickname, long expireAt) {
    }

    /** 登录成功时发放一个 token */
    public String issue(Long userId, String username, String nickname) {
        String token = UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, new Session(userId, username, nickname,
                System.currentTimeMillis() + EXPIRE_SECONDS * 1000));
        return token;
    }

    /** 校验 token，无效或已过期返回 null，顺便清掉过期项 */
    public Session verify(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        Session session = sessions.get(token);
        if (session == null) {
            return null;
        }
        if (session.expireAt() < System.currentTimeMillis()) {
            sessions.remove(token);
            return null;
        }
        return session;
    }

    /** 退出登录时作废 token */
    public void revoke(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    /** 有效期（秒），返回给前端展示用 */
    public long getExpireSeconds() {
        return EXPIRE_SECONDS;
    }
}
