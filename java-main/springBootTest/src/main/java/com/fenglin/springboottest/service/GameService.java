package com.fenglin.springboottest.service;

import java.util.Map;

/**
 * 小游戏成绩业务接口
 */
public interface GameService {

    /** 我的最佳成绩 + 全服排行榜 + 我的排名 + 段位 */
    Map<String, Object> overview(Long userId, String gameCode);

    /**
     * 提交一局成绩（只在刷新纪录时更新，其他情况只累加游玩次数）
     *
     * @return 是否破纪录、当前最佳、排名
     */
    Map<String, Object> submit(Long userId, String gameCode, int score, int combo, int burgers);
}
