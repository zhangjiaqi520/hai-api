package com.haiapi.controller;

import com.haiapi.common.result.Result;
import com.haiapi.entity.Channel;
import com.haiapi.entity.Token;
import com.haiapi.mapper.ChannelMapper;
import com.haiapi.mapper.TokenMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/test")
public class TestController {
    private final ChannelMapper channelMapper;
    private final TokenMapper tokenMapper;

    public TestController(ChannelMapper channelMapper, TokenMapper tokenMapper) {
        this.channelMapper = channelMapper;
        this.tokenMapper = tokenMapper;
    }

    @PostMapping("/channel")
    public Result<Channel> createTestChannel(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        String type = request.get("type");
        String apiKey = request.get("apiKey");
        String baseUrl = request.get("baseUrl");

        if (name == null || type == null || apiKey == null) {
            return Result.error(400, "Missing required parameters");
        }

        Channel channel = new Channel();
        channel.setName(name);
        channel.setType(type);
        channel.setKeyEncrypted(apiKey);
        channel.setBaseUrl(baseUrl != null ? baseUrl : getDefaultBaseUrl(type));
        channel.setModels("[\"*\"]");
        channel.setPriority(0);
        channel.setWeight(100);
        channel.setIsEnabled(true);
        channel.setStatus("active");

        channelMapper.insert(channel);
        log.info("Created test channel: {}", name);
        return Result.success(channel);
    }

    @PostMapping("/token")
    public Result<Map<String, Object>> createTestToken(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        String allowedModels = request.getOrDefault("allowedModels", "[\"*\"]");

        if (name == null) {
            return Result.error(400, "Name is required");
        }

        String rawToken = "sk-hai-" + UUID.randomUUID().toString().replace("-", "");

        Token token = new Token();
        token.setName(name);
        token.setKeyHash(hashToken(rawToken));
        token.setAllowedModels(allowedModels);
        token.setQuotaLimit(10000L);
        token.setQuotaUsed(0L);
        token.setUnlimited(false);
        token.setIsEnabled(true);
        token.setStatus("active");

        tokenMapper.insert(token);

        Map<String, Object> result = new HashMap<>();
        result.put("id", token.getId());
        result.put("name", token.getName());
        result.put("token", rawToken);
        result.put("allowedModels", allowedModels);

        log.info("Created test token: {}, key: {}", name, rawToken);
        return Result.success(result);
    }

    @GetMapping("/channels")
    public Result<List<Channel>> listChannels() {
        List<Channel> channels = channelMapper.selectEnabledChannels();
        return Result.success(channels);
    }

    private String getDefaultBaseUrl(String type) {
        switch (type) {
            case "deepseek": return "https://api.deepseek.com/v1";
            case "qwen": return "https://dashscope.aliyuncs.com/compatible-mode/v1";
            case "doubao": return "https://ark.cn-beijing.volces.com/api/v3";
            case "zhipu": return "https://open.bigmodel.cn/api/paas/v4";
            case "minimax": return "https://api.minimax.chat/v1";
            case "kimi": return "https://api.moonshot.cn/v1";
            default: return "";
        }
    }

    private String hashToken(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }
}
