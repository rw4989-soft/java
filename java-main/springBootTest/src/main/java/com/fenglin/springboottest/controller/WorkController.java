package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.config.AuthInterceptor;
import com.fenglin.springboottest.service.WorkService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 打工日常接口（需要登录，token 由 AuthInterceptor 统一校验）
 */
@RestController
@RequestMapping("/api/work")
public class WorkController {

    @Autowired
    private WorkService workService;

    /**
     * 打工大厅：两个角色的档案、今日是否已打卡、最近打卡记录
     * GET /api/work/overview
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview(HttpServletRequest request) {
        try {
            return Result.success("更衣室的柜子打开了 🚪", workService.overview(uid(request)));
        } catch (IllegalStateException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 抽一次班次（5 幕剧情）
     * GET /api/work/shift/draw?roleCode=spongebob
     */
    @GetMapping("/shift/draw")
    public Result<Map<String, Object>> draw(HttpServletRequest request,
                                            @RequestParam String roleCode) {
        try {
            return Result.success("打卡成功，今天也要加油鸭 🕐", workService.draw(uid(request), roleCode));
        } catch (IllegalStateException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 结算这一次班次
     * POST /api/work/shift/settle
     * body: { "roleCode": "spongebob", "picks": [{"sceneId":"sb-01","choiceIndex":0}, ...] }
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/shift/settle")
    public Result<Map<String, Object>> settle(HttpServletRequest request,
                                              @RequestBody Map<String, Object> body) {
        try {
            String roleCode = String.valueOf(body.get("roleCode"));
            Object raw = body.get("picks");
            List<Map<String, Object>> picks = raw instanceof List<?> list
                    ? (List<Map<String, Object>>) list
                    : List.of();
            return Result.success("这班到此为止，辛苦了 🧾",
                    workService.settle(uid(request), roleCode, picks));
        } catch (IllegalStateException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 打卡时间轴
     * GET /api/work/shifts?roleCode=spongebob&limit=10
     */
    @GetMapping("/shifts")
    public Result<List<Map<String, Object>>> shifts(HttpServletRequest request,
                                                    @RequestParam(required = false) String roleCode,
                                                    @RequestParam(defaultValue = "10") int limit) {
        return Result.success("历史打卡记录 📜", workService.history(uid(request), roleCode, limit));
    }

    /** 拦截器已经把用户 ID 放进请求属性里了 */
    private Long uid(HttpServletRequest request) {
        Object v = request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        if (v instanceof Long l) {
            return l;
        }
        return v == null ? null : Long.valueOf(String.valueOf(v));
    }
}
