package com.fenglin.springboottest.service;

import java.util.Map;

/**
 * 首页数据服务
 */
public interface HomeService {

    /**
     * 首页聚合数据：分类树 + 分级统计 + 热门词 + 统计条
     */
    Map<String, Object> overview();

    /**
     * 分级分类搜索 + 分页
     *
     * @param keyword      关键词，可为空
     * @param categoryCode 分类编码（一级或二级），可为空
     * @param grade        分级 S/A/B/C，可为空
     * @param sort         排序：hot / rating / new
     * @param page         页码，从 1 开始
     * @param size         每页条数
     */
    Map<String, Object> searchPlaces(String keyword, String categoryCode, String grade,
                                     String sort, int page, int size);

    /**
     * 条目详情，同时把浏览次数 +1；不存在返回 null
     */
    Map<String, Object> getPlace(Long id);
}
