package com.haiapi.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.haiapi.common.constant.SystemConstant;
import com.haiapi.common.exception.BusinessException;
import com.haiapi.common.util.CryptoUtil;
import com.haiapi.entity.Channel;
import com.haiapi.entity.Token;
import com.haiapi.entity.UsageLog;
import com.haiapi.mapper.ChannelMapper;
import com.haiapi.mapper.TokenMapper;
import com.haiapi.mapper.UsageLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RelayService {

    private final ChannelMapper channelMapper;
    private final TokenMapper tokenMapper;
    private final UsageLogMapper usageLogMapper;
    private final WebClient webClient;

    public RelayService(ChannelMapper channelMapper,
                       TokenMapper tokenMapper,
                       UsageLogMapper usageLogMapper) {
        this.channelMapper = channelMapper;
        this.tokenMapper = tokenMapper;
        this.usageLogMapper = usageLogMapper;
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
    }

    public Map<String, Object> chatComplete(String apiKey, Map<String, Object> request, String ipAddress) {
        long startTime = System.currentTimeMillis();

        try {
            Token token = validateToken(apiKey);
            if (token == null) {
                throw BusinessException.of(401, "Invalid or missing API key");
            }

            String model = (String) request.get("model");
            if (model == null || model.isEmpty()) {
                throw BusinessException.of(400, "Model parameter is required");
            }

            if (!token.isModelAllowed(model)) {
                throw BusinessException.of(403, "Model not allowed for this key");
            }

            Channel channel = selectChannel(model);
            if (channel == null) {
                throw BusinessException.of(503, "No available channel for model: " + model);
            }

            log.info("Relay request - token: {}, model: {}, channel: {}",
                    token.getName(), model, channel.getName());

            String response = relayToUpstream(channel, request);

            long responseTime = System.currentTimeMillis() - startTime;
            JSONObject responseJson = JSON.parseObject(response);
            int inputTokens = calculateInputTokens(request);
            int outputTokens = calculateOutputTokens(responseJson);

            recordUsage(token, channel, model, inputTokens, outputTokens, responseTime, 200, null, ipAddress);

            Map<String, Object> result = new HashMap<>();
            result.put("response", responseJson);
            result.put("channel", channel.getName());
            result.put("responseTime", responseTime);

            return result;

        } catch (BusinessException e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.warn("Business exception: {}", e.getMessage());
            recordUsage(null, null, (String) request.get("model"), 0, 0, responseTime,
                    e.getCode(), e.getMessage(), ipAddress);
            throw e;
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.error("Unexpected error in chat complete", e);
            recordUsage(null, null, (String) request.get("model"), 0, 0, responseTime,
                    500, e.getMessage(), ipAddress);
            throw BusinessException.of(500, "Internal server error: " + e.getMessage());
        }
    }

    private Token validateToken(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return null;
        }

        if (!apiKey.startsWith(SystemConstant.TOKEN_PREFIX)) {
            apiKey = SystemConstant.TOKEN_PREFIX + apiKey;
        }

        String keyHash = hashToken(apiKey);
        Token token = tokenMapper.selectByKeyHash(keyHash);

        if (token == null) {
            return null;
        }

        if (!token.isValid()) {
            if (token.isExpired()) {
                log.warn("Token expired: {}", token.getName());
            } else if (token.isQuotaExceeded()) {
                log.warn("Token quota exceeded: {}", token.getName());
            } else if (!token.getIsEnabled()) {
                log.warn("Token disabled: {}", token.getName());
            }
            return null;
        }

        return token;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private Channel selectChannel(String model) {
        List<Channel> availableChannels = channelMapper.selectEnabledChannels();

        List<Channel> eligibleChannels = availableChannels.stream()
                .filter(ch -> ch.supportsModel(model))
                .collect(Collectors.toList());

        if (eligibleChannels.isEmpty()) {
            log.warn("No channel available for model: {}", model);
            return null;
        }

        return weightedRandomSelect(eligibleChannels);
    }

    private Channel weightedRandomSelect(List<Channel> channels) {
        int totalWeight = channels.stream()
                .mapToInt(Channel::getWeight)
                .sum();

        int random = new Random().nextInt(totalWeight);

        int cumulativeWeight = 0;
        for (Channel channel : channels) {
            cumulativeWeight += channel.getWeight();
            if (random < cumulativeWeight) {
                return channel;
            }
        }

        return channels.get(0);
    }

    private String relayToUpstream(Channel channel, Map<String, Object> request) {
        try {
            String apiKey = CryptoUtil.aesDecrypt(channel.getKeyEncrypted());

            String url = channel.getBaseUrl();
            if (!url.endsWith("/")) {
                url += "/";
            }
            url += "chat/completions";

            log.debug("Relaying to: {}", url);

            String response = webClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(JSON.toJSONString(request))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(120))
                    .block();

            return response;

        } catch (Exception e) {
            log.error("Failed to relay request to upstream", e);
            throw BusinessException.of(502, "Failed to relay request: " + e.getMessage());
        }
    }

    private int calculateInputTokens(Map<String, Object> request) {
        try {
            Object messagesObj = request.get("messages");
            if (messagesObj instanceof List) {
                List<?> messages = (List<?>) messagesObj;
                int total = 0;
                for (Object msg : messages) {
                    if (msg instanceof Map) {
                        Map<?, ?> message = (Map<?, ?>) msg;
                        Object content = message.get("content");
                        if (content != null) {
                            total += content.toString().length() / 4;
                        }
                    }
                }
                return total;
            }
        } catch (Exception e) {
            log.warn("Failed to calculate input tokens", e);
        }
        return 0;
    }

    private int calculateOutputTokens(JSONObject response) {
        try {
            JSONArray choices = response.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                JSONObject firstChoice = choices.getJSONObject(0);
                JSONObject message = firstChoice.getJSONObject("message");
                if (message != null) {
                    String content = message.getString("content");
                    if (content != null) {
                        return content.length() / 4;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to calculate output tokens", e);
        }
        return 0;
    }

    private void recordUsage(Token token, Channel channel, String model,
                            int inputTokens, int outputTokens, long responseTime,
                            int statusCode, String errorType, String ipAddress) {
        try {
            UsageLog usageLog = new UsageLog();
            if (token != null) {
                usageLog.setTokenId(token.getId());
            }
            if (channel != null) {
                usageLog.setChannelId(channel.getId());
            }
            usageLog.setModel(model);
            usageLog.setInputTokens(inputTokens);
            usageLog.setOutputTokens(outputTokens);
            usageLog.setResponseTimeMs((int) responseTime);
            usageLog.setStatusCode(statusCode);
            usageLog.setErrorType(errorType);
            usageLog.setIpAddress(ipAddress);
            usageLog.setCreatedAt(LocalDateTime.now());

            usageLogMapper.insert(usageLog);

            if (token != null && statusCode >= 200 && statusCode < 300) {
                tokenMapper.incrementUsage(token.getId(), inputTokens + outputTokens);
            }

            if (channel != null) {
                updateChannelStats(channel, responseTime, statusCode);
            }

            log.debug("Usage recorded - model: {}, tokens: {}/{}, time: {}ms, status: {}",
                    model, inputTokens, outputTokens, responseTime, statusCode);

        } catch (Exception e) {
            log.error("Failed to record usage", e);
        }
    }

    private void updateChannelStats(Channel channel, long responseTime, int statusCode) {
        try {
            if (statusCode >= 200 && statusCode < 300) {
                channel.setLastSuccessAt(LocalDateTime.now());
                channelMapper.updateById(channel);
            }
        } catch (Exception e) {
            log.error("Failed to update channel stats", e);
        }
    }
}
