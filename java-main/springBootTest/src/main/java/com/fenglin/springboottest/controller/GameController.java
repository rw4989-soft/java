package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.config.AuthInterceptor;
import com.fenglin.springboottest.service.GameService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 小游戏成绩接口
 */
@RestController
@RequestMapping("/api/game")
public class GameController {

    @Autowired
    private GameService gameService;

    /**
     * 我的最佳 + 排行榜
     * GET /api/game/overview?gameCode=burger
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview(HttpServletRequest request,
                                               @RequestParam(defaultValue = "burger") String gameCode) {
        return Result.success("排行榜已更新 🏆", gameService.overview(uid(request), gameCode));
    }

    /**
     * 提交成绩
     * POST /api/game/score  body: { "gameCode":"burger", "score":680, "combo":12, "burgers":9 }
     */
    @PostMapping("/score")
    public Result<Map<String, Object>> score(HttpServletRequest request,
                                             @RequestBody Map<String, Object> body) {
        String gameCode = body.get("gameCode") == null ? "burger" : String.valueOf(body.get("gameCode"));
        int score = toInt(body.get("score"));
        int combo = toInt(body.get("combo"));
        int burgers = toInt(body.get("burgers"));

        Map<String, Object> data = gameService.submit(uid(request), gameCode, score, combo, burgers);
        String msg = Boolean.TRUE.equals(data.get("newRecord")) ? "🎉 破纪录了！" : "成绩已记录 🧾";
        return Result.success(msg, data);
    }

    private int toInt(Object o) {
        if (o instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (Exception e) {
            return 0;
        }
    }

    private Long uid(HttpServletRequest request) {
        Object v = request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        if (v instanceof Long l) {
            return l;
        }
        return v == null ? null : Long.valueOf(String.valueOf(v));
    }
}
