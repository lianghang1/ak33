package com.sfi.matrix.support.controller;

import com.sfi.matrix.iam.audit.annotation.OperateLog;
import com.sfi.matrix.support.dto.FileDTO;
import com.sfi.scaffold.query.QueryUtilsX;
import com.sfi.scaffold.query.filter.OperatorX;
import com.sfi.scaffold.query.filter.QueryFilterX;
import com.sfi.matrix.support.dto.FileUploadForm;
import com.sfi.matrix.support.dto.MultipartInitiateRequest;
import com.sfi.matrix.support.dto.MultipartPartForm;
import com.sfi.matrix.support.dto.UploadCommand;
import com.sfi.matrix.support.entity.SysFile;
import com.sfi.matrix.support.entity.SysFilePart;
import com.sfi.matrix.support.service.file.FileService;
import com.sfi.scaffold.common.request.IdRequest;
import com.sfi.scaffold.common.result.PageResult;
import com.sfi.scaffold.common.result.R;
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
import java.util.List;

/**
 * 文件业务接口：上传、地址解析、查询与分片上传。
 *
 * <p>强业务特化资源，不接通用 CRUD 基类：上传/分片写操作走 POST，列表读走 query starter
 * （从 request 提取参数 map，X 前缀协议）；上传类端点因 multipart 特性豁免 JSON 请求体。</p>
 *
 * @author lianghang
 */
@RestController
@RequestMapping("/api/file")
public class FileController {

    private final FileService service;

    public FileController(FileService service) {
        this.service = service;
    }

    // ---- 核心：上传 / 地址 / 查询 ----

    /** 上传文件（multipart 豁免）：文件走 @RequestPart，其余字段进表单 DTO */
    @OperateLog(module = "文件管理", operation = "上传文件")
    @PostMapping("/upload")
    public R<FileDTO> upload(@RequestPart("file") MultipartFile file,
                             @Valid @ModelAttribute FileUploadForm form) {
        UploadCommand cmd = new UploadCommand();
        cmd.setSource(file);
        cmd.setOriginalFilename(file.getOriginalFilename());
        cmd.setConfigId(form.getConfigId());
        cmd.setChannelCode(form.getChannelCode());
        cmd.setObjectType(form.getObjectType());
        cmd.setObjectId(form.getObjectId());
        cmd.setModule(form.getModule());
        cmd.setThumbWidth(form.getThumbWidth());
        cmd.setThumbHeight(form.getThumbHeight());
        return R.ok(service.upload(cmd));
    }

    /** 取访问地址：公开直出原始地址，私有生成限时地址 */
    @GetMapping("/url")
    public R<String> url(@RequestParam("fileId") Long fileId,
                         @RequestParam(value = "thumbnail", defaultValue = "false") boolean thumbnail) {
        return R.ok(service.url(fileId, thumbnail));
    }

    /** 按 id 查询成功文件 DTO（含可访问地址） */
    @GetMapping("/get")
    public R<FileDTO> get(@RequestParam("id") Long id) {
        return R.ok(service.get(id));
    }

    /**
     * 列表查询：走 query starter 通路②（JPA 实体），从 request 提取参数 map。
     * 支持 EQ_objectType / EQ_objectId 等 X 前缀条件与 page/size 分页；命中记录转带 url 的 DTO。
     */
    /** 固定成功条件：非分片（multipartState 为空）或分片已完成，作为 OR 组与检索条件 AND 叠加 */
    private static final List<QueryFilterX> SUCCESS_OR_FILTERS = List.of(
            new QueryFilterX("multipartState", OperatorX.NU),
            new QueryFilterX("multipartState", OperatorX.EQ, "COMPLETED"));

    @GetMapping("/list")
    public R<PageResult<FileDTO>> list(HttpServletRequest request) {
        Page<SysFile> page = QueryUtilsX.doQuery(
                QueryUtilsX.parse(request.getParameterMap()),
                SUCCESS_OR_FILTERS,
                PageRequest.of(pageNum(request) - 1, pageSize(request)),
                SysFile.class);
        List<FileDTO> list = page.getContent().stream().map(service::toDto).toList();
        return R.ok(PageResult.of(list, page.getTotalElements(), pageNum(request), pageSize(request)));
    }

    // ---- 分片：能力 / 初始化 / 上传片 / 完成 / 取消 / 列举 ----

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

    /** 上传单片（multipart 豁免）：分片文件走 @RequestPart，索引进表单 DTO */
    @OperateLog(module = "文件管理", operation = "上传分片")
    @PostMapping("/upload-part")
    public R<SysFilePart> uploadPart(@RequestPart("file") MultipartFile file,
                                     @Valid @ModelAttribute MultipartPartForm form) {
        com.sfi.matrix.support.dto.MultipartPartCommand cmd =
                new com.sfi.matrix.support.dto.MultipartPartCommand();
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

    // ---- 内部工具 ----

    private static com.sfi.matrix.support.dto.MultipartInitCommand toCommand(MultipartInitiateRequest req) {
        com.sfi.matrix.support.dto.MultipartInitCommand cmd =
                new com.sfi.matrix.support.dto.MultipartInitCommand();
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
