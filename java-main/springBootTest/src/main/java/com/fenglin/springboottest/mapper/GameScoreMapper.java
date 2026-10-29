package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.GameScore;
import org.apache.ibatis.annotations.Mapper;

/** 小游戏成绩 Mapper */
@Mapper
public interface GameScoreMapper extends BaseMapper<GameScore> {
}
