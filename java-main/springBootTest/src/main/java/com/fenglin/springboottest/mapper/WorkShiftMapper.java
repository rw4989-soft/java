package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.WorkShift;
import org.apache.ibatis.annotations.Mapper;

/** 打卡记录 Mapper */
@Mapper
public interface WorkShiftMapper extends BaseMapper<WorkShift> {
}
