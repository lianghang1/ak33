package com.sfi.matrix.support.service;

import com.sfi.matrix.support.dto.FileConfigInfo;
import com.sfi.matrix.support.dto.FileTypeInfo;
import com.sfi.matrix.support.enums.FileType;
import com.sfi.matrix.support.exception.FileErrorCode;
import com.sfi.matrix.support.service.file.FileConfigInfoProvider;
import com.sfi.matrix.support.service.file.FileTypeGuard;
import com.sfi.scaffold.common.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * FileTypeGuard：读取与门禁分离；detect 不依赖配置、一次算出 mime/ext/fileType，
 * check 依赖白名单校验，命中放行、不匹配/配置缺失拒绝（fail-closed）。
 *
 * @author lianghang
 */
class FileTypeGuardTest {

    /** 1x1 透明 PNG 的最小字节，Tika 能检出 image/png */
    private static final byte[] PNG = new byte[]{
            (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n',
            0x00, 0x00, 0x00, 0x0D, 'I', 'H', 'D', 'R',
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00
    };

    private FileConfigInfoProvider fileConfigProvider;
    private FileTypeGuard guard;

    @BeforeEach
    void setUp() {
        fileConfigProvider = mock(FileConfigInfoProvider.class);
        guard = new FileTypeGuard(fileConfigProvider);
    }

    private static FileConfigInfo.MimeRule rule(String mime, String... exts) {
        FileConfigInfo.MimeRule r = new FileConfigInfo.MimeRule();
        r.setMime(mime);
        r.setExts(Arrays.asList(exts));
        return r;
    }

    private void stubAllowed(FileConfigInfo.MimeRule... rules) {
        FileConfigInfo config = new FileConfigInfo();
        config.setAllowed(Arrays.asList(rules));
        when(fileConfigProvider.getConfig()).thenReturn(config);
    }

    @Test
    void detectAndCheck_allowsWhenMimeAndExtMatch() {
        stubAllowed(rule("image/png", "png"));
        assertDoesNotThrow(() -> guard.detectAndCheck(new ByteArrayInputStream(PNG), "photo.png"));
    }

    @Test
    void detect_returnsMimeExtAndFileType() {
        // 读取不依赖配置，一次算出 mime/ext/fileType
        FileTypeInfo info = guard.detect(new ByteArrayInputStream(PNG), "photo.PNG");
        assertEquals("image/png", info.getMime());
        assertEquals("png", info.getExtension());
        assertEquals(FileType.IMAGE, info.getFileType());
    }

    @Test
    void detect_worksWithoutConfig() {
        // 白名单未配置时 detect 仍能读出类型——读取与门禁分离的意义
        when(fileConfigProvider.getConfig()).thenReturn(null);
        FileTypeInfo info = guard.detect(new ByteArrayInputStream(PNG), "photo.png");
        assertEquals("image/png", info.getMime());
    }

    @Test
    void detectAndCheck_allowsExtCaseInsensitive() {
        stubAllowed(rule("image/png", "png"));
        assertDoesNotThrow(() -> guard.detectAndCheck(new ByteArrayInputStream(PNG), "photo.PNG"));
    }

    @Test
    void detectAndCheck_rejectsWhenExtNotInAllowedSet() {
        // png 字节冒充 .jpg：mime=image/png 命中，但 jpg 不在该 mime 的后缀集合里
        stubAllowed(rule("image/png", "png"), rule("image/jpeg", "jpg", "jpeg"));
        BizException ex = assertThrows(BizException.class,
                () -> guard.detectAndCheck(new ByteArrayInputStream(PNG), "fake.jpg"));
        assertEquals(FileErrorCode.FILE_TYPE_NOT_ALLOWED, ex.getErrorCode());
    }

    @Test
    void detectAndCheck_rejectsWhenMimeNotInWhitelist() {
        stubAllowed(rule("image/png", "png"));
        byte[] text = "hello".getBytes(StandardCharsets.UTF_8);
        BizException ex = assertThrows(BizException.class,
                () -> guard.detectAndCheck(new ByteArrayInputStream(text), "note.txt"));
        assertEquals(FileErrorCode.FILE_TYPE_NOT_ALLOWED, ex.getErrorCode());
    }

    @Test
    void check_rejectsWhenConfigMissing() {
        // 配置缺失 → fail-closed
        when(fileConfigProvider.getConfig()).thenReturn(null);
        FileTypeInfo info = guard.detect(new ByteArrayInputStream(PNG), "photo.png");
        BizException ex = assertThrows(BizException.class, () -> guard.check(info));
        assertEquals(FileErrorCode.FILE_TYPE_CONFIG_MISSING, ex.getErrorCode());
    }

    @Test
    void check_rejectsWhenAllowedEmpty() {
        FileConfigInfo config = new FileConfigInfo();
        config.setAllowed(Collections.emptyList());
        when(fileConfigProvider.getConfig()).thenReturn(config);
        FileTypeInfo info = guard.detect(new ByteArrayInputStream(PNG), "photo.png");
        BizException ex = assertThrows(BizException.class, () -> guard.check(info));
        assertEquals(FileErrorCode.FILE_TYPE_CONFIG_MISSING, ex.getErrorCode());
    }
}
