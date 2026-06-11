package com.duanruixin.pulse.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.duanruixin.pulse.app.entity.Message;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}