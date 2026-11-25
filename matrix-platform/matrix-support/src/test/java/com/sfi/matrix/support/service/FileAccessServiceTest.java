package com.sfi.matrix.support.service;

import com.sfi.conflux.file.engine.FileGateway;
import com.sfi.matrix.support.entity.SysFile;
import com.sfi.matrix.support.service.file.FileAccessService;
import org.dromara.x.file.storage.core.FileInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FileAccessService：公开文件直出原始地址、私有文件按对象坐标签名、缩略图用独立坐标、
 * 记录不存在抛异常、平台不支持签名时抛异常。
 *
 * @author lianghang
 */
class FileAccessServiceTest {

    private FileGateway gateway;
    private SysFileService fileService;
    private FileAccessService service;

    @BeforeEach
    void setUp() {
        gateway = mock(FileGateway.class);
        fileService = mock(SysFileService.class);
        service = new FileAccessService(gateway, fileService);
        ReflectionTestUtils.setField(service, "presignSeconds", 300);
    }

    private SysFile file(boolean privateAccess) {
        SysFile f = new SysFile();
        f.setConfigId("cfg-1");
        f.setChannelCode("oss");
        f.setPath("2026/11/");
        f.setFilename("abc.jpg");
        f.setRawUrl("https://cdn/2026/11/abc.jpg");
        f.setThPath("2026/11/");
        f.setThFilename("abc.min.jpg");
        f.setThRawUrl("https://cdn/2026/11/abc.min.jpg");
        f.setPrivateAccess(privateAccess);
        return f;
    }

    @Test
    void publicFile_returnsRawUrl_noSign() {
        when(fileService.get(1L)).thenReturn(file(false));

        assertEquals("https://cdn/2026/11/abc.jpg", service.url(1L, false));
        verify(gateway, never()).generatePresignedUrl(any(), any(), any(), any(Date.class));
    }

    @Test
    void publicFile_thumbnail_returnsThRawUrl() {
        when(fileService.get(1L)).thenReturn(file(false));

        assertEquals("https://cdn/2026/11/abc.min.jpg", service.url(1L, true));
        verify(gateway, never()).generatePresignedUrl(any(), any(), any(), any(Date.class));
    }

    @Test
    void privateFile_signsWithObjectKey() {
        when(fileService.get(1L)).thenReturn(file(true));
        when(gateway.generatePresignedUrl(eq("cfg-1"), eq("oss"), any(FileInfo.class), any(Date.class)))
                .thenReturn("https://signed/original");

        assertEquals("https://signed/original", service.url(1L, false));

        org.mockito.ArgumentCaptor<FileInfo> cap = org.mockito.ArgumentCaptor.forClass(FileInfo.class);
        verify(gateway).generatePresignedUrl(eq("cfg-1"), eq("oss"), cap.capture(), any(Date.class));
        assertEquals("2026/11/", cap.getValue().getPath());
        assertEquals("abc.jpg", cap.getValue().getFilename());
    }

    @Test
    void privateFile_thumbnail_signsWithThCoordinates() {
        when(fileService.get(1L)).thenReturn(file(true));
        when(gateway.generatePresignedUrl(eq("cfg-1"), eq("oss"), any(FileInfo.class), any(Date.class)))
                .thenReturn("https://signed/thumb");

        assertEquals("https://signed/thumb", service.url(1L, true));

        org.mockito.ArgumentCaptor<FileInfo> cap = org.mockito.ArgumentCaptor.forClass(FileInfo.class);
        verify(gateway).generatePresignedUrl(eq("cfg-1"), eq("oss"), cap.capture(), any(Date.class));
        assertEquals("2026/11/", cap.getValue().getPath());
        assertEquals("abc.min.jpg", cap.getValue().getFilename());
    }

    @Test
    void missingFile_throws() {
        when(fileService.get(99L)).thenReturn(null);
        assertThrows(com.sfi.scaffold.common.exception.BizException.class, () -> service.url(99L, false));
    }

    @Test
    void privateFile_platformNoPresign_throws() {
        when(fileService.get(1L)).thenReturn(file(true));
        when(gateway.generatePresignedUrl(any(), any(), any(), any(Date.class))).thenReturn(null);
        assertThrows(com.sfi.scaffold.common.exception.BizException.class, () -> service.url(1L, false));
    }
}
