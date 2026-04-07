package com.haiapi.controller;

import com.haiapi.common.result.Result;
import com.haiapi.service.RelayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1")
public class RelayController {
    private final RelayService relayService;

    public RelayController(RelayService relayService) {
        this.relayService = relayService;
    }

    @PostMapping("/chat/completions")
    public Result<Map<String, Object>> chatCompletions(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> request) {
        String apiKey = extractApiKey(authorization);
        String ipAddress = getClientIp();
        log.info("Chat completions request - IP: {}, model: {}", ipAddress, request.get("model"));
        Map<String, Object> result = relayService.chatComplete(apiKey, request, ipAddress);
        return Result.success(result);
    }

    private String extractApiKey(String authorization) {
        if (authorization == null || authorization.isEmpty()) return null;
        if (authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return authorization;
    }

    private String getClientIp() {
        return "127.0.0.1";
    }

    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("ok");
    }

    @GetMapping("/models")
    public Result<Map<String, Object>> listModels() {
        Map<String, Object> response = Map.of(
                "object", "list",
                "data", new Object[]{
                        Map.of("id", "deepseek-chat", "object", "model", "created", System.currentTimeMillis() / 1000, "owned_by", "deepseek"),
                        Map.of("id", "qwen-turbo", "object", "model", "created", System.currentTimeMillis() / 1000, "owned_by", "alibaba"),
                        Map.of("id", "doubao-pro-32k", "object", "model", "created", System.currentTimeMillis() / 1000, "owned_by", "volcengine")
                }
        );
        return Result.success(response);
    }
}
