package com.fontal.cookagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fontal.cookagent.entity.Recipe;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RecipeMapper extends BaseMapper<Recipe> {
}
