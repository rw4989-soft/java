package com.fenglin.springboottest.config;

import com.fenglin.springboottest.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 登录校验拦截器：保护 /api/home/** 这组接口。
 *
 * 前端每次请求带上 Authorization: Bearer &lt;token&gt;，
 * 校验不通过直接返回 401 + 统一 JSON，前端收到 401 会清本地登录态并跳回登录页。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /** 请求属性名：拦截器校验通过后把昵称放进去，Controller 需要时可以直接取 */
    public static final String ATTR_NICKNAME = "currentNickname";

    /** 请求属性名：当前登录用户 ID */
    public static final String ATTR_USER_ID = "currentUserId";

    @Autowired
    private TokenService tokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        // 浏览器的跨域预检请求直接放行
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String header = request.getHeader("Authorization");
        String token = null;
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7).trim();
        }

        TokenService.Session session = tokenService.verify(token);
        if (session == null) {
            writeUnauthorized(response);
            return false;
        }

        request.setAttribute(ATTR_NICKNAME, session.nickname());
        request.setAttribute(ATTR_USER_ID, session.userId());
        return true;
    }

    private void writeUnauthorized(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"登录状态已失效，请重新登录\",\"data\":null}");
    }
}
