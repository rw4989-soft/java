package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.service.HomeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 首页接口（需要登录，由 AuthInterceptor 统一校验 token）
 */
@RestController
@RequestMapping("/api/home")
public class HomeController {

    @Autowired
    private HomeService homeService;

    /**
     * 首页聚合：分类树 + 分级统计 + 热门词 + 统计条
     * GET /api/home/overview
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.success("比奇堡地图已展开 🗺️", homeService.overview());
    }

    /**
     * 只要分类树
     * GET /api/home/categories
     */
    @GetMapping("/categories")
    public Result<Object> categories() {
        Object categories = homeService.overview().get("categories");
        return Result.success("分类打捞完毕 🗂️", categories);
    }

    /**
     * 分级分类搜索
     * GET /api/home/places?keyword=&categoryCode=&grade=&sort=hot&page=1&size=8
     */
    @GetMapping("/places")
    public Result<Map<String, Object>> places(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false, defaultValue = "hot") String sort,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "8") int size) {

        return Result.success("找到了这些角落 🐚",
                homeService.searchPlaces(keyword, categoryCode, grade, sort, page, size));
    }

    /**
     * 条目详情（浏览次数 +1）
     * GET /api/home/places/{id}
     */
    @GetMapping("/places/{id}")
    public Result<Map<String, Object>> place(@PathVariable Long id) {
        Map<String, Object> place = homeService.getPlace(id);
        if (place == null) {
            return Result.error("这个角落好像被海水冲走了 🫧");
        }
        return Result.success("资料已打捞上来 📜", place);
    }
}
