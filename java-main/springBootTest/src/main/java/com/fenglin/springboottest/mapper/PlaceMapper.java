package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.Place;
import org.apache.ibatis.annotations.Mapper;

/**
 * 条目表 Mapper
 */
@Mapper
public interface PlaceMapper extends BaseMapper<Place> {
}
