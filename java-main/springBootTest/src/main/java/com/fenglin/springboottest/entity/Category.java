package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 分类表实体类（对应数据库表 t_category）
 * 两级结构：parent_id = 0 为一级分类
 */
@Data
@TableName("t_category")
public class Category {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类编码（前端筛选传这个） */
    private String code;

    /** 分类名称 */
    private String name;

    /** 图标 emoji */
    private String icon;

    /** 父分类 ID，0 表示一级 */
    private Long parentId;

    /** 排序号 */
    private Integer sortNo;
}
