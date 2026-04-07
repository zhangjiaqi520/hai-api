package com.haiapi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.haiapi.entity.Channel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface ChannelMapper extends BaseMapper<Channel> {
    @Select("SELECT * FROM channels WHERE is_enabled = true AND status = 'active' ORDER BY priority DESC, weight DESC")
    List<Channel> selectEnabledChannels();
}
