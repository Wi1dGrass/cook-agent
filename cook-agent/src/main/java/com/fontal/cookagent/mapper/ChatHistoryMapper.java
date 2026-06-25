package com.fontal.cookagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fontal.cookagent.entity.ChatHistory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatHistoryMapper extends BaseMapper<ChatHistory> {
}