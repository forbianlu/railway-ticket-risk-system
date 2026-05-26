package com.example.railway.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

import com.example.railway.common.BusinessException;

@Service
public class CallbackSignatureService {

    public String paymentPlainText(String paymentNo,
                                   String callbackRequestId,
                                   BigDecimal amount,
                                   Boolean success,
                                   String timestamp) {
        return "paymentNo=" + normalize(paymentNo)
                + "&callbackRequestId=" + normalize(callbackRequestId)
                + "&amount=" + normalizeAmount(amount)
                + "&success=" + String.valueOf(Boolean.TRUE.equals(success))
                + "&timestamp=" + normalize(timestamp);
    }

    public String refundPlainText(String refundNo,
                                  String callbackRequestId,
                                  BigDecimal amount,
                                  Boolean success,
                                  String timestamp) {
        return "refundNo=" + normalize(refundNo)
                + "&callbackRequestId=" + normalize(callbackRequestId)
                + "&amount=" + normalizeAmount(amount)
                + "&success=" + String.valueOf(Boolean.TRUE.equals(success))
                + "&timestamp=" + normalize(timestamp);
    }

    public void verify(String plainText,
                       String signature,
                       String timestamp,
                       String secret,
                       boolean enabled,
                       long toleranceSeconds) {
        if (!enabled) {
            return;
        }
        if (signature == null || signature.trim().isEmpty()) {
            throw new BusinessException("回调签名不能为空");
        }
        verifyTimestamp(timestamp, toleranceSeconds);
        String expected = sign(plainText, secret);
        if (!constantTimeEquals(expected, signature.trim())) {
            throw new BusinessException("回调签名不匹配");
        }
    }

    public String sign(String plainText, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : digest) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("签名计算失败", exception);
        }
    }

    public String currentTimestamp() {
        return String.valueOf(Instant.now().toEpochMilli());
    }

    public String normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        return amount.stripTrailingZeros().toPlainString();
    }

    private void verifyTimestamp(String timestamp, long toleranceSeconds) {
        if (timestamp == null || timestamp.trim().isEmpty()) {
            throw new BusinessException("回调时间戳不能为空");
        }
        long callbackMillis;
        try {
            callbackMillis = Long.parseLong(timestamp.trim());
        } catch (NumberFormatException exception) {
            throw new BusinessException("回调时间戳不合法");
        }
        long deltaMillis = Math.abs(Instant.now().toEpochMilli() - callbackMillis);
        if (deltaMillis > toleranceSeconds * 1000L) {
            throw new BusinessException("回调时间戳已过期");
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (actual == null) {
            return false;
        }
        byte[] left = expected.getBytes(StandardCharsets.UTF_8);
        byte[] right = actual.getBytes(StandardCharsets.UTF_8);
        if (left.length != right.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < left.length; i++) {
            result |= left[i] ^ right[i];
        }
        return result == 0;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
