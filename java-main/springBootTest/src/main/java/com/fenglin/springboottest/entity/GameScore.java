package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 小游戏成绩（对应 t_game_score）：每个用户每个游戏一条，只存最好成绩。
 */
@Data
@TableName("t_game_score")
public class GameScore {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 游戏标识，目前只有 burger（蟹堡王出餐大作战） */
    private String gameCode;

    private Integer bestScore;

    private Integer bestCombo;

    /** 单局最多出餐数 */
    private Integer burgers;

    /** 游玩次数 */
    private Integer plays;

    private LocalDateTime updateTime;
}
