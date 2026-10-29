package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.entity.GameScore;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.GameScoreMapper;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 小游戏成绩实现。
 *
 * 只存「最好成绩」而不是每局流水：排行榜够用，表也不会无限膨胀。
 */
@Service
public class GameServiceImpl implements GameService {

    @Autowired
    private GameScoreMapper scoreMapper;

    @Autowired
    private UserMapper userMapper;

    /** 段位阶梯：分数达到 min 即为该段位 */
    private record Rank(String name, String icon, int min) {
    }

    private static final List<Rank> RANKS = List.of(
            new Rank("铁铲学徒", "🔧", 0),
            new Rank("铜铲厨师", "🥉", 250),
            new Rank("银铲主厨", "🥈", 500),
            new Rank("金铲大师", "🥇", 800),
            new Rank("蟹堡王之星", "🌟", 1200)
    );

    @Override
    public Map<String, Object> overview(Long userId, String gameCode) {
        GameScore mine = ensureRow(userId, gameCode);
        int best = mine.getBestScore() == null ? 0 : mine.getBestScore();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("gameCode", gameCode);
        data.put("bestScore", best);
        data.put("bestCombo", mine.getBestCombo());
        data.put("burgers", mine.getBurgers());
        data.put("plays", mine.getPlays());
        data.put("rank", rankOf(best).name());
        data.put("rankIcon", rankOf(best).icon());
        data.put("ranks", RANKS.stream().map(r -> Map.of("name", r.name(), "icon", r.icon(), "min", r.min())).toList());
        data.put("leaderboard", leaderboard(userId, gameCode));
        data.put("myRank", myRank(userId, gameCode, best));
        return data;
    }

    @Override
    public Map<String, Object> submit(Long userId, String gameCode, int score, int combo, int burgers) {
        GameScore row = ensureRow(userId, gameCode);

        int prevBest = row.getBestScore() == null ? 0 : row.getBestScore();
        boolean newRecord = score > prevBest;

        row.setPlays((row.getPlays() == null ? 0 : row.getPlays()) + 1);
        if (newRecord) {
            row.setBestScore(score);
        }
        row.setBestCombo(Math.max(nvl(row.getBestCombo()), combo));
        row.setBurgers(Math.max(nvl(row.getBurgers()), burgers));
        row.setUpdateTime(LocalDateTime.now());
        scoreMapper.updateById(row);

        int best = row.getBestScore();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("newRecord", newRecord);
        data.put("prevBest", prevBest);
        data.put("bestScore", best);
        data.put("bestCombo", row.getBestCombo());
        data.put("plays", row.getPlays());
        data.put("rank", rankOf(best).name());
        data.put("rankIcon", rankOf(best).icon());
        data.put("myRank", myRank(userId, gameCode, best));
        data.put("leaderboard", leaderboard(userId, gameCode));
        return data;
    }

    // ==================== 内部 ====================

    private GameScore ensureRow(Long userId, String gameCode) {
        GameScore row = scoreMapper.selectOne(new LambdaQueryWrapper<GameScore>()
                .eq(GameScore::getUserId, userId)
                .eq(GameScore::getGameCode, gameCode));

        if (row == null) {
            GameScore fresh = new GameScore();
            fresh.setUserId(userId);
            fresh.setGameCode(gameCode);
            fresh.setBestScore(0);
            fresh.setBestCombo(0);
            fresh.setBurgers(0);
            fresh.setPlays(0);
            scoreMapper.insert(fresh);
            row = scoreMapper.selectById(fresh.getId());
        }
        return row;
    }

    /** 全服前 10 名（只列真的玩过的） */
    private List<Map<String, Object>> leaderboard(Long userId, String gameCode) {
        List<GameScore> top = scoreMapper.selectList(new LambdaQueryWrapper<GameScore>()
                .eq(GameScore::getGameCode, gameCode)
                .gt(GameScore::getBestScore, 0)
                .orderByDesc(GameScore::getBestScore)
                .last("limit 10"));

        List<Map<String, Object>> list = new ArrayList<>();
        int pos = 1;
        for (GameScore s : top) {
            User u = userMapper.selectById(s.getUserId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("pos", pos++);
            m.put("nickname", u == null ? "神秘海底生物" : u.getNickname());
            m.put("score", s.getBestScore());
            m.put("combo", s.getBestCombo());
            m.put("rankName", rankOf(s.getBestScore()).name());
            m.put("rankIcon", rankOf(s.getBestScore()).icon());
            m.put("mine", s.getUserId().equals(userId));
            list.add(m);
        }
        return list;
    }

    /** 我的名次：比我高的人有几个，就是第几名 */
    private int myRank(Long userId, String gameCode, int best) {
        if (best <= 0) {
            return 0;
        }
        Long higher = scoreMapper.selectCount(new LambdaQueryWrapper<GameScore>()
                .eq(GameScore::getGameCode, gameCode)
                .gt(GameScore::getBestScore, best));
        return higher.intValue() + 1;
    }

    private Rank rankOf(int score) {
        Rank current = RANKS.get(0);
        for (Rank r : RANKS) {
            if (score >= r.min()) {
                current = r;
            }
        }
        return current;
    }

    private int nvl(Integer v) {
        return v == null ? 0 : v;
    }
}
