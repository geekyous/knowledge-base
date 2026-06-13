package com.geekyous.kb.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 可信代理感知的客户端 IP 解析器。
 *
 * <p>解决两个安全缺陷：
 * <ol>
 *   <li><b>X-Forwarded-For 伪造</b>：Nginx 用 {@code $proxy_add_x_forwarded_for} 把真实客户端 IP
 *       <b>追加到 XFF 末尾</b>，客户端伪造的 IP 在最左。旧代码取最左值 = 直接采用伪造值，
 *       攻击者每请求换一个 IP 即可绕过按 IP 限流。</li>
 *   <li><b>代理后 IP 维度丢失</b>：经 Nginx 转发时 {@code getRemoteAddr()} = Nginx 容器 IP，
 *       所有请求塌缩到同一个限流桶。</li>
 * </ol>
 *
 * <p>核心算法：仅当 TCP 对端是可信代理时才读 XFF，并从右往左跳过仍是可信代理的条目，
 * 取第一个非可信 IP 作为真实客户端。客户端伪造的最左值永远不会被读到。
 *
 * <p>直连场景（对端不在可信列表，如绕过 Nginx 直打 :8080）：XFF 不可信，直接返回 TCP 对端地址。
 * 配置为空（IDE 直跑）时所有来源都视为不可信 = 直连模式。
 *
 * <h3>DNS 安全</h3>
 * <p>{@code getRemoteAddr()} 永远返回数值 IP 字面量，对其调用 {@link InetAddress#getByName}
 * 不会触发 DNS。但 XFF 来自不可信客户端，可能塞入主机名（如 {@code evil.com}）——若对其调用
 * {@code InetAddress.getByName} 会触发 DNS 查询（SSRF/信息泄露向量）。因此 XFF token 只走
 * "严格正则 + 字符串前缀"的纯字符串匹配路径，绝不解析主机名。
 *
 * @author Geekyous Guo
 */
@Slf4j
@Component
public class ClientIpResolver {

    private static final String XFF_HEADER = "X-Forwarded-For";
    private static final String IPV4_MAPPED_PREFIX = "::ffff:";

    /** 严格 dotted-quad 字面量（仅校验结构，八段范围在 parseIpv4 内校验）。 */
    private static final Pattern IPV4 = Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");

    /** 启动时一次性解析好的不可变匹配器列表；空 = 直连模式（任何来源都不可信）。 */
    private final List<IpMatcher> trustedMatchers;

    public ClientIpResolver(@Value("${app.security.trusted-proxies:}") String rawTrustedProxies) {
        this.trustedMatchers = parse(rawTrustedProxies);
    }

    /**
     * 解析请求的真实客户端 IP。
     *
     * @return 归一化后的客户端 IP；无法识别（null/空/unknown）时返回 null，交由调用方兜底。
     */
    public String resolve(HttpServletRequest request) {
        String remote = request.getRemoteAddr();

        // 直连模式 / TCP 对端不是可信代理 → XFF 不可信，直接用对端地址
        if (!isTrusted(remote, true)) {
            return usable(remote);
        }

        // 对端是可信代理 → 从 XFF 右往左找第一个非可信 IP（= 真实客户端）
        String xff = request.getHeader(XFF_HEADER);
        if (xff != null && !xff.isBlank()) {
            String[] parts = xff.split(",");
            for (int i = parts.length - 1; i >= 0; i--) {
                String candidate = parts[i].trim();
                if (isUnknown(candidate)) {
                    continue;
                }
                if (!isTrusted(candidate, false)) {
                    return usable(candidate);
                }
                // 仍是可信代理 → 继续往左
            }
        }

        // XFF 全是可信代理 / 无 XFF → 回退 TCP 对端
        return usable(remote);
    }

    /**
     * 判断 IP 是否属于可信代理。
     *
     * @param ip            待判断的 IP
     * @param trustedSource true=来自 getRemoteAddr（可信，可全解析）；false=来自 XFF（不可信，仅字符串匹配）
     */
    private boolean isTrusted(String ip, boolean trustedSource) {
        if (isUnknown(ip)) {
            return false;
        }
        String trimmed = ip.trim();
        Integer v4 = trustedSource ? toIpv4IntFromTrusted(trimmed) : toIpv4IntSafe(trimmed);
        if (v4 != null) {
            for (IpMatcher matcher : trustedMatchers) {
                if (matcher.matchesIpv4(v4)) {
                    return true;
                }
            }
        }
        for (IpMatcher matcher : trustedMatchers) {
            if (matcher.matchesExact(trimmed)) {
                return true;
            }
        }
        return false;
    }

    /** 归一化为可用 key 字符串：去空白、提取 IPv4-mapped、unknown → null。纯字符串操作，无 DNS。 */
    private static String usable(String ip) {
        if (isUnknown(ip)) {
            return null;
        }
        String trimmed = ip.trim();
        String mapped = stripMappedPrefix(trimmed);
        return mapped != null ? mapped : trimmed;
    }

    private static boolean isUnknown(String ip) {
        return ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip.trim());
    }

    // ------------------------------------------------------------------
    // IPv4 int 解析（两种信任级别）
    // ------------------------------------------------------------------

    /**
     * 可信来源（getRemoteAddr）：用 {@link InetAddress} 全解析，正确处理所有 IPv4-mapped IPv6 形式。
     * 仅在可信来源调用——getRemoteAddr 必为数值字面量，不会触发 DNS。
     */
    private static Integer toIpv4IntFromTrusted(String ip) {
        try {
            byte[] b = InetAddress.getByName(ip).getAddress();
            if (b.length == 4) {
                return bytesToInt(b, 0);
            }
            if (b.length == 16 && isIpv4Mapped(b)) {
                return bytesToInt(b, 12);
            }
        } catch (Exception ignored) {
            // 解析失败 → 保守视为非 IPv4
        }
        return null;
    }

    /**
     * 不可信来源（XFF）：仅接受严格 dotted-quad 或 {@code ::ffff:} 前缀形式，DNS 安全。
     */
    private static Integer toIpv4IntSafe(String ip) {
        if (isStrictIpv4(ip)) {
            return parseIpv4(ip);
        }
        String mapped = stripMappedPrefix(ip);
        return mapped != null ? parseIpv4(mapped) : null;
    }

    private static boolean isStrictIpv4(String s) {
        Matcher m = IPV4.matcher(s);
        if (!m.matches()) {
            return false;
        }
        for (int i = 1; i <= 4; i++) {
            if (Integer.parseInt(m.group(i)) > 255) {
                return false;
            }
        }
        return true;
    }

    private static int parseIpv4(String s) {
        Matcher m = IPV4.matcher(s);
        if (!m.matches()) {
            throw new IllegalArgumentException("not ipv4: " + s);
        }
        int b0 = Integer.parseInt(m.group(1));
        int b1 = Integer.parseInt(m.group(2));
        int b2 = Integer.parseInt(m.group(3));
        int b3 = Integer.parseInt(m.group(4));
        if (b0 > 255 || b1 > 255 || b2 > 255 || b3 > 255) {
            throw new IllegalArgumentException("invalid octet: " + s);
        }
        return (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
    }

    private static int bytesToInt(byte[] b, int offset) {
        return ((b[offset] & 0xFF) << 24)
                | ((b[offset + 1] & 0xFF) << 16)
                | ((b[offset + 2] & 0xFF) << 8)
                | (b[offset + 3] & 0xFF);
    }

    /** IPv4-mapped IPv6（::ffff:a.b.c.d）：前 10 字节为 0，第 11、12 字节为 0xFF。 */
    private static boolean isIpv4Mapped(byte[] b) {
        for (int i = 0; i < 10; i++) {
            if (b[i] != 0) {
                return false;
            }
        }
        return b[10] == (byte) 0xFF && b[11] == (byte) 0xFF;
    }

    /** 纯字符串检测 ::ffff: 前缀并提取尾部 dotted-quad；不匹配返回 null。 */
    private static String stripMappedPrefix(String s) {
        if (s.length() <= IPV4_MAPPED_PREFIX.length()) {
            return null;
        }
        if (!s.substring(0, IPV4_MAPPED_PREFIX.length()).equalsIgnoreCase(IPV4_MAPPED_PREFIX)) {
            return null;
        }
        String tail = s.substring(IPV4_MAPPED_PREFIX.length());
        return isStrictIpv4(tail) ? tail : null;
    }

    // ------------------------------------------------------------------
    // 配置解析
    // ------------------------------------------------------------------

    private List<IpMatcher> parse(String raw) {
        List<IpMatcher> list = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return list;
        }
        for (String token : raw.split(",")) {
            String t = token.trim();
            if (t.isEmpty() || "unknown".equalsIgnoreCase(t)) {
                continue;
            }
            try {
                list.add(t.contains("/") ? new CidrV4Matcher(t) : new ExactIpMatcher(t));
            } catch (Exception e) {
                log.warn("忽略无法解析的可信代理配置项 '{}': {}", t, e.getMessage());
            }
        }
        return list;
    }

    /** IP 匹配器：IPv4 CIDR 或精确值。 */
    private interface IpMatcher {
        boolean matchesIpv4(int ip);

        boolean matchesExact(String ip);
    }

    /** IPv4 CIDR 匹配：a.b.c.d/prefix。 */
    private static final class CidrV4Matcher implements IpMatcher {
        private final int network;
        private final int mask;

        CidrV4Matcher(String cidr) {
            int slash = cidr.indexOf('/');
            int prefix = Integer.parseInt(cidr.substring(slash + 1).trim());
            if (prefix < 0 || prefix > 32) {
                throw new IllegalArgumentException("prefix out of range [0,32]: " + prefix);
            }
            this.network = parseIpv4(cidr.substring(0, slash).trim());
            this.mask = prefix == 0 ? 0 : 0xFFFFFFFF << (32 - prefix);
        }

        @Override
        public boolean matchesIpv4(int ip) {
            return (ip & mask) == (network & mask);
        }

        @Override
        public boolean matchesExact(String ip) {
            return false;
        }
    }

    /** 精确匹配（IPv4 精确值或 IPv6 如 ::1）。 */
    private static final class ExactIpMatcher implements IpMatcher {
        private final String expected;

        ExactIpMatcher(String ip) {
            this.expected = ip.trim();
        }

        @Override
        public boolean matchesIpv4(int ip) {
            return false;
        }

        @Override
        public boolean matchesExact(String ip) {
            return expected.equalsIgnoreCase(ip);
        }
    }
}
