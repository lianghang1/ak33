package com.sfi.matrix.support.controller;

import com.sfi.conflux.file.engine.FileGateway;
import com.sfi.matrix.support.controller.file.LocalFileAccessController;
import com.sfi.matrix.support.entity.SysFile;
import com.sfi.matrix.support.service.SysFileService;
import com.sfi.matrix.support.service.file.FileUrlResolver;
import com.sfi.scaffold.starter.web.exception.GlobalExceptionHandler;
import org.dromara.x.file.storage.core.Downloader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LocalFileAccessController：验签放行/拒绝、nginx X-Accel-Redirect 路径由 fileId 查库得出、
 * stream 模式 SDK 下载流写回。
 *
 * @author lianghang
 */
class LocalFileAccessControllerTest {

    private FileUrlResolver urlResolver;
    private SysFileService fileService;
    private FileGateway fileGateway;
    private LocalFileAccessController controller;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        urlResolver = mock(FileUrlResolver.class);
        when(urlResolver.getNginxInternalPrefix()).thenReturn("/__self_internal__/");
        fileService = mock(SysFileService.class);
        fileGateway = mock(FileGateway.class);
        controller = new LocalFileAccessController(urlResolver, fileService, fileGateway);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private SysFile file() {
        SysFile f = new SysFile();
        f.setId(1L);
        f.setConfigId("cfg-1");
        f.setChannelCode("local-plus");
        f.setPath("avatar/2026/11/");
        f.setFilename("abc.jpg");
        f.setThPath("avatar/2026/11/");
        f.setThFilename("abc.min.jpg");
        f.setContentType("image/jpeg");
        return f;
    }

    @Test
    void file_signInvalid_rejected_noHeader() throws Exception {
        when(urlResolver.checkAccess(any(SysFile.class), anyLong(), org.mockito.ArgumentMatchers.anyBoolean(), anyString()))
                .thenReturn(false);

        mvc.perform(get("/self/file").param("fileId", "1").param("s", "bad"))
                .andExpect(status().isOk());

        verify(fileGateway, never()).download(anyString(), anyString(), any());
    }

    @Test
    void file_validSign_setsXAccelRedirectFromDb() throws Exception {
        when(urlResolver.checkAccess(any(SysFile.class), anyLong(), org.mockito.ArgumentMatchers.anyBoolean(), anyString()))
                .thenReturn(true);
        when(fileService.get(1L)).thenReturn(file());

        mvc.perform(get("/self/file").param("fileId", "1").param("thumb", "false"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Accel-Redirect", "/__self_internal__/avatar/2026/11/abc.jpg"));
    }

    @Test
    void file_validSign_thumbnailUsesThFilename() throws Exception {
        when(urlResolver.checkAccess(any(SysFile.class), anyLong(), org.mockito.ArgumentMatchers.anyBoolean(), anyString()))
                .thenReturn(true);
        when(fileService.get(1L)).thenReturn(file());

        mvc.perform(get("/self/file").param("fileId", "1").param("thumb", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Accel-Redirect", "/__self_internal__/avatar/2026/11/abc.min.jpg"));
    }

    @Test
    void file_missingRecord_rejected_noHeader() throws Exception {
        when(urlResolver.checkAccess(any(SysFile.class), anyLong(), org.mockito.ArgumentMatchers.anyBoolean(), anyString()))
                .thenReturn(true);
        when(fileService.get(1L)).thenReturn(null);

        mvc.perform(get("/self/file").param("fileId", "1"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("X-Accel-Redirect"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void access_validSign_streamsDownloadedBytes() throws Exception {
        when(urlResolver.checkAccess(any(SysFile.class), anyLong(), org.mockito.ArgumentMatchers.anyBoolean(), anyString()))
                .thenReturn(true);
        when(fileService.get(1L)).thenReturn(file());

        Downloader downloader = mock(Downloader.class);
        when(fileGateway.download(eq("cfg-1"), eq("local-plus"), any())).thenReturn(downloader);
        org.mockito.Mockito.doAnswer(inv -> {
            Consumer<java.io.InputStream> consumer = inv.getArgument(0);
            consumer.accept(new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)));
            return null;
        }).when(downloader).inputStream(any());

        String body = mvc.perform(get("/self/access").param("fileId", "1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        org.junit.jupiter.api.Assertions.assertEquals("hello", body);
    }
}
