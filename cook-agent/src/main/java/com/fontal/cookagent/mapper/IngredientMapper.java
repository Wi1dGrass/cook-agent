package com.fontal.cookagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fontal.cookagent.entity.Ingredient;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IngredientMapper extends BaseMapper<Ingredient> {
}
