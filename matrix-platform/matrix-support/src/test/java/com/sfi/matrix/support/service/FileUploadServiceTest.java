package com.sfi.matrix.support.service;

import com.sfi.conflux.file.engine.FileGateway;
import com.sfi.matrix.support.dto.FileTypeInfo;
import com.sfi.matrix.support.dto.UploadCommand;
import com.sfi.matrix.support.entity.SysFile;
import com.sfi.matrix.support.enums.FileType;
import com.sfi.matrix.support.service.file.FileConfigProvider;
import com.sfi.matrix.support.service.file.FileTypeGuard;
import com.sfi.matrix.support.service.file.FileUploadService;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.upload.UploadPretreatment;
import org.dromara.x.file.storage.core.hash.HashInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * FileUploadService：门禁结果复用、上传链路、逐字段落库、缩略图随传、DTO 转换。
 *
 * @author lianghang
 */
class FileUploadServiceTest {

    private FileGateway gateway;
    private SysFileService fileService;
    private FileTypeGuard fileTypeGuard;
    private FileStorageService storage;
    private UploadPretreatment pre;
    private FileUploadService service;
    private HashInfo hash;
    private FileConfigProvider fileConfigProvider;

    @BeforeEach
    void setUp() {
        gateway = mock(FileGateway.class);
        fileService = mock(SysFileService.class);
        fileTypeGuard = mock(FileTypeGuard.class);
        storage = mock(FileStorageService.class);
        pre = mock(UploadPretreatment.class);
        hash = mock(HashInfo.class);
        fileConfigProvider = mock(FileConfigProvider.class);
        when(hash.getMd5()).thenReturn("d41d8cd98f00b204e9800998ecf8427e");

        when(gateway.nativeService("cfg-1", "oss")).thenReturn(storage);
        when(storage.of(any(), anyString(), anyString())).thenReturn(pre);
        when(pre.setPlatform(anyString())).thenReturn(pre);
        when(pre.setNotSupportMetadataThrowException(anyBoolean())).thenReturn(pre);
        when(pre.setNotSupportAclThrowException(anyBoolean())).thenReturn(pre);
        when(pre.setIgnoreThumbnailException(anyBoolean(), anyBoolean())).thenReturn(pre);
        when(fileService.save(any(SysFile.class))).thenAnswer(inv -> inv.getArgument(0));

        service = new FileUploadService(gateway, fileService, fileTypeGuard, fileConfigProvider);
    }

    private FileTypeInfo imageType() {
        return new FileTypeInfo("image/jpeg", "jpg", FileType.IMAGE);
    }

    private FileInfo baseInfo() {
        FileInfo info = new FileInfo();
        info.setPath("2026/11/");
        info.setFilename("abc.jpg");
        info.setOriginalFilename("海报.jpg");
        info.setUrl("https://cdn/2026/11/abc.jpg");
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
        info.setThUrl("https://cdn/2026/11/abc.min.jpg");
        info.setThSize(256L);
        info.setThContentType("image/jpeg");
        when(fileTypeGuard.detectAndCheck(any(InputStream.class), anyString())).thenReturn(imageType());
        when(pre.upload()).thenReturn(info);

        UploadCommand cmd = command("海报.jpg");
        cmd.setPrivateAccess(true);
        cmd.setObjectType("CEMETERY");
        cmd.setObjectId("tomb-1");
        SysFile record = service.upload(cmd);

        assertEquals("cfg-1", record.getConfigId());
        assertEquals("oss", record.getChannelCode());
        assertEquals("2026/11/", record.getPath());
        assertEquals("abc.jpg", record.getFilename());
        assertEquals("海报.jpg", record.getOriginalFilename());
        assertEquals("https://cdn/2026/11/abc.jpg", record.getRawUrl());
        assertEquals("image/jpeg", record.getContentType());
        assertEquals(FileType.IMAGE.name(), record.getFileType());
        assertEquals(Boolean.TRUE, record.getPrivateAccess());
        assertEquals("CEMETERY", record.getObjectType());
        assertEquals("tomb-1", record.getObjectId());
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", record.getHashMd5());
        assertEquals("2026/11/", record.getThPath());
        assertEquals("abc.min.jpg", record.getThFilename());
        assertEquals("https://cdn/2026/11/abc.min.jpg", record.getThRawUrl());
    }

    @Test
    void upload_noThumbnail_leavesThFieldsNull() {
        FileTypeInfo docType = new FileTypeInfo("application/pdf", "pdf", FileType.DOCUMENT);
        FileInfo info = baseInfo();
        info.setExt("pdf");
        info.setContentType("application/pdf");
        when(fileTypeGuard.detectAndCheck(any(InputStream.class), anyString())).thenReturn(docType);
        when(pre.upload()).thenReturn(info);

        UploadCommand cmd = command("doc.pdf");
        SysFile record = service.upload(cmd);

        assertEquals("application/pdf", record.getContentType());
        assertEquals(FileType.DOCUMENT.name(), record.getFileType());
        assertEquals(Boolean.FALSE, record.getPrivateAccess());
        assertNull(record.getThFilename());
        assertNull(record.getThRawUrl());
    }

    @Test
    void toDto_mapsEntityFields() {
        SysFile f = new SysFile();
        f.setId(7L);
        f.setFilename("abc.jpg");
        f.setRawUrl("https://cdn/2026/11/abc.jpg");
        f.setFileType("IMAGE");
        f.setThRawUrl("https://cdn/2026/11/abc.min.jpg");
        f.setHashMd5("md5-1");

        assertEquals(7L, service.toDto(f).getId());
        assertEquals("abc.jpg", service.toDto(f).getFilename());
        assertEquals("https://cdn/2026/11/abc.jpg", service.toDto(f).getUrl());
        assertEquals("https://cdn/2026/11/abc.min.jpg", service.toDto(f).getThumbUrl());
        assertEquals("md5-1", service.toDto(f).getMd5());
    }
}
