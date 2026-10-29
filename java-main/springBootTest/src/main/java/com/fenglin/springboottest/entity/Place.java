package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 条目表实体类（对应数据库表 t_place）
 */
@Data
@TableName("t_place")
public class Place {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 名称 */
    private String name;

    /** 图标 emoji */
    private String emoji;

    /** 一句话简介 */
    private String summary;

    /** 详细介绍 */
    private String detail;

    /** 分级：S / A / B / C */
    private String grade;

    /** 分级中文名 */
    private String gradeLabel;

    /** 评分 */
    private BigDecimal rating;

    /** 热度 */
    private Integer heat;

    /** 浏览次数 */
    private Integer viewCount;

    /** 所属分类 ID */
    private Long categoryId;

    /** 标签，逗号分隔 */
    private String tags;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
