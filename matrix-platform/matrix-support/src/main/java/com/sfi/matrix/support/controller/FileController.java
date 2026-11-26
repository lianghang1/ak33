package com.sfi.matrix.support.controller;

import com.sfi.matrix.iam.audit.annotation.OperateLog;
import com.sfi.matrix.support.dto.FileDTO;
import com.sfi.matrix.support.dto.FileUploadForm;
import com.sfi.matrix.support.dto.MultipartInitiateRequest;
import com.sfi.matrix.support.dto.MultipartInitCommand;
import com.sfi.matrix.support.dto.MultipartPartCommand;
import com.sfi.matrix.support.dto.MultipartPartForm;
import com.sfi.matrix.support.dto.UploadCommand;
import com.sfi.matrix.support.entity.SysFile;
import com.sfi.matrix.support.entity.SysFilePart;
import com.sfi.matrix.support.service.file.FileService;
import com.sfi.scaffold.common.request.IdRequest;
import com.sfi.scaffold.common.result.PageResult;
import com.sfi.scaffold.common.result.R;
import com.sfi.scaffold.query.QueryUtilsX;
import com.sfi.scaffold.query.filter.OperatorX;
import com.sfi.scaffold.query.filter.QueryFilterX;
import org.dromara.x.file.storage.core.upload.MultipartUploadSupportInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件业务接口：上传、查询与分片上传。
 *
 * @author lianghang
 */
@RestController
@RequestMapping("/api/file")
public class FileController {

    private static final List<QueryFilterX> SUCCESS_OR_FILTERS = Arrays.asList(
            new QueryFilterX("multipartState", OperatorX.NU),
            new QueryFilterX("multipartState", OperatorX.EQ, "COMPLETED"));

    private final FileService service;

    public FileController(FileService service) {
        this.service = service;
    }

    /** 上传：文件与缩略图走 multipart，其余字段进表单 DTO */
    @OperateLog(module = "文件管理", operation = "上传文件")
    @PostMapping("/upload")
    public R<FileDTO> upload(@RequestPart("file") MultipartFile file,
                             @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail,
                             @Valid @ModelAttribute FileUploadForm form) {
        UploadCommand cmd = new UploadCommand();
        cmd.setSource(file);
        cmd.setOriginalFilename(file.getOriginalFilename());
        cmd.setConfigId(form.getConfigId());
        cmd.setChannelCode(form.getChannelCode());
        cmd.setObjectType(form.getObjectType());
        cmd.setObjectId(form.getObjectId());
        cmd.setModule(form.getModule());
        cmd.setThumbnailSource(thumbnail);
        cmd.setThumbWidth(form.getThumbWidth());
        cmd.setThumbHeight(form.getThumbHeight());
        return R.ok(service.upload(cmd));
    }

    /** 按 id 查询成功文件 DTO（含可访问地址） */
    @GetMapping("/get")
    public R<FileDTO> get(@RequestParam("id") Long id) {
        return R.ok(service.get(id));
    }

    /** 按业务对象查询成功文件 DTO 列表 */
    @GetMapping("/objects")
    public R<List<FileDTO>> objects(@RequestParam("objectType") String objectType,
                                    @RequestParam("objectId") String objectId) {
        return R.ok(service.listByObject(objectType, objectId));
    }

    /** 列表查询 */
    @GetMapping("/list")
    public R<PageResult<FileDTO>> list(HttpServletRequest request) {
        Page<SysFile> page = QueryUtilsX.doQuery(
                QueryUtilsX.parse(request.getParameterMap()),
                SUCCESS_OR_FILTERS,
                PageRequest.of(pageNum(request) - 1, pageSize(request)),
                SysFile.class);
        List<FileDTO> list = page.getContent().stream().map(service::toDto).collect(Collectors.toList());
        return R.ok(PageResult.of(list, page.getTotalElements(), pageNum(request), pageSize(request)));
    }

    /** 查询平台是否支持手动分片上传 */
    @GetMapping("/multipart-support")
    public R<MultipartUploadSupportInfo> multipartSupport(
            @RequestParam(value = "configId", required = false) String configId,
            @RequestParam(value = "channelCode", required = false) String channelCode) {
        return R.ok(service.multipartSupport(configId, channelCode));
    }

    /** 初始化分片会话 */
    @OperateLog(module = "文件管理", operation = "初始化分片上传")
    @PostMapping("/initiate")
    public R<?> initiate(@Valid @RequestBody MultipartInitiateRequest req) {
        return R.ok(service.initiate(toCommand(req)));
    }

    /** 上传单片：分片文件走 multipart，索引进表单 DTO */
    @OperateLog(module = "文件管理", operation = "上传分片")
    @PostMapping("/upload-part")
    public R<SysFilePart> uploadPart(@RequestPart("file") MultipartFile file,
                                     @Valid @ModelAttribute MultipartPartForm form) {
        MultipartPartCommand cmd = new MultipartPartCommand();
        cmd.setFileId(form.getFileId());
        cmd.setPartNumber(form.getPartNumber());
        cmd.setSource(file);
        return R.ok(service.uploadPart(cmd));
    }

    /** 完成分片：合并并回填记录 */
    @OperateLog(module = "文件管理", operation = "完成分片上传")
    @PostMapping("/complete")
    public R<Void> complete(@Valid @RequestBody IdRequest req) {
        service.complete(req.getId());
        return R.ok();
    }

    /** 取消分片会话 */
    @OperateLog(module = "文件管理", operation = "取消分片上传")
    @PostMapping("/abort")
    public R<Void> abort(@Valid @RequestBody IdRequest req) {
        service.abort(req.getId());
        return R.ok();
    }

    /** 列举已上传分片 */
    @GetMapping("/parts")
    public R<List<SysFilePart>> parts(@RequestParam("fileId") Long fileId) {
        return R.ok(service.listParts(fileId));
    }

    private static MultipartInitCommand toCommand(MultipartInitiateRequest req) {
        MultipartInitCommand cmd = new MultipartInitCommand();
        cmd.setConfigId(req.getConfigId());
        cmd.setChannelCode(req.getChannelCode());
        cmd.setOriginalFilename(req.getOriginalFilename());
        cmd.setSize(req.getSize());
        cmd.setObjectType(req.getObjectType());
        cmd.setObjectId(req.getObjectId());
        cmd.setModule(req.getModule());
        cmd.setMetadata(req.getMetadata());
        return cmd;
    }

    private static int pageNum(HttpServletRequest request) {
        try {
            int v = Integer.parseInt(request.getParameter("pageNum"));
            return v < 1 ? 1 : v;
        } catch (Exception e) {
            return 1;
        }
    }

    private static int pageSize(HttpServletRequest request) {
        try {
            int v = Integer.parseInt(request.getParameter("pageSize"));
            return v > 0 ? Math.min(v, 200) : 10;
        } catch (Exception e) {
            return 10;
        }
    }
}
