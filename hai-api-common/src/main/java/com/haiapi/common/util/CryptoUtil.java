package com.haiapi.common.util;

import cn.hutool.crypto.SecureUtil;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public class CryptoUtil {
    private CryptoUtil() {}

    public static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256加密失败", e);
        }
    }

    public static String base64Encode(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    public static String base64Decode(String encodedText) {
        return new String(Base64.getDecoder().decode(encodedText), StandardCharsets.UTF_8);
    }

    public static String aesEncrypt(String text) {
        return SecureUtil.aes("hai-api-aes-key!".getBytes()).encryptBase64(text);
    }

    public static String aesDecrypt(String encryptedText) {
        return SecureUtil.aes("hai-api-aes-key!".getBytes()).decryptStr(encryptedText);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
