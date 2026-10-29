package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fenglin.springboottest.entity.Category;
import com.fenglin.springboottest.entity.Place;
import com.fenglin.springboottest.mapper.CategoryMapper;
import com.fenglin.springboottest.mapper.PlaceMapper;
import com.fenglin.springboottest.service.HomeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 首页数据服务实现
 */
@Service
public class HomeServiceImpl implements HomeService {

    /** 分级固定顺序 */
    private static final List<String> GRADE_ORDER = Arrays.asList("S", "A", "B", "C");

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private PlaceMapper placeMapper;

    // ==================== 首页聚合 ====================

    @Override
    public Map<String, Object> overview() {
        List<Category> all = loadCategories();
        Map<Long, Long> countMap = countByCategoryId();

        // 一级分类，每项挂 children
        List<Map<String, Object>> categories = new ArrayList<>();
        List<Category> tops = all.stream()
                .filter(c -> c.getParentId() == null || c.getParentId() == 0L)
                .sorted(Comparator.comparing(c -> c.getSortNo() == null ? 0 : c.getSortNo()))
                .toList();

        for (Category top : tops) {
            List<Map<String, Object>> children = all.stream()
                    .filter(c -> top.getId().equals(c.getParentId()))
                    .sorted(Comparator.comparing(c -> c.getSortNo() == null ? 0 : c.getSortNo()))
                    .map(c -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", c.getId());
                        m.put("code", c.getCode());
                        m.put("name", c.getName());
                        m.put("icon", c.getIcon());
                        m.put("placeCount", countMap.getOrDefault(c.getId(), 0L));
                        return m;
                    })
                    .collect(Collectors.toList());

            // 一级的条目数 = 自身直接挂的数量 + 所有子分类的数量
            long ownCount = countMap.getOrDefault(top.getId(), 0L);
            long childCount = children.stream()
                    .mapToLong(m -> ((Number) m.get("placeCount")).longValue())
                    .sum();

            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", top.getId());
            node.put("code", top.getCode());
            node.put("name", top.getName());
            node.put("icon", top.getIcon());
            node.put("placeCount", ownCount + childCount);
            node.put("children", children);
            categories.add(node);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("categories", categories);
        result.put("grades", gradeStats());
        result.put("hotKeywords", hotKeywords());
        result.put("stats", statsBar(all.size()));
        return result;
    }

    // ==================== 搜索 ====================

    @Override
    public Map<String, Object> searchPlaces(String keyword, String categoryCode, String grade,
                                            String sort, int page, int size) {

        Map<Long, Category> categoryMap = toMap(loadCategories());

        LambdaQueryWrapper<Place> qw = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            qw.and(w -> w.like(Place::getName, kw)
                    .or().like(Place::getTags, kw)
                    .or().like(Place::getSummary, kw));
        }

        List<Long> categoryIds = resolveCategoryIds(categoryCode);
        if (categoryIds != null) {
            if (categoryIds.isEmpty()) {
                // 编码不存在，直接返回空列表
                return pageResult(page, size, 0L, List.of());
            }
            qw.in(Place::getCategoryId, categoryIds);
        }

        if (grade != null && !grade.isBlank()) {
            qw.eq(Place::getGrade, grade.trim().toUpperCase());
        }

        String sortKey = sort == null ? "hot" : sort.trim();
        switch (sortKey) {
            case "rating" -> qw.orderByDesc(Place::getRating);
            case "new" -> qw.orderByDesc(Place::getId);
            default -> qw.orderByDesc(Place::getHeat);
        }

        int safePage = Math.max(page, 1);
        int safeSize = size <= 0 ? 8 : Math.min(size, 100);

        Page<Place> p = placeMapper.selectPage(new Page<>(safePage, safeSize), qw);

        List<Map<String, Object>> list = p.getRecords().stream()
                .map(place -> toListItem(place, categoryMap))
                .collect(Collectors.toList());

        return pageResult(safePage, safeSize, p.getTotal(), list);
    }

    @Override
    public Map<String, Object> getPlace(Long id) {
        Place place = placeMapper.selectById(id);
        if (place == null) {
            return null;
        }

        // 浏览次数 +1（演示用，真实项目建议异步或走缓存）
        int views = (place.getViewCount() == null ? 0 : place.getViewCount()) + 1;
        Place update = new Place();
        update.setId(place.getId());
        update.setViewCount(views);
        placeMapper.updateById(update);
        place.setViewCount(views);

        Map<Long, Category> categoryMap = toMap(loadCategories());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", place.getId());
        data.put("name", place.getName());
        data.put("emoji", place.getEmoji());
        data.put("summary", place.getSummary());
        data.put("detail", place.getDetail());
        data.put("grade", place.getGrade());
        data.put("gradeLabel", place.getGradeLabel());
        data.put("rating", place.getRating());
        data.put("heat", place.getHeat());
        data.put("viewCount", place.getViewCount());
        data.put("tags", splitTags(place.getTags()));
        fillCategoryNames(data, place, categoryMap);
        return data;
    }

    // ==================== 内部工具 ====================

    private List<Category> loadCategories() {
        return categoryMapper.selectList(new QueryWrapper<Category>().orderByAsc("sort_no"));
    }

    private Map<Long, Category> toMap(List<Category> list) {
        Map<Long, Category> map = new HashMap<>();
        list.forEach(c -> map.put(c.getId(), c));
        return map;
    }

    /** 按分类统计条目数：categoryId -> count */
    private Map<Long, Long> countByCategoryId() {
        List<Map<String, Object>> rows = placeMapper.selectMaps(
                new QueryWrapper<Place>().select("category_id AS cid", "COUNT(*) AS cnt").groupBy("category_id"));

        Map<Long, Long> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object cid = row.get("cid");
            Object cnt = row.get("cnt");
            if (cid instanceof Number c && cnt instanceof Number n) {
                map.put(c.longValue(), n.longValue());
            }
        }
        return map;
    }

    /** 分级统计，固定 S/A/B/C 顺序 */
    private List<Map<String, Object>> gradeStats() {
        List<Map<String, Object>> rows = placeMapper.selectMaps(
                new QueryWrapper<Place>().select("grade AS g", "COUNT(*) AS cnt").groupBy("grade"));

        Map<String, Long> countMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object g = row.get("g");
            Object cnt = row.get("cnt");
            if (g != null && cnt instanceof Number n) {
                countMap.put(g.toString(), n.longValue());
            }
        }

        Map<String, String> labels = Map.of(
                "S", "传说级", "A", "稀有级", "B", "常见级", "C", "普通级");

        List<Map<String, Object>> list = new ArrayList<>();
        for (String g : GRADE_ORDER) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("value", g);
            m.put("label", labels.get(g));
            m.put("count", countMap.getOrDefault(g, 0L));
            list.add(m);
        }
        return list;
    }

    /** 热门搜索词：取热度最高的 6 个条目名 */
    private List<String> hotKeywords() {
        List<Place> hot = placeMapper.selectList(new QueryWrapper<Place>()
                .select("name")
                .orderByDesc("heat")
                .last("LIMIT 6"));
        return hot.stream().map(Place::getName).collect(Collectors.toList());
    }

    /** 顶部统计条 */
    private List<Map<String, Object>> statsBar(int categoryCount) {
        List<Map<String, Object>> rows = placeMapper.selectMaps(new QueryWrapper<Place>()
                .select("COUNT(*) AS total", "SUM(view_count) AS views", "AVG(rating) AS avg_rating"));

        long total = 0;
        long views = 0;
        double avg = 0;
        if (!rows.isEmpty()) {
            Map<String, Object> row = rows.get(0);
            if (row.get("total") instanceof Number n) total = n.longValue();
            if (row.get("views") instanceof Number n) views = n.longValue();
            if (row.get("avg_rating") instanceof Number n) avg = n.doubleValue();
        }

        List<Map<String, Object>> stats = new ArrayList<>();
        stats.add(stat("🎯", "收录条目", String.valueOf(total)));
        stats.add(stat("🗂️", "分类", String.valueOf(categoryCount)));
        stats.add(stat("👀", "累计浏览", humanNumber(views)));
        stats.add(stat("⭐", "平均评分", BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP).toPlainString()));
        return stats;
    }

    private Map<String, Object> stat(String icon, String label, String value) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("icon", icon);
        m.put("label", label);
        m.put("value", value);
        return m;
    }

    /** 18000 -> 1.8万 */
    private String humanNumber(long n) {
        if (n >= 10000) {
            return BigDecimal.valueOf(n / 10000.0).setScale(1, RoundingMode.HALF_UP).toPlainString() + "万";
        }
        return String.valueOf(n);
    }

    /** 把分类编码解析成一组分类 ID（一级分类包含其所有子分类）；编码为空返回 null */
    private List<Long> resolveCategoryIds(String categoryCode) {
        if (categoryCode == null || categoryCode.isBlank()) {
            return null;
        }
        List<Category> all = loadCategories();
        Category target = all.stream()
                .filter(c -> categoryCode.trim().equals(c.getCode()))
                .findFirst()
                .orElse(null);
        if (target == null) {
            return List.of();
        }

        List<Long> ids = new ArrayList<>();
        ids.add(target.getId());
        all.stream()
                .filter(c -> target.getId().equals(c.getParentId()))
                .forEach(c -> ids.add(c.getId()));
        return ids;
    }

    private Map<String, Object> toListItem(Place place, Map<Long, Category> categoryMap) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", place.getId());
        m.put("name", place.getName());
        m.put("emoji", place.getEmoji());
        m.put("summary", place.getSummary());
        m.put("grade", place.getGrade());
        m.put("rating", place.getRating());
        m.put("heat", place.getHeat());
        m.put("tags", splitTags(place.getTags()));
        fillCategoryNames(m, place, categoryMap);
        return m;
    }

    /** 补 parentCategoryName / categoryName */
    private void fillCategoryNames(Map<String, Object> target, Place place, Map<Long, Category> categoryMap) {
        Category self = categoryMap.get(place.getCategoryId());
        if (self == null) {
            target.put("parentCategoryName", "");
            target.put("categoryName", "");
            return;
        }

        if (self.getParentId() != null && self.getParentId() != 0L) {
            Category parent = categoryMap.get(self.getParentId());
            target.put("parentCategoryName", parent == null ? "" : parent.getName());
            target.put("categoryName", self.getName());
        } else {
            target.put("parentCategoryName", self.getName());
            target.put("categoryName", "");
        }
    }

    private List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private Map<String, Object> pageResult(int page, int size, long total, List<Map<String, Object>> list) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("page", page);
        data.put("size", size);
        data.put("total", total);
        data.put("hasNext", (long) page * size < total);
        data.put("list", list);
        return data;
    }
}
