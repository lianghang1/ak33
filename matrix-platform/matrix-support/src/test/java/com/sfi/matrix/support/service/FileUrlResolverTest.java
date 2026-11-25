package com.sfi.matrix.support.service;

import com.sfi.conflux.file.config.FileChannelConfig;
import com.sfi.conflux.file.config.FileConfigProvider;
import com.sfi.conflux.file.engine.FileGateway;
import com.sfi.matrix.support.entity.SysFile;
import com.sfi.matrix.support.service.file.FileUrlResolver;
import org.dromara.x.file.storage.core.FileInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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
 * FileUrlResolver：公开文件直出原始地址、私有对象存储走预签名、
 * 本地文件走 /self/ 限时签名、验签覆盖过期与常量时间比较、domain 拼接。
 *
 * @author lianghang
 */
class FileUrlResolverTest {

    private FileGateway gateway;
    private FileConfigProvider configProvider;
    private SysFileService fileService;
    private FileUrlResolver resolver;

    @BeforeEach
    void setUp() {
        gateway = mock(FileGateway.class);
        configProvider = mock(FileConfigProvider.class);
        fileService = mock(SysFileService.class);
        resolver = new FileUrlResolver(gateway, configProvider, fileService);
        ReflectionTestUtils.setField(resolver, "presignSeconds", 300);
        ReflectionTestUtils.setField(resolver, "mode", "nginx");
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

    private void stubDomain(String domain) {
        Map<String, String> attr = new HashMap<>();
        attr.put("domain", domain);
        when(configProvider.getConfig("cfg-1", "local-plus"))
                .thenReturn(new FileChannelConfig("cfg-1", "local-plus", attr));
    }

    @Test
    void publicFile_returnsRawUrl_noSign() {
        SysFile f = file(false, "oss");
        assertEquals("https://cdn/2026/11/abc.jpg", resolver.url(f, false));
        assertEquals("https://cdn/2026/11/abc.min.jpg", resolver.url(f, true));
        verify(gateway, never()).generatePresignedUrl(any(), any(), any(), any(Date.class));
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

    @Test
    void localFile_nginxMode_signsSelfUrl() {
        stubDomain("http://localhost/");
        SysFile f = file(true, "local-plus");

        String url = resolver.url(f, false);
        assertTrue(url.startsWith("http://localhost/self/file?"), url);
        assertTrue(url.contains("fileId=1"), url);
        assertTrue(url.contains("thumb=0"), url);
        assertTrue(url.contains("&s="), url);
    }

    @Test
    void localFile_streamMode_usesAccessPath() {
        ReflectionTestUtils.setField(resolver, "mode", "stream");
        stubDomain("http://localhost");
        SysFile f = file(true, "local-plus");

        String url = resolver.url(f, true);
        assertTrue(url.startsWith("http://localhost/self/access?"), url);
        assertTrue(url.contains("thumb=1"), url);
    }

    @Test
    void signAndVerify_roundTrip() {
        long t = System.currentTimeMillis() / 1000L + 300;
        String sign = resolver.sign(1L, t, false);
        assertTrue(resolver.verify(1L, t, false, sign));
        assertFalse(resolver.verify(1L, t, true, sign), "thumb 参与签名，篡改后应失败");
        assertFalse(resolver.verify(2L, t, false, sign), "fileId 参与签名，篡改后应失败");
    }

    @Test
    void verify_expiredOrBlank_rejects() {
        long past = System.currentTimeMillis() / 1000L - 1;
        String sign = resolver.sign(1L, past, false);
        assertFalse(resolver.verify(1L, past, false, sign), "过期签名应拒绝");
        assertFalse(resolver.verify(1L, System.currentTimeMillis() / 1000L + 300, false, ""));
    }

    @Test
    void missingFile_throws() {
        when(fileService.get(99L)).thenReturn(null);
        assertThrows(com.sfi.scaffold.common.exception.BizException.class, () -> resolver.url(99L, false));
    }
}
