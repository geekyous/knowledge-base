package com.company.kb.utils;

/**
 * 敏感字段脱敏工具 — 对 API 响应中的敏感数据进行掩码处理
 * 敏感字段脱敏工具 — 对 API 响应中的敏感数据进行掩码处理
 *
 * <h3>支持的脱敏类型</h3>
 * <table>
 *   <tr><th>类型</th><th>原始值</th><th>脱敏后</th></tr>
 *   <tr><td>邮箱</td><td>user@example.com</td><td>u***@example.com</td></tr>
 *   <tr><td>手机号</td><td>13812345678</td><td>138****5678</td></tr>
 *   <tr><td>身份证</td><td>110101199001011234</td><td>1101****1234</td></tr>
 * </table>
 *
 * @author Geekyous
 * @since 1.0.0
 */
public class SensitiveFieldUtil {

    private SensitiveFieldUtil() {
        // 工具类禁止实例化
    }

    /**
     * 邮箱脱敏：保留首字母 + *** + @及之后内容
     *
     * @param email 原始邮箱
     * @return 脱敏后的邮箱，如 u***@example.com
     */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            // 无效邮箱格式，返回原始值
            return email;
        }
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        // 保留第一个字符 + ***，最少显示 1 个字符
        char firstChar = localPart.charAt(0);
        return firstChar + "***" + domain;
    }

    /**
     * 手机号脱敏：保留前 3 位 + **** + 后 4 位
     *
     * @param phone 原始手机号
     * @return 脱敏后的手机号，如 138****5678
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }
        // 去除可能的空格和横线
        String clean = phone.replaceAll("[\\s\\-]", "");
        if (clean.length() < 7) {
            // 号码太短，不全脱敏
            return clean;
        }
        return clean.substring(0, 3) + "****" + clean.substring(clean.length() - 4);
    }

    /**
     * 身份证号脱敏：保留前 4 位 + **** + 后 4 位
     *
     * @param idCard 原始身份证号
     * @return 脱敏后的身份证号，如 1101****1234
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.isBlank()) {
            return idCard;
        }
        if (idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 4) + "****" + idCard.substring(idCard.length() - 4);
    }
}
