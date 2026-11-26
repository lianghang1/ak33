package com.sfi.matrix.support.service;

import com.sfi.conflux.file.config.FileChannelConfig;
import com.sfi.conflux.file.config.FileConfigProvider;
import com.sfi.conflux.file.engine.FileGateway;
import com.sfi.matrix.support.dto.FileConfigInfo;
import com.sfi.matrix.support.entity.SysFile;
import com.sfi.matrix.support.service.file.FileConfigInfoProvider;
import com.sfi.matrix.support.service.file.FileUrlResolver;
import org.dromara.x.file.storage.core.FileInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FileUrlResolver：本地 rawUrl 回填 /self/ 地址、公开直出不签名、私有本地动态签名、
 * 对象存储预签名、checkAccess 公开放行与私有验签、过期与篡改拒绝。
 *
 * @author lianghang
 */
class FileUrlResolverTest {

    private FileGateway gateway;
    private FileConfigProvider configProvider;
    private FileConfigInfoProvider configInfoProvider;
    private FileUrlResolver resolver;

    @BeforeEach
    void setUp() {
        gateway = mock(FileGateway.class);
        configProvider = mock(FileConfigProvider.class);
        configInfoProvider = mock(FileConfigInfoProvider.class);
        resolver = new FileUrlResolver(gateway, configProvider, configInfoProvider);
        FileConfigInfo cfg = new FileConfigInfo();
        cfg.setPresignSeconds(300);
        when(configInfoProvider.getConfig()).thenReturn(cfg);
    }

    private SysFile file(boolean privateAccess, String channelCode) {
        SysFile f = new SysFile();
        f.setId(1L);
        f.setConfigId("cfg-1");
        f.setChannelCode(channelCode);
        f.setPath("2026/11/");
        f.setFilename("abc.jpg");
        f.setRawUrl("https://cdn/2026/11/abc.jpg");
        f.setThPath("2026/11/");
        f.setThFilename("abc.min.jpg");
        f.setThRawUrl("https://cdn/2026/11/abc.min.jpg");
        f.setPrivateAccess(privateAccess);
        return f;
    }

    private FileConfigInfo fileConfig(String mode) {
        FileConfigInfo cfg = new FileConfigInfo();
        cfg.setLocalAccessMode(mode);
        cfg.setPresignSeconds(300);
        return cfg;
    }

    private void stubDomain(String domain) {
        Map<String, String> attr = new HashMap<>();
        attr.put("domain", domain);
        when(configProvider.getConfig("cfg-1", "local-plus"))
                .thenReturn(new FileChannelConfig("cfg-1", "local-plus", attr));
    }

    // ---- fillLocalUrls：本地回填 /self/ 基础地址 ----

    @Test
    void fillLocalUrls_nginxMode_writesSelfBaseUrl() {
        stubDomain("http://localhost/");
        SysFile f = file(false, "local-plus");

        resolver.fillLocalUrls(f);

        assertEquals("http://localhost/self/file?fileId=1&thumb=0", f.getRawUrl());
        assertEquals("http://localhost/self/file?fileId=1&thumb=1", f.getThRawUrl());
    }

    @Test
    void fillLocalUrls_streamMode_usesAccessPath() {
        when(configInfoProvider.getConfig()).thenReturn(fileConfig("stream"));
        stubDomain("http://localhost");
        SysFile f = file(false, "local-plus");

        resolver.fillLocalUrls(f);

        assertEquals("http://localhost/self/access?fileId=1&thumb=0", f.getRawUrl());
    }

    @Test
    void fillLocalUrls_noThumbnail_leavesThRawUrlUntouched() {
        stubDomain("http://localhost/");
        SysFile f = file(false, "local-plus");
        f.setThFilename(null);
        String originalTh = f.getThRawUrl();

        resolver.fillLocalUrls(f);

        assertEquals(originalTh, f.getThRawUrl());
    }

    @Test
    void fillLocalUrls_objectStorage_keepsSdkUrl() {
        SysFile f = file(false, "oss");

        resolver.fillLocalUrls(f);

        assertEquals("https://cdn/2026/11/abc.jpg", f.getRawUrl());
        verify(configProvider, never()).getConfig(any(), any());
    }

    // ---- url：公开直出 / 私有分流 ----

    @Test
    void publicFile_returnsRawUrl_noSign() {
        SysFile f = file(false, "oss");
        assertEquals("https://cdn/2026/11/abc.jpg", resolver.url(f, false));
        assertEquals("https://cdn/2026/11/abc.min.jpg", resolver.url(f, true));
        verify(gateway, never()).generatePresignedUrl(any(), any(), any(), any(Date.class));
    }

    @Test
    void publicLocal_returnsRawUrl_noSign() {
        stubDomain("http://localhost/");
        SysFile f = file(false, "local-plus");
        resolver.fillLocalUrls(f);

        String url = resolver.url(f, false);
        assertEquals("http://localhost/self/file?fileId=1&thumb=0", url);
        assertFalse(url.contains("&s="), "公开文件不应带签名");
    }

    @Test
    void privateLocal_appendsDynamicSignToRawUrl() {
        stubDomain("http://localhost/");
        SysFile f = file(true, "local-plus");
        resolver.fillLocalUrls(f);

        String url = resolver.url(f, false);
        assertTrue(url.startsWith("http://localhost/self/file?fileId=1&thumb=0&t="), url);
        assertTrue(url.contains("&s="), url);
    }

    @Test
    void privateOss_signsWithObjectKey() {
        SysFile f = file(true, "oss");
        when(gateway.generatePresignedUrl(eq("cfg-1"), eq("oss"), any(FileInfo.class), any(Date.class)))
                .thenReturn("https://signed/original");

        assertEquals("https://signed/original", resolver.url(f, false));

        org.mockito.ArgumentCaptor<FileInfo> cap = org.mockito.ArgumentCaptor.forClass(FileInfo.class);
        verify(gateway).generatePresignedUrl(eq("cfg-1"), eq("oss"), cap.capture(), any(Date.class));
        assertEquals("2026/11/", cap.getValue().getPath());
        assertEquals("abc.jpg", cap.getValue().getFilename());
    }

    @Test
    void privateOss_thumbnail_signsWithThCoordinates() {
        SysFile f = file(true, "oss");
        when(gateway.generatePresignedUrl(eq("cfg-1"), eq("oss"), any(FileInfo.class), any(Date.class)))
                .thenReturn("https://signed/thumb");

        assertEquals("https://signed/thumb", resolver.url(f, true));

        org.mockito.ArgumentCaptor<FileInfo> cap = org.mockito.ArgumentCaptor.forClass(FileInfo.class);
        verify(gateway).generatePresignedUrl(eq("cfg-1"), eq("oss"), cap.capture(), any(Date.class));
        assertEquals("2026/11/", cap.getValue().getPath());
        assertEquals("abc.min.jpg", cap.getValue().getFilename());
    }

    @Test
    void privateOss_platformNoPresign_throws() {
        SysFile f = file(true, "oss");
        when(gateway.generatePresignedUrl(any(), any(), any(), any(Date.class))).thenReturn(null);
        assertThrows(com.sfi.scaffold.common.exception.BizException.class, () -> resolver.url(f, false));
    }

    // ---- checkAccess：公开放行 / 私有验签 ----

    @Test
    void checkAccess_publicLocal_allowsWithoutSign() {
        assertTrue(resolver.checkAccess(file(false, "local-plus"), 0L, false, null), "公开本地文件无需签名即放行");
    }

    @Test
    void checkAccess_privateLocal_validSign_allows() {
        SysFile f = file(true, "local-plus");
        long t = System.currentTimeMillis() / 1000L + 300;
        assertTrue(resolver.checkAccess(f, t, false, resolver.sign(1L, t, false)));
    }

    @Test
    void checkAccess_privateLocal_tamperOrExpired_rejects() {
        SysFile f = file(true, "local-plus");
        long t = System.currentTimeMillis() / 1000L + 300;
        String sign = resolver.sign(1L, t, false);
        assertFalse(resolver.checkAccess(f, t, true, sign), "thumb 篡改应拒绝");
        assertFalse(resolver.checkAccess(f, t, false, resolver.sign(2L, t, false)), "签名属于其他文件应拒绝");
        assertFalse(resolver.checkAccess(null, t, false, sign), "文件不存在应拒绝");
        assertFalse(resolver.checkAccess(f, t, false, ""), "空签名应拒绝");
        assertFalse(resolver.checkAccess(f, System.currentTimeMillis() / 1000L - 1, false,
                resolver.sign(1L, System.currentTimeMillis() / 1000L - 1, false)), "过期应拒绝");
    }

    @Test
    void checkAccess_objectStorageOrMissing_rejects() {
        assertFalse(resolver.checkAccess(file(false, "oss"), 0L, false, null), "非本地文件不走本地准入");
        assertFalse(resolver.checkAccess(null, 0L, false, null), "不存在的文件拒绝");
    }
}
