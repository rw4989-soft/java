package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 认证相关 REST 接口（供 Vue 前端调用）
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    /**
     * 连通性探测（前端可在控制台访问 /api/auth/health 验证后端是否在线）
     */
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("蟹堡王后厨在线 🍔", "ok");
    }

    /**
     * 登录
     * POST /api/auth/login  body: {"username": "...", "password": "..."}
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Result.error("章鱼哥说：用户名和密码都不能空！");
        }

        User user = userService.login(username.trim(), password);
        if (user == null) {
            return Result.error("痞老板偷不走密码，但你也输错啦！");
        }

        return Result.success("🎉 登录成功！欢迎回来，" + user.getNickname() + "！",
                Map.of("nickname", user.getNickname()));
    }

    /**
     * 注册
     * POST /api/auth/register  body: {"username": "...", "password": "...", "nickname": "..."}
     */
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String nickname = body.get("nickname");

        if (username == null || username.isBlank()) {
            return Result.error("章鱼哥说：连名字都不填还想加入比奇堡？");
        }
        if (username.trim().length() < 3) {
            return Result.error("名字太短啦，至少 3 个字符！");
        }
        if (password == null || password.isBlank()) {
            return Result.error("密码不能空，不然痞老板就来了！");
        }
        if (password.length() < 6) {
            return Result.error("密码至少 6 位，蟹老板的钱柜都比这安全！");
        }

        User user = userService.register(username.trim(), password, nickname);
        if (user == null) {
            return Result.error("这个名字已经被别人抢先注册啦，换一个试试！");
        }

        return Result.success("🎉 注册成功！欢迎入住菠萝屋隔壁！",
                Map.of("nickname", user.getNickname()));
    }
}
