package com.fenglin.springboottest.service;

import java.util.List;
import java.util.Map;

/**
 * 打工日常业务接口
 */
public interface WorkService {

    /**
     * 打工大厅聚合数据：角色设定 + 各自档案 + 今日状态 + 最近打卡记录
     */
    Map<String, Object> overview(Long userId);

    /**
     * 抽一次班次（5 幕剧情）。今天已经打过卡的话会抛业务异常。
     */
    Map<String, Object> draw(Long userId, String roleCode);

    /**
     * 结算这一次班次
     *
     * @param picks 前端按顺序提交的选择：[{sceneId, choiceIndex}, ...]
     */
    Map<String, Object> settle(Long userId, String roleCode, List<Map<String, Object>> picks);

    /**
     * 某个角色的打卡时间轴
     */
    List<Map<String, Object>> history(Long userId, String roleCode, int limit);
}
