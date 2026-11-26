package com.sfi.matrix.support.controller;

import com.sfi.matrix.support.controller.file.FileController;
import com.sfi.matrix.support.dto.FileDTO;
import com.sfi.matrix.support.dto.MultipartInitResult;
import com.sfi.matrix.support.service.file.FileService;
import com.sfi.scaffold.starter.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FileController：multipart 上传参数绑定、JSON 分片请求、GET 查询、id 类请求体。
 *
 * @author lianghang
 */
class FileControllerTest {

    private FileService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(FileService.class);
        mvc = MockMvcBuilders.standaloneSetup(new FileController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void upload_bindsFileAndForm() throws Exception {
        FileDTO dto = new FileDTO();
        dto.setId(1L);
        dto.setFilename("abc.jpg");
        when(service.upload(any())).thenReturn(dto);

        MockMultipartFile file = new MockMultipartFile("file", "海报.jpg", "image/jpeg", new byte[16]);
        MockMultipartFile thumb = new MockMultipartFile("thumbnail", "thumb.jpg", "image/jpeg", new byte[8]);
        mvc.perform(multipart("/api/file/upload").file(file).file(thumb)
                        .param("configId", "cfg-1")
                        .param("channelCode", "oss")
                        .param("module", "portal")
                        .param("objectType", "CEMETERY")
                        .param("objectId", "tomb-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1));

        org.mockito.ArgumentCaptor<com.sfi.matrix.support.dto.UploadCommand> cap =
                org.mockito.ArgumentCaptor.forClass(com.sfi.matrix.support.dto.UploadCommand.class);
        verify(service).upload(cap.capture());
        org.junit.jupiter.api.Assertions.assertEquals("海报.jpg", cap.getValue().getOriginalFilename());
        org.junit.jupiter.api.Assertions.assertEquals("cfg-1", cap.getValue().getConfigId());
        org.junit.jupiter.api.Assertions.assertEquals("portal", cap.getValue().getModule());
        org.junit.jupiter.api.Assertions.assertNotNull(cap.getValue().getThumbnailSource());
    }

    @Test
    void upload_invalidThumbWidth_rejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[16]);
        mvc.perform(multipart("/api/file/upload").file(file).param("thumbWidth", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));
    }

    @Test
    void get_returnsDto() throws Exception {
        FileDTO dto = new FileDTO();
        dto.setId(7L);
        when(service.get(7L)).thenReturn(dto);
        mvc.perform(get("/api/file/get").param("id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(7));
    }

    @Test
    void objects_returnsList() throws Exception {
        FileDTO dto = new FileDTO();
        dto.setId(8L);
        when(service.listByObject("CEMETERY", "tomb-1")).thenReturn(java.util.Collections.singletonList(dto));
        mvc.perform(get("/api/file/objects").param("objectType", "CEMETERY").param("objectId", "tomb-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(8));
        verify(service).listByObject("CEMETERY", "tomb-1");
    }

    @Test
    void initiate_bindsJsonBody() throws Exception {
        MultipartInitResult result = new MultipartInitResult();
        result.setFileId(1L);
        result.setUploadId("upload-1");
        when(service.initiate(any())).thenReturn(result);

        String body = "{\"originalFilename\":\"movie.mp4\",\"size\":1000,\"objectType\":\"CEMETERY\",\"objectId\":\"t-1\"}";
        mvc.perform(post("/api/file/initiate").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uploadId").value("upload-1"));

        org.mockito.ArgumentCaptor<com.sfi.matrix.support.dto.MultipartInitCommand> cap =
                org.mockito.ArgumentCaptor.forClass(com.sfi.matrix.support.dto.MultipartInitCommand.class);
        verify(service).initiate(cap.capture());
        org.junit.jupiter.api.Assertions.assertEquals("movie.mp4", cap.getValue().getOriginalFilename());
        org.junit.jupiter.api.Assertions.assertEquals(1000L, cap.getValue().getSize());
    }

    @Test
    void initiate_missingFilename_rejected() throws Exception {
        String body = "{\"size\":1000}";
        mvc.perform(post("/api/file/initiate").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));
    }

    @Test
    void uploadPart_bindsFileAndForm() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "part1", "application/octet-stream", new byte[16]);
        mvc.perform(multipart("/api/file/upload-part").file(file)
                        .param("fileId", "1")
                        .param("partNumber", "2"))
                .andExpect(status().isOk());

        org.mockito.ArgumentCaptor<com.sfi.matrix.support.dto.MultipartPartCommand> cap =
                org.mockito.ArgumentCaptor.forClass(com.sfi.matrix.support.dto.MultipartPartCommand.class);
        verify(service).uploadPart(cap.capture());
        org.junit.jupiter.api.Assertions.assertEquals(1L, cap.getValue().getFileId());
        org.junit.jupiter.api.Assertions.assertEquals(2, cap.getValue().getPartNumber());
    }

    @Test
    void completeAndAbort_bindIdRequest() throws Exception {
        mvc.perform(post("/api/file/complete").contentType("application/json").content("{\"id\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mvc.perform(post("/api/file/abort").contentType("application/json").content("{\"id\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        verify(service).complete(5L);
        verify(service).abort(5L);
    }

    @Test
    void parts_returnsList() throws Exception {
        when(service.listParts(1L)).thenReturn(java.util.Collections.emptyList());
        mvc.perform(get("/api/file/parts").param("fileId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        verify(service).listParts(1L);
    }
}
