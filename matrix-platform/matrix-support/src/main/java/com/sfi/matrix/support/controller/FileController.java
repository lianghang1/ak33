package com.sfi.matrix.support.controller;

import com.sfi.matrix.iam.audit.annotation.OperateLog;
import com.sfi.matrix.support.dto.FileUploadRequest;
import com.sfi.matrix.support.dto.UploadCommand;
import com.sfi.matrix.support.entity.SysFile;
import com.sfi.matrix.support.service.file.FileAccessService;
import com.sfi.matrix.support.service.file.FileUploadService;
import com.sfi.scaffold.common.result.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件接口：上传落库 + 访问地址（公开直出 / 私有签名）。
 *
 * <p>非标准 CRUD——上传走 multipart、访问走地址解析，故独立成类。
 * 存储平台、签名算法全在 conflux / x-file-storage 内部，本类只做入参编排与结果返回。</p>
 *
 * @author lianghang
 */
@RestController
@RequestMapping("/api/file")
public class FileController {

    private final FileUploadService uploadService;
    private final FileAccessService accessService;

    public FileController(FileUploadService uploadService, FileAccessService accessService) {
        this.uploadService = uploadService;
        this.accessService = accessService;
    }

    /** 上传并落库，返回文件记录（含 id，后续按 id 取访问地址） */
    @OperateLog(module = "文件管理", operation = "上传文件")
    @PostMapping("/upload")
    public R<SysFile> upload(@RequestPart("file") MultipartFile file,
                             @ModelAttribute FileUploadRequest req) {
        UploadCommand cmd = new UploadCommand();
        cmd.setConfigId(req.getConfigId());
        cmd.setChannelCode(req.getChannelCode());
        cmd.setSource(file);
        cmd.setOriginalFilename(file.getOriginalFilename());
        cmd.setObjectType(req.getObjectType());
        cmd.setObjectId(req.getObjectId());
        return R.ok(uploadService.upload(cmd));
    }

    /** 取访问地址：公开文件返回原始地址，私有文件返回限时签名地址 */
    @GetMapping("/url")
    public R<String> url(@RequestParam("fileId") Long fileId,
                         @RequestParam(value = "thumbnail", defaultValue = "false") boolean thumbnail) {
        return R.ok(accessService.url(fileId, thumbnail));
    }
}
