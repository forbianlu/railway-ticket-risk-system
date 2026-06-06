package com.example.railway.common;

public final class MaskingUtils {

    private MaskingUtils() {
    }

    public static String maskIdNo(String idNo) {
        if (idNo == null || idNo.trim().isEmpty()) {
            return "";
        }
        String value = idNo.trim();
        if (value.length() <= 4) {
            return repeat("*", value.length());
        }
        if (value.length() <= 8) {
            return value.substring(0, 1) + repeat("*", value.length() - 2) + value.substring(value.length() - 1);
        }
        return value.substring(0, 4) + repeat("*", Math.max(4, value.length() - 8)) + value.substring(value.length() - 4);
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return "";
        }
        String value = phone.trim();
        if (value.length() <= 4) {
            return repeat("*", value.length());
        }
        if (value.length() <= 7) {
            return value.substring(0, 2) + repeat("*", value.length() - 4) + value.substring(value.length() - 2);
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }

    private static String repeat(String text, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(text);
        }
        return builder.toString();
    }
}
