package com.fontal.cookagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fontal.cookagent.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
