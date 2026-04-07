package com.haiapi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.haiapi.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
