package com.geekyous.kb.config;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link ClientIpResolver} 单测 — 覆盖可信代理感知解析的核心场景与边界。
 *
 * @author Geekyous Guo
 */
class ClientIpResolverTest {

    /** 构造一个指定 remoteAddr 与 XFF 的请求 mock。xff=null 表示无该头。 */
    private static HttpServletRequest request(String remoteAddr, String xff) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRemoteAddr()).thenReturn(remoteAddr);
        when(req.getHeader("X-Forwarded-For")).thenReturn(xff);
        return req;
    }

    @Test
    @DisplayName("直连模式（空配置）：忽略 XFF，返回 TCP 对端地址")
    void directMode_ignoresXff() {
        ClientIpResolver resolver = new ClientIpResolver("");
        // 客户端伪造 XFF，但无代理配置 → XFF 完全忽略
        assertEquals("127.0.0.1", resolver.resolve(request("127.0.0.1", "1.2.3.4")));
    }

    @Test
    @DisplayName("单级代理 + 客户端伪造 XFF：取最右真实 IP，丢弃伪造的最左值")
    void singleProxy_spoofedXff_returnsRightmostReal() {
        ClientIpResolver resolver = new ClientIpResolver("172.16.0.0/12");
        // 客户端伪造 1.2.3.4，Nginx 追加真实 203.0.113.9 到末尾
        assertEquals("203.0.113.9", resolver.resolve(request("172.18.0.3", "1.2.3.4, 203.0.113.9")));
    }

    @Test
    @DisplayName("单级代理 + 正常 XFF（无伪造）：取真实 IP")
    void singleProxy_normalXff() {
        ClientIpResolver resolver = new ClientIpResolver("172.16.0.0/12");
        assertEquals("203.0.113.9", resolver.resolve(request("172.18.0.3", "203.0.113.9")));
    }

    @Test
    @DisplayName("直连打 :8080（remote 是公网、不可信）：忽略 XFF，返回真实 TCP 对端")
    void directHit_publicRemote_ignoresXff() {
        ClientIpResolver resolver = new ClientIpResolver("172.16.0.0/12");
        assertEquals("203.0.113.99", resolver.resolve(request("203.0.113.99", "1.2.3.4")));
    }

    @Test
    @DisplayName("IPv4-mapped IPv6 remote（::ffff:172.18.0.3）：正确识别为可信代理")
    void ipv4MappedRemote_trusted() {
        ClientIpResolver resolver = new ClientIpResolver("172.16.0.0/12");
        assertEquals("203.0.113.9", resolver.resolve(request("::ffff:172.18.0.3", "203.0.113.9")));
    }

    @Test
    @DisplayName("多级代理链：从右往左跳过可信代理（nginx + LB），停在实际客户端")
    void multiProxyChain_skipsTrustedProxies() {
        // 链路：client(203.0.113.9) → LB(10.0.0.5) → nginx(172.18.0.3) → backend
        ClientIpResolver resolver = new ClientIpResolver("172.16.0.0/12,10.0.0.0/8");
        assertEquals("203.0.113.9", resolver.resolve(request("172.18.0.3", "203.0.113.9, 10.0.0.5")));
    }

    @Test
    @DisplayName("XFF 全部是可信代理：回退到 TCP 对端地址")
    void allXffTrusted_fallsBackToRemote() {
        ClientIpResolver resolver = new ClientIpResolver("172.16.0.0/12,10.0.0.0/8");
        assertEquals("172.18.0.3", resolver.resolve(request("172.18.0.3", "10.0.0.5, 172.19.0.2")));
    }

    @Test
    @DisplayName("无 XFF 头：回退到 TCP 对端地址")
    void noXff_fallsBackToRemote() {
        ClientIpResolver resolver = new ClientIpResolver("172.16.0.0/12");
        assertEquals("172.18.0.3", resolver.resolve(request("172.18.0.3", null)));
    }

    @Test
    @DisplayName("XFF 含 unknown/空段：跳过，取首个有效非可信 IP")
    void xffWithUnknownSegments_skipped() {
        ClientIpResolver resolver = new ClientIpResolver("172.16.0.0/12");
        assertEquals("203.0.113.9", resolver.resolve(request("172.18.0.3", "unknown, , 203.0.113.9")));
    }

    @Test
    @DisplayName("CIDR /12 边界：172.31.x.x 在范围内可信，172.32.x.x 越界不可信")
    void cidrBoundary() {
        ClientIpResolver resolver = new ClientIpResolver("172.16.0.0/12");
        // 172.31.0.1 在 172.16.0.0/12 内 → 信任代理，返回 XFF 真实 IP
        assertEquals("203.0.113.9", resolver.resolve(request("172.31.0.1", "203.0.113.9")));
        // 172.32.0.1 越界 → 不可信（直连模式），XFF 被忽略
        assertEquals("172.32.0.1", resolver.resolve(request("172.32.0.1", "203.0.113.9")));
    }

    @Test
    @DisplayName("精确 IP 配置（::1 环回）：匹配可信后从 XFF 取真实 IP")
    void exactIpConfig() {
        ClientIpResolver resolver = new ClientIpResolver("::1");
        assertEquals("203.0.113.9", resolver.resolve(request("::1", "203.0.113.9")));
    }

    @Test
    @DisplayName("DNS 安全：XFF 含主机名 token，不解析、不崩溃，按非可信原样返回")
    void xffWithHostname_doesNotTriggerDns() {
        ClientIpResolver resolver = new ClientIpResolver("172.16.0.0/12");
        // 主机名不匹配可信代理 → 作为非可信 candidate 原样返回（不会调用 InetAddress.getByName）
        assertEquals("evil.com", resolver.resolve(request("172.18.0.3", "evil.com")));
    }

    @Test
    @DisplayName("remote 不可识别（null/unknown）：resolve 返回 null，交由调用方兜底")
    void unresolvableRemote_returnsNull() {
        ClientIpResolver resolver = new ClientIpResolver("");
        assertNull(resolver.resolve(request(null, "1.2.3.4")));
        assertNull(resolver.resolve(request("unknown", "1.2.3.4")));
    }
}
