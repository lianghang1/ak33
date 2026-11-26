package com.sfi.matrix.support.service;

import com.sfi.conflux.file.engine.FileGateway;
import com.sfi.matrix.support.dto.FileConfigInfo;
import com.sfi.matrix.support.dto.FileDTO;
import com.sfi.matrix.support.dto.FileTypeInfo;
import com.sfi.matrix.support.dto.UploadCommand;
import com.sfi.matrix.support.enums.FileType;
import com.sfi.matrix.support.service.file.FileConfigInfoProvider;
import com.sfi.matrix.support.service.file.FileService;
import com.sfi.matrix.support.service.file.FileTypeGuard;
import com.sfi.matrix.support.service.file.FileUploadSupport;
import com.sfi.matrix.support.service.file.FileUrlResolver;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.constant.Constant;
import org.dromara.x.file.storage.core.hash.HashInfo;
import org.dromara.x.file.storage.core.upload.UploadPretreatment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FileService 上传：门禁结果复用、上传链路、逐字段落库、缩略图随传、DTO 转换。
 *
 * @author lianghang
 */
class FileUploadServiceTest {

    private FileGateway gateway;
    private SysFileService fileService;
    private FileTypeGuard fileTypeGuard;
    private FileStorageService storage;
    private UploadPretreatment pre;
    private FileUrlResolver urlResolver;
    private FileService service;
    private HashInfo hash;
    private FileConfigInfo configInfo;

    @BeforeEach
    void setUp() {
        gateway = mock(FileGateway.class);
        fileService = mock(SysFileService.class);
        SysFilePartService partService = mock(SysFilePartService.class);
        fileTypeGuard = mock(FileTypeGuard.class);
        storage = mock(FileStorageService.class);
        pre = mock(UploadPretreatment.class);
        urlResolver = mock(FileUrlResolver.class);
        hash = mock(HashInfo.class);
        FileConfigInfoProvider fileConfigProvider = mock(FileConfigInfoProvider.class);
        when(hash.getMd5()).thenReturn("d41d8cd98f00b204e9800998ecf8427e");

        configInfo = new FileConfigInfo();
        configInfo.setDefaultConfigId("cfg-1");
        configInfo.setDefaultChannelCode("oss");
        when(fileConfigProvider.getConfig()).thenReturn(configInfo);

        when(gateway.nativeService("cfg-1", "oss")).thenReturn(storage);
        when(storage.isSupportAcl(anyString())).thenReturn(true);
        when(storage.of(any(), anyString(), anyString())).thenReturn(pre);
        when(pre.setPlatform(anyString())).thenReturn(pre);
        when(pre.setAcl(anyBoolean(), any())).thenReturn(pre);
        when(pre.setPath(anyString())).thenReturn(pre);
        when(pre.setNotSupportMetadataThrowException(anyBoolean())).thenReturn(pre);
        when(pre.setNotSupportAclThrowException(anyBoolean())).thenReturn(pre);
        when(pre.setIgnoreThumbnailException(anyBoolean(), anyBoolean())).thenReturn(pre);
        when(fileService.save(any(com.sfi.matrix.support.entity.SysFile.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // toDto 会通过 urlResolver 解析地址
        when(urlResolver.url(any(com.sfi.matrix.support.entity.SysFile.class), anyBoolean()))
                .thenAnswer(inv -> {
                    com.sfi.matrix.support.entity.SysFile f = inv.getArgument(0);
                    boolean thumb = inv.getArgument(1);
                    return thumb ? f.getThRawUrl() : f.getRawUrl();
                });

        FileUploadSupport support = new FileUploadSupport(fileConfigProvider);
        service = new FileService(gateway, fileService, partService, fileTypeGuard, support, urlResolver);
    }

    private FileTypeInfo imageType() {
        return new FileTypeInfo("image/jpeg", "jpg", FileType.IMAGE);
    }

    private FileInfo baseInfo() {
        FileInfo info = new FileInfo();
        info.setPath("portal/2026/11/");
        info.setFilename("abc.jpg");
        info.setOriginalFilename("海报.jpg");
        info.setUrl("https://cdn/portal/2026/11/abc.jpg");
        info.setSize(1024L);
        info.setExt("jpg");
        info.setContentType("image/jpeg");
        info.setHashInfo(hash);
        return info;
    }

    private UploadCommand command(String originalFilename) {
        UploadCommand cmd = new UploadCommand();
        cmd.setConfigId("cfg-1");
        cmd.setChannelCode("oss");
        cmd.setOriginalFilename(originalFilename);
        cmd.setSource(new MockMultipartFile("file", originalFilename, "image/jpeg", new byte[16]));
        return cmd;
    }

    @Test
    void upload_persistsAllFields_withThumbnail() {
        FileInfo info = baseInfo();
        info.setThFilename("abc.min.jpg");
        info.setThUrl("https://cdn/portal/2026/11/abc.min.jpg");
        info.setThSize(256L);
        info.setThContentType("image/jpeg");

        when(fileTypeGuard.detectAndCheck(any(InputStream.class), anyString())).thenReturn(imageType());
        when(pre.upload()).thenReturn(info);

        UploadCommand cmd = command("海报.jpg");
        cmd.setModule("portal");
        cmd.setObjectType("CEMETERY");
        cmd.setObjectId("tomb-1");
        FileDTO dto = service.upload(cmd);

        assertEquals("abc.jpg", dto.getFilename());
        assertEquals("海报.jpg", dto.getOriginalFilename());
        assertEquals("https://cdn/portal/2026/11/abc.jpg", dto.getUrl());
        assertEquals("https://cdn/portal/2026/11/abc.min.jpg", dto.getThumbUrl());
        assertEquals("https://cdn/portal/2026/11/abc.jpg", dto.getRawUrl());
        assertEquals("https://cdn/portal/2026/11/abc.min.jpg", dto.getRawThumbUrl());
        org.mockito.Mockito.verify(urlResolver).fillLocalUrls(any(com.sfi.matrix.support.entity.SysFile.class));
    }

    @Test
    void upload_noThumbnail_leavesThFieldsNull() {
        configInfo.setAllowedModules(java.util.Collections.singletonList("default"));
        FileTypeInfo docType = new FileTypeInfo("application/pdf", "pdf", FileType.DOCUMENT);
        FileInfo info = baseInfo();
        info.setExt("pdf");
        info.setContentType("application/pdf");
        when(fileTypeGuard.detectAndCheck(any(InputStream.class), anyString())).thenReturn(docType);
        when(pre.upload()).thenReturn(info);

        FileDTO dto = service.upload(command("doc.pdf"));

        assertEquals("application/pdf", dto.getContentType());
        assertEquals(FileType.DOCUMENT.name(), dto.getFileType());
        assertNull(dto.getThumbUrl());
        assertNull(dto.getRawThumbUrl());
    }

    @Test
    void upload_normalizesMultipartSourceToInputStream() {
        when(fileTypeGuard.detectAndCheck(any(InputStream.class), anyString())).thenReturn(imageType());
        when(pre.upload()).thenReturn(baseInfo());

        UploadCommand cmd = command("海报.jpg");
        cmd.setThumbnailSource(new MockMultipartFile("thumbnail", "t.jpg", "image/jpeg", new byte[8]));
        service.upload(cmd);

        ArgumentCaptor<Object> mainSource = ArgumentCaptor.forClass(Object.class);
        verify(storage).of(mainSource.capture(), anyString(), anyString());
        assertTrue(mainSource.getValue() instanceof InputStream, "主文件须归一化为 InputStream");

        ArgumentCaptor<Object> thumbSource = ArgumentCaptor.forClass(Object.class);
        verify(pre).thumbnailOf(thumbSource.capture());
        assertTrue(thumbSource.getValue() instanceof InputStream, "缩略图须归一化为 InputStream");
    }

    @Test
    void toDto_mapsEntityFields() {
        com.sfi.matrix.support.entity.SysFile f = new com.sfi.matrix.support.entity.SysFile();
        f.setId(7L);
        f.setFilename("abc.jpg");
        f.setRawUrl("https://cdn/2026/11/abc.jpg");
        f.setFileType("IMAGE");
        f.setThRawUrl("https://cdn/2026/11/abc.min.jpg");
        f.setHashMd5("md5-1");

        FileDTO dto = service.toDto(f);
        assertEquals(7L, dto.getId());
        assertEquals("abc.jpg", dto.getFilename());
        assertEquals("https://cdn/2026/11/abc.jpg", dto.getUrl());
        assertEquals("https://cdn/2026/11/abc.min.jpg", dto.getThumbUrl());
        assertEquals("md5-1", dto.getMd5());
    }
}
