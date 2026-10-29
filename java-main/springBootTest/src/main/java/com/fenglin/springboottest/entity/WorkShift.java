package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 打卡记录（对应 t_work_shift）：每上完一次班写一条，前端拿它画时间轴。
 */
@Data
@TableName("t_work_shift")
public class WorkShift {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 角色：spongebob / squidward */
    private String roleCode;

    /** 所属日期 */
    private LocalDate shiftDate;

    private Integer coinsGain;

    private Integer moodDelta;

    private Integer performDelta;

    /** 班次评级 S / A / B / C */
    private String grade;

    /** 一句话总结 */
    private String summary;

    /** 过程回放（JSON 字符串） */
    private String timeline;

    private LocalDateTime createTime;
}
