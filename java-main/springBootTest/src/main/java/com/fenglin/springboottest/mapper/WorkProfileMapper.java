package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.WorkProfile;
import org.apache.ibatis.annotations.Mapper;

/** 打工档案 Mapper */
@Mapper
public interface WorkProfileMapper extends BaseMapper<WorkProfile> {
}
