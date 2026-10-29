package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 打工档案（对应 t_work_profile）
 *
 * 一个用户在「海绵宝宝」「章鱼哥」两个身份下各有一份档案，
 * 互不影响：你可以在蟹堡王当煎肉饼冠军，同时在收银台摸鱼。
 */
@Data
@TableName("t_work_profile")
public class WorkProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 角色：spongebob / squidward */
    private String roleCode;

    /** 攒下的蟹堡币 */
    private Integer coins;

    /** 心情 0-100 */
    private Integer mood;

    /** 累计绩效 */
    private Integer performance;

    /** 累计上班次数 */
    private Integer shiftCount;

    /** 连续打卡天数 */
    private Integer streak;

    /** 最后一次打卡日期 */
    private LocalDate lastShiftDate;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
