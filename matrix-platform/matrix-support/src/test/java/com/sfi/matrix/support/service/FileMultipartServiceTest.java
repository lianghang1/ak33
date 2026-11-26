package com.sfi.matrix.support.service;

import com.sfi.conflux.file.engine.FileGateway;
import com.sfi.matrix.support.dto.FileConfigInfo;
import com.sfi.matrix.support.dto.FileTypeInfo;
import com.sfi.matrix.support.dto.MultipartInitCommand;
import com.sfi.matrix.support.dto.MultipartInitResult;
import com.sfi.matrix.support.dto.MultipartPartCommand;
import com.sfi.matrix.support.entity.SysFile;
import com.sfi.matrix.support.entity.SysFilePart;
import com.sfi.matrix.support.enums.FileType;
import com.sfi.matrix.support.service.file.*;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.upload.AbortMultipartUploadPretreatment;
import org.dromara.x.file.storage.core.upload.CompleteMultipartUploadPretreatment;
import org.dromara.x.file.storage.core.upload.FilePartInfo;
import org.dromara.x.file.storage.core.upload.InitiateMultipartUploadPretreatment;
import org.dromara.x.file.storage.core.upload.MultipartUploadSupportInfo;
import org.dromara.x.file.storage.core.upload.UploadPartPretreatment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FileService 分片：初始化、分片上传（首片门禁/断点续传）、完成、取消、列举。
 *
 * @author lianghang
 */
class FileMultipartServiceTest {

    private FileGateway gateway;
    private SysFileService fileService;
    private SysFilePartService partService;
    private FileTypeGuard fileTypeGuard;
    private FileStorageService storage;
    private FileService service;
    private FileUrlResolver urlResolver;

    @BeforeEach
    void setUp() {
        gateway = mock(FileGateway.class);
        fileService = mock(SysFileService.class);
        partService = mock(SysFilePartService.class);
        fileTypeGuard = mock(FileTypeGuard.class);
        storage = mock(FileStorageService.class);
        FileConfigInfoProvider provider = mock(FileConfigInfoProvider.class);
        FileConfigInfo config = new FileConfigInfo();
        config.setDefaultConfigId("cfg-1");
        config.setDefaultChannelCode("oss");
        when(provider.getConfig()).thenReturn(config);

        when(gateway.nativeService("cfg-1", "oss")).thenReturn(storage);
        when(storage.isSupportAcl(anyString())).thenReturn(true);
        when(storage.isSupportMultipartUpload(anyString()))
                .thenReturn(MultipartUploadSupportInfo.supportAll());
        when(fileService.save(any(SysFile.class))).thenAnswer(inv -> inv.getArgument(0));

        FileUploadSupport support = new FileUploadSupport(provider);
        urlResolver = mock(FileUrlResolver.class);
        service = new FileService(gateway, fileService, partService, fileTypeGuard, support, urlResolver);
    }

    @Test
    void support_returnsPlatformCapability() {
        MultipartUploadSupportInfo info = MultipartUploadSupportInfo.supportAll();
        when(storage.isSupportMultipartUpload("oss")).thenReturn(info);

        assertEquals(info, service.multipartSupport("cfg-1", "oss"));
    }

    @Test
    void initiate_createsSessionRecord() {
        InitiateMultipartUploadPretreatment pre = mock(InitiateMultipartUploadPretreatment.class);
        when(storage.initiateMultipartUpload()).thenReturn(pre);
        when(pre.setPlatform(anyString())).thenReturn(pre);
        when(pre.setOriginalFilename(anyString())).thenReturn(pre);
        when(pre.setSize(any())).thenReturn(pre);
        when(pre.setContentType(anyString())).thenReturn(pre);
        when(pre.setPath(anyString())).thenReturn(pre);
        when(pre.setFileAcl(anyBoolean(), any())).thenReturn(pre);
        when(pre.setNotSupportMetadataThrowException(anyBoolean())).thenReturn(pre);
        when(pre.setNotSupportAclThrowException(anyBoolean())).thenReturn(pre);
        when(fileTypeGuard.detectByName("movie.mp4"))
                .thenReturn(new FileTypeInfo("video/mp4", "mp4", FileType.VIDEO));

        FileInfo info = new FileInfo();
        info.setUploadId("upload-1");
        info.setPath("default/2026/11/");
        info.setFilename("abc.mp4");
        info.setUrl("https://cdn/default/2026/11/abc.mp4");
        info.setSize(1000L);
        info.setExt("mp4");
        when(pre.init()).thenReturn(info);
        when(fileService.save(any(SysFile.class))).thenAnswer(inv -> {
            SysFile f = inv.getArgument(0);
            f.setId(1L);
            return f;
        });

        MultipartInitCommand cmd = new MultipartInitCommand();
        cmd.setOriginalFilename("movie.mp4");
        cmd.setSize(1000L);
        MultipartInitResult result = service.initiate(cmd);

        assertEquals(1L, result.getFileId());
        assertEquals("upload-1", result.getUploadId());
        verify(fileService).save(any(SysFile.class));
    }

    @Test
    void initiate_platformNotSupport_throwsAndDoesNotTouchDb() {
        when(storage.isSupportMultipartUpload("oss")).thenReturn(MultipartUploadSupportInfo.notSupport());
        MultipartInitCommand cmd = new MultipartInitCommand();
        cmd.setOriginalFilename("movie.mp4");
        cmd.setSize(1000L);

        assertThrows(com.sfi.scaffold.common.exception.BizException.class, () -> service.initiate(cmd));
        verify(storage, org.mockito.Mockito.never()).initiateMultipartUpload();
        verify(fileService, org.mockito.Mockito.never()).save(any(SysFile.class));
    }

    @Test
    void uploadPart_firstPartGuardFails_rejects() {
        SysFile record = activeRecord();
        when(fileService.get(1L)).thenReturn(record);
        when(fileTypeGuard.detectAndCheck(any(InputStream.class), anyString()))
                .thenThrow(new com.sfi.scaffold.common.exception.BizException(
                        com.sfi.matrix.support.exception.FileErrorCode.FILE_TYPE_NOT_ALLOWED));

        MultipartPartCommand cmd = new MultipartPartCommand();
        cmd.setFileId(1L);
        cmd.setPartNumber(1);
        cmd.setSource(new MockMultipartFile("part", "p1", "application/octet-stream", new byte[16]));

        assertThrows(com.sfi.scaffold.common.exception.BizException.class, () -> service.uploadPart(cmd));
        verify(storage, org.mockito.Mockito.never()).uploadPart(any(), anyInt(), any());
    }

    @Test
    void uploadPart_nonFirstPartPlatformFails_rejects() {
        SysFile record = activeRecord();
        record.setMultipartState("UPLOADING");
        when(fileService.get(1L)).thenReturn(record);
        when(partService.findPart("upload-1", 2)).thenReturn(Collections.emptyList());
        when(storage.uploadPart(any(FileInfo.class), eq(2), any()))
                .thenThrow(new com.sfi.scaffold.common.exception.BizException("平台上传失败"));

        MultipartPartCommand cmd = new MultipartPartCommand();
        cmd.setFileId(1L);
        cmd.setPartNumber(2);
        cmd.setSource(new byte[16]);

        assertThrows(com.sfi.scaffold.common.exception.BizException.class, () -> service.uploadPart(cmd));
        verify(partService, org.mockito.Mockito.never()).save(any(SysFilePart.class));
    }

    @Test
    void complete_platformFails_rejects() {
        SysFile record = activeRecord();
        when(fileService.get(1L)).thenReturn(record);
        SysFilePart part = new SysFilePart();
        part.setPartNumber(1);
        part.setETag("etag-1");
        part.setPartSize(512L);
        when(partService.listByUploadId("upload-1")).thenReturn(Collections.singletonList(part));
        when(storage.completeMultipartUpload(any(FileInfo.class)))
                .thenThrow(new com.sfi.scaffold.common.exception.BizException("合并失败"));

        assertThrows(com.sfi.scaffold.common.exception.BizException.class, () -> service.complete(1L));
        verify(partService, org.mockito.Mockito.never()).clearByUploadId("upload-1");
    }

    @Test
    void uploadPart_firstPartChecksGuardAndRecordsPart() {
        SysFile record = activeRecord();
        when(fileService.get(1L)).thenReturn(record);
        when(fileTypeGuard.detectAndCheck(any(InputStream.class), anyString()))
                .thenReturn(new FileTypeInfo("video/mp4", "mp4", FileType.VIDEO));
        when(partService.findPart("upload-1", 1)).thenReturn(Collections.emptyList());

        UploadPartPretreatment partPre = mock(UploadPartPretreatment.class);
        when(storage.uploadPart(any(FileInfo.class), eq(1), any())).thenReturn(partPre);
        FilePartInfo partInfo = new FilePartInfo();
        partInfo.setETag("etag-1");
        partInfo.setPartSize(512L);
        when(partPre.upload()).thenReturn(partInfo);
        when(partService.save(any(SysFilePart.class))).thenAnswer(inv -> inv.getArgument(0));

        MultipartPartCommand cmd = new MultipartPartCommand();
        cmd.setFileId(1L);
        cmd.setPartNumber(1);
        cmd.setSource(new MockMultipartFile("part", "p1", "application/octet-stream", new byte[16]));
        SysFilePart part = service.uploadPart(cmd);

        assertEquals("etag-1", part.getETag());
        assertEquals(512L, part.getPartSize());
        assertEquals("upload-1", part.getMultipartUploadId());
        verify(fileTypeGuard).detectAndCheck(any(InputStream.class), anyString());
    }

    @Test
    void uploadPart_duplicatePart_skipsUpload() {
        SysFile record = activeRecord();
        record.setMultipartState("UPLOADING");
        when(fileService.get(1L)).thenReturn(record);
        SysFilePart existing = new SysFilePart();
        existing.setPartNumber(2);
        existing.setETag("etag-2");
        when(partService.findPart("upload-1", 2)).thenReturn(Collections.singletonList(existing));

        MultipartPartCommand cmd = new MultipartPartCommand();
        cmd.setFileId(1L);
        cmd.setPartNumber(2);
        cmd.setSource(new byte[16]);
        SysFilePart result = service.uploadPart(cmd);

        assertEquals("etag-2", result.getETag());
        verify(storage, org.mockito.Mockito.never()).uploadPart(any(), anyInt(), any());
    }

    @Test
    void complete_mergesPartsAndCleansUp() {
        SysFile record = activeRecord();
        when(fileService.get(1L)).thenReturn(record);

        SysFilePart part = new SysFilePart();
        part.setPartNumber(1);
        part.setETag("etag-1");
        part.setPartSize(512L);
        when(partService.listByUploadId("upload-1")).thenReturn(Collections.singletonList(part));

        CompleteMultipartUploadPretreatment completePre = mock(CompleteMultipartUploadPretreatment.class);
        when(storage.completeMultipartUpload(any(FileInfo.class))).thenReturn(completePre);
        when(completePre.setPartInfoList(any())).thenReturn(completePre);
        FileInfo merged = new FileInfo();
        merged.setPath("default/2026/11/");
        merged.setFilename("abc.mp4");
        merged.setUrl("https://cdn/default/2026/11/abc.mp4");
        merged.setSize(1000L);
        merged.setExt("mp4");
        when(completePre.complete()).thenReturn(merged);

        SysFile done = service.complete(1L);

        assertEquals("COMPLETED", done.getMultipartState());
        assertEquals("abc.mp4", done.getFilename());
        verify(urlResolver).fillLocalUrls(any(SysFile.class));
        verify(partService).clearByUploadId("upload-1");
    }

    @Test
    void abort_marksAbortedAndCleansUp() {
        SysFile record = activeRecord();
        when(fileService.get(1L)).thenReturn(record);
        AbortMultipartUploadPretreatment abortPre = mock(AbortMultipartUploadPretreatment.class);
        when(storage.abortMultipartUpload(any(FileInfo.class))).thenReturn(abortPre);
        when(abortPre.abort()).thenReturn(new FileInfo());

        service.abort(1L);

        assertEquals("ABORTED", record.getMultipartState());
        verify(partService).clearByUploadId("upload-1");
    }

    @Test
    void listParts_returnsLocalRecords() {
        SysFile record = activeRecord();
        when(fileService.get(1L)).thenReturn(record);
        SysFilePart part = new SysFilePart();
        part.setPartNumber(1);
        when(partService.listByUploadId("upload-1")).thenReturn(Collections.singletonList(part));

        List<SysFilePart> parts = service.listParts(1L);

        assertEquals(1, parts.size());
        assertEquals(1, parts.get(0).getPartNumber());
    }

    @Test
    void uploadPart_rejectsInvalidPartNumber() {
        SysFile record = activeRecord();
        when(fileService.get(1L)).thenReturn(record);
        MultipartPartCommand cmd = new MultipartPartCommand();
        cmd.setFileId(1L);
        cmd.setPartNumber(0);
        cmd.setSource(new byte[16]);

        try {
            service.uploadPart(cmd);
            org.junit.jupiter.api.Assertions.fail("应抛出分片序号错误");
        } catch (com.sfi.scaffold.common.exception.BizException e) {
            assertNotNull(e.getMessage());
        }
    }

    private SysFile activeRecord() {
        SysFile record = new SysFile();
        record.setId(1L);
        record.setConfigId("cfg-1");
        record.setChannelCode("oss");
        record.setOriginalFilename("movie.mp4");
        record.setContentType("video/mp4");
        record.setFileType("VIDEO");
        record.setPath("default/2026/11/");
        record.setFilename("abc.mp4");
        record.setMultipartUploadId("upload-1");
        record.setMultipartState("INITIATED");
        return record;
    }
}
