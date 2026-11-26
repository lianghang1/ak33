package com.sfi.matrix.support.controller.file;

import com.sfi.conflux.file.engine.FileGateway;
import com.sfi.matrix.support.entity.SysFile;
import com.sfi.matrix.support.service.SysFileService;
import com.sfi.matrix.support.service.file.FileUrlResolver;
import com.sfi.scaffold.common.exception.BizException;
import com.sfi.scaffold.common.util.StreamPipeUtils;
import com.sfi.scaffold.starter.security.anonymous.Anonymous;
import org.dromara.x.file.storage.core.FileInfo;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.sfi.matrix.support.exception.FileErrorCode.FILE_ACCESS_SIGN_INVALID;

/**
 * 本地文件访问端点（匿名放行，靠限时签名守护私有文件）。
 *
 * <p>两个端点共用 {@link FileUrlResolver#checkAccess} 验签：公开文件直接放行，私有文件校验
 * fileId+t+thumb 签名。文件路径一律由 fileId 查库得出，不接受客户端传 path。nginx 部署配置见
 * {@link FileUrlResolver#fillLocalUrls}。</p>
 *
 * @author lianghang
 */
@RestController
public class LocalFileAccessController {

    private final FileUrlResolver urlResolver;
    private final SysFileService fileService;
    private final FileGateway fileGateway;

    public LocalFileAccessController(FileUrlResolver urlResolver, SysFileService fileService,
                                     FileGateway fileGateway) {
        this.urlResolver = urlResolver;
        this.fileService = fileService;
        this.fileGateway = fileGateway;
    }

    /**
     * nginx 模式：验签通过后仅下发 X-Accel-Redirect，由 nginx 内部 location 读盘返回。
     * 配置注意事项：internal location 的 alias 须与 local-plus 的 storagePath 完全一致（含尾斜杠），
     * location 前缀须与 fileConfig.nginxInternalPrefix 一致，详见 FileUrlResolver.fillLocalUrls。
     */
    @Anonymous
    @GetMapping("/self/file")
    public void file(@RequestParam("fileId") Long fileId,
                     @RequestParam(value = "t", defaultValue = "0") long t,
                     @RequestParam(value = "thumb", defaultValue = "false") boolean thumb,
                     @RequestParam(value = "s", defaultValue = "") String sign,
                     HttpServletResponse response) {
        SysFile file = authorize(fileId, t, thumb, sign);
        response.setHeader(HttpHeaders.CONTENT_TYPE, file.getContentType());
        response.setHeader("X-Accel-Redirect", urlResolver.getNginxInternalPrefix() + objectKey(file, thumb));
    }

    /** stream 模式：验签通过后由 Java 读 SDK 下载流写回响应，不设 X-Accel-Redirect */
    @Anonymous
    @GetMapping("/self/access")
    public void access(@RequestParam("fileId") Long fileId,
                       @RequestParam(value = "t", defaultValue = "0") long t,
                       @RequestParam(value = "thumb", defaultValue = "false") boolean thumb,
                       @RequestParam(value = "s", defaultValue = "") String sign,
                       HttpServletResponse response) throws IOException {
        SysFile file = authorize(fileId, t, thumb, sign);
        response.setContentType(file.getContentType());
        FileInfo info = file.toFileInfo(thumb);
        fileGateway.download(file.getConfigId(), file.getChannelCode(), info)
                .inputStream(in -> {
                    try {
                        StreamPipeUtils.copy(in, response.getOutputStream(), false);
                    } catch (IOException e) {
                        throw new java.io.UncheckedIOException(e);
                    }
                });
    }

    /** 按 fileId 查库并验签 */
    private SysFile authorize(Long fileId, long t, boolean thumb, String sign) {
        SysFile file = fileService.get(fileId);
        if (!urlResolver.checkAccess(file, t, thumb, sign)) {
            throw new BizException(FILE_ACCESS_SIGN_INVALID);
        }
        return file;
    }

    /** 对象键：path 与 filename 拼接，补斜杠 */
    private static String objectKey(SysFile file, boolean thumb) {
        String path = thumb ? file.getThPath() : file.getPath();
        String filename = thumb ? file.getThFilename() : file.getFilename();
        if (path == null || path.isEmpty()) {
            return filename;
        }
        return path.endsWith("/") ? path + filename : path + "/" + filename;
    }
}
