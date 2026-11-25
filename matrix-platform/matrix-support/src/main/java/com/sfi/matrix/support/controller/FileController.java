package com.sfi.matrix.support.controller;

import com.sfi.matrix.iam.audit.annotation.OperateLog;
import com.sfi.matrix.support.dto.FileDTO;
import com.sfi.matrix.support.dto.FileUploadRequest;
import com.sfi.matrix.support.dto.UploadCommand;
import com.sfi.matrix.support.service.file.FileService;
import com.sfi.scaffold.common.result.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件接口：上传落库 + 地址解析 + 查询。
 *
 * <p>非标准 CRUD——上传走 multipart、访问走地址解析，故独立成类。
 * 存储平台、签名算法全在 conflux / x-file-storage 内部，本类只做入参编排与结果返回。</p>
 *
 * @author lianghang
 */
@RestController
@RequestMapping("/api/file")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    /** 上传并落库，返回带可访问地址的 DTO */
    @OperateLog(module = "文件管理", operation = "上传文件")
    @PostMapping("/upload")
    public R<FileDTO> upload(@RequestPart("file") MultipartFile file,
                             @ModelAttribute FileUploadRequest req) {
        UploadCommand cmd = new UploadCommand();
        cmd.setConfigId(req.getConfigId());
        cmd.setChannelCode(req.getChannelCode());
        cmd.setSource(file);
        cmd.setOriginalFilename(file.getOriginalFilename());
        cmd.setObjectType(req.getObjectType());
        cmd.setObjectId(req.getObjectId());
        return R.ok(fileService.upload(cmd));
    }

    /** 取访问地址：公开文件返回原始地址，私有文件返回限时签名地址 */
    @GetMapping("/url")
    public R<String> url(@RequestParam("fileId") Long fileId,
                         @RequestParam(value = "thumbnail", defaultValue = "false") boolean thumbnail) {
        return R.ok(fileService.url(fileId, thumbnail));
    }

    /** 按 id 查询文件 DTO（含可访问地址） */
    @GetMapping("/get")
    public R<FileDTO> get(@RequestParam("id") Long id) {
        return R.ok(fileService.get(id));
    }

    /** 按业务对象查询文件 DTO（只返回上传成功的文件） */
    @GetMapping("/list-by-object")
    public R<List<FileDTO>> listByObject(@RequestParam("objectType") String objectType,
                                         @RequestParam("objectId") String objectId) {
        return R.ok(fileService.listByObject(objectType, objectId));
    }
}
