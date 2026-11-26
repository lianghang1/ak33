package com.sfi.matrix.cachetest;

import com.sfi.matrix.iam.service.IamService;
import com.sfi.matrix.support.constant.SupportConstants;
import com.sfi.matrix.support.dto.DictOptionValue;
import com.sfi.matrix.support.entity.SysDict;
import com.sfi.matrix.support.entity.SysDictOption;
import com.sfi.matrix.support.repository.dict.SysDictOptionRepository;
import com.sfi.matrix.support.repository.dict.SysDictRepository;
import com.sfi.matrix.support.service.dict.DictOptionQueryService;
import com.sfi.matrix.support.service.dict.DictOptionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 字典缓存测试：批量取值命中缓存、写入后缓存失效。
 *
 * <p>用真实 Spring 缓存代理（@EnableCaching）包裹服务，mock 底层 repository 精确统计回源次数。</p>
 *
 * @author lianghang
 */
class DictCacheTest {

    private static final String TENANT = "t1";
    private static final String PLATFORM = "PLATFORM";

    private SysDictRepository dictRepository;
    private SysDictOptionRepository optionRepository;
    private AnnotationConfigApplicationContext ctx;
    private DictOptionQueryService queryService;
    private DictOptionService optionService;
    private CacheManager cacheManager;

    @TestConfiguration
    @EnableCaching
    static class CacheConfig {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                    SupportConstants.CACHE_DICT, SupportConstants.CACHE_DICT_OPTION);
        }
    }

    @BeforeEach
    void setUp() {
        dictRepository = mock(SysDictRepository.class);
        optionRepository = mock(SysDictOptionRepository.class);
        IamService iamService = mock(IamService.class);
        when(iamService.getPlatformTenantId()).thenReturn(PLATFORM);

        ctx = new AnnotationConfigApplicationContext();
        ctx.registerBean(CacheConfig.class);
        ctx.registerBean(SysDictRepository.class, () -> dictRepository);
        ctx.registerBean(SysDictOptionRepository.class, () -> optionRepository);
        ctx.registerBean(IamService.class, () -> iamService);
        ctx.registerBean(DictOptionQueryService.class,
                () -> new DictOptionQueryService(optionRepository, dictRepository, iamService));
        ctx.registerBean(DictOptionService.class,
                () -> new DictOptionService(optionRepository, ctx.getBean(DictOptionQueryService.class)));
        ctx.refresh();

        queryService = ctx.getBean(DictOptionQueryService.class);
        optionService = ctx.getBean(DictOptionService.class);
        cacheManager = ctx.getBean(CacheManager.class);
    }

    @AfterEach
    void tearDown() {
        if (ctx != null) ctx.close();
    }

    private void stubDict(String code, String tenantId, Long dictId) {
        SysDict dict = new SysDict();
        dict.setId(dictId);
        dict.setTenantId(tenantId);
        dict.setDictCode(code);
        when(dictRepository.findByDeletedFalseOrderByDictCodeAsc())
                .thenReturn(Collections.singletonList(dict));
    }

    private void stubOption(Long dictId, String code, String label) {
        SysDictOption option = new SysDictOption();
        option.setId(10L);
        option.setDictId(dictId);
        option.setOptionCode(code);
        option.setOptionLabel(label);
        when(optionRepository.findByDictIdAndDeletedFalseOrderBySortNoAsc(dictId))
                .thenReturn(Collections.singletonList(option));
    }

    @Test
    void valuesBatch_secondCallHitsCache() {
        stubDict("sys_normal_disable", PLATFORM, 1L);
        stubOption(1L, "1", "正常");

        Map<String, List<DictOptionValue>> first =
                optionService.valuesBatch(Collections.singletonList("sys_normal_disable"), TENANT);
        assertEquals("正常", first.get("sys_normal_disable").get(0).getLabel());

        Map<String, List<DictOptionValue>> second =
                optionService.valuesBatch(Collections.singletonList("sys_normal_disable"), TENANT);
        assertEquals("正常", second.get("sys_normal_disable").get(0).getLabel());

        // 两次取值只回源查询一次字典列表（第二次命中缓存）
        verify(dictRepository, times(1)).findByDeletedFalseOrderByDictCodeAsc();
        verify(optionRepository, times(1)).findByDictIdAndDeletedFalseOrderBySortNoAsc(1L);
    }

    @Test
    void save_evictsOptionCache() {
        stubDict("sys_normal_disable", PLATFORM, 1L);
        stubOption(1L, "1", "正常");

        queryService.values("sys_normal_disable", TENANT);
        verify(dictRepository, times(1)).findByDeletedFalseOrderByDictCodeAsc();

        // 写入选项后缓存被清，再次取值回源
        when(optionRepository.save(any(SysDictOption.class))).thenAnswer(inv -> inv.getArgument(0));
        SysDictOption incoming = new SysDictOption();
        incoming.setOptionCode("2");
        incoming.setOptionLabel("停用");
        optionService.save(incoming);

        queryService.values("sys_normal_disable", TENANT);
        verify(dictRepository, times(2)).findByDeletedFalseOrderByDictCodeAsc();
    }
}
