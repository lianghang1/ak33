package com.sfi.matrix.iam.auth;

import com.sfi.conflux.auth.api.AuthResult;
import com.sfi.conflux.auth.api.AuthUser;
import com.sfi.conflux.auth.engine.AuthGateway;
import com.sfi.scaffold.common.exception.BizException;
import com.sfi.scaffold.starter.security.online.ExternalIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Conflux 身份核验适配器单测：凭证原样转交、AuthUser→ExternalIdentity 转换、失败显式抛错。
 * mock AuthGateway，不触发真实 conflux 网络。
 *
 * @author lianghang
 */
class ConfluxIdentityResolverTest {

    private AuthGateway authGateway;
    private ConfluxIdentityResolver resolver;

    @BeforeEach
    void setUp() {
        authGateway = mock(AuthGateway.class);
        resolver = new ConfluxIdentityResolver(authGateway);
    }

    private AuthUser user() {
        return new AuthUser()
                .setChannelCode("wechat_mp")
                .setOpenId("open-1")
                .setUnionId("union-1")
                .setUsername("张三")
                .setAvatar("http://avatar");
    }

    @Test
    void resolve_success_convertsAuthUser() {
        when(authGateway.parseCallback(anyString(), anyString(), any()))
                .thenReturn(AuthResult.ok(user()));

        Map<String, String> credentials = new LinkedHashMap<String, String>();
        credentials.put("code", "code-1");
        ExternalIdentity identity = resolver.resolve("wechat_mp", credentials);

        assertThat(identity.getOpenId()).isEqualTo("open-1");
        assertThat(identity.getUnionId()).isEqualTo("union-1");
        assertThat(identity.getNickname()).isEqualTo("张三");
        assertThat(identity.getAvatar()).isEqualTo("http://avatar");
        assertThat(identity.getProvider()).isEqualTo("wechat_mp");
    }

    @Test
    void resolve_defaultsConfigAndChannelToProvider() {
        when(authGateway.parseCallback(anyString(), anyString(), any()))
                .thenReturn(AuthResult.ok(user()));

        resolver.resolve("wechat_mp", new LinkedHashMap<String, String>());

        verify(authGateway).parseCallback(eq("wechat_mp"), eq("wechat_mp"), any());
    }

    @Test
    void resolve_explicitConfigAndChannel_fromCredentials() {
        when(authGateway.parseCallback(anyString(), anyString(), any()))
                .thenReturn(AuthResult.ok(user()));
        Map<String, String> credentials = new LinkedHashMap<String, String>();
        credentials.put("configId", "tenant-a");
        credentials.put("channelCode", "wechat_open");

        resolver.resolve("wechat_mp", credentials);

        verify(authGateway).parseCallback(eq("tenant-a"), eq("wechat_open"), any());
    }

    @Test
    void resolve_fail_throwsBizException() {
        when(authGateway.parseCallback(anyString(), anyString(), any()))
                .thenReturn(AuthResult.fail("ILLEGAL_STATE", "state 缺失"));

        assertThatThrownBy(() -> resolver.resolve("wechat_mp", new LinkedHashMap<String, String>()))
                .isInstanceOf(BizException.class);
    }

    @Test
    void resolve_nullResult_throwsBizException() {
        when(authGateway.parseCallback(anyString(), anyString(), any())).thenReturn(null);

        assertThatThrownBy(() -> resolver.resolve("wechat_mp", new LinkedHashMap<String, String>()))
                .isInstanceOf(BizException.class);
    }

    @Test
    void resolve_passesWholeCredentialsAsRawParams() {
        when(authGateway.parseCallback(anyString(), anyString(), any()))
                .thenReturn(AuthResult.ok(user()));
        Map<String, String> credentials = new LinkedHashMap<String, String>();
        credentials.put("code", "code-1");
        credentials.put("state", "state-1");

        resolver.resolve("wechat_mp", credentials);

        ArgumentCaptor<com.sfi.conflux.auth.api.AuthCallbackRequest> captor =
                ArgumentCaptor.forClass(com.sfi.conflux.auth.api.AuthCallbackRequest.class);
        verify(authGateway).parseCallback(anyString(), anyString(), captor.capture());
        assertThat(captor.getValue().param("code")).isEqualTo("code-1");
        assertThat(captor.getValue().param("state")).isEqualTo("state-1");
    }
}
