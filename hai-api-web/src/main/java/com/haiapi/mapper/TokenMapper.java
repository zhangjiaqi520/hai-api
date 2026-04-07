package com.haiapi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.haiapi.entity.Token;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TokenMapper extends BaseMapper<Token> {
    @Select("SELECT * FROM tokens WHERE key_hash = #{keyHash} LIMIT 1")
    Token selectByKeyHash(String keyHash);

    @Select("UPDATE tokens SET quota_used = quota_used + #{increment}, last_used_at = NOW() WHERE id = #{tokenId}")
    void incrementUsage(String tokenId, long increment);
}
