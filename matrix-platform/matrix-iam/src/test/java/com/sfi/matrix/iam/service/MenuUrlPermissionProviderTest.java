package com.sfi.matrix.iam.service;

import com.sfi.matrix.iam.entity.SysMenu;
import com.sfi.matrix.iam.repository.SysMenuRepository;
import com.sfi.scaffold.starter.security.permission.UrlPermissionRule;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link MenuUrlPermissionProvider}：从菜单表 api_pattern 解析 URL 权限规则，
 * 覆盖逗号分隔多模式、METHOD: 前缀、无 api_pattern 跳过。
 *
 * @author lianghang
 */
class MenuUrlPermissionProviderTest {

    private SysMenu menu(String permission, String apiPattern) {
        SysMenu m = new SysMenu();
        m.setPermission(permission);
        m.setApiPattern(apiPattern);
        return m;
    }

    @Test
    void parsesCommaSeparatedPatternsAndMethodPrefix() {
        SysMenuRepository repo = mock(SysMenuRepository.class);
        when(repo.findByDeletedFalseOrderBySortNoAsc()).thenReturn(Arrays.asList(
                menu("platform:dict:list", "GET:/api/dict/**,GET:/api/dict-type/**"),
                menu("platform:dict:save", "POST:/api/dict/**,POST:/api/dict-type/**"),
                menu("platform:tenant:list", "/api/iam/tenant/**,/api/iam/tenants")));

        List<UrlPermissionRule> rules = new MenuUrlPermissionProvider(repo).loadRules();

        assertThat(rules).hasSize(6);
        assertThat(rules).anySatisfy(r -> {
            assertThat(r.getMethod()).isEqualTo("GET");
            assertThat(r.getPattern()).isEqualTo("/api/dict/**");
            assertThat(r.getPermissions()).containsExactly("platform:dict:list");
        });
        assertThat(rules).anySatisfy(r -> {
            assertThat(r.getMethod()).isEqualTo("POST");
            assertThat(r.getPattern()).isEqualTo("/api/dict-type/**");
            assertThat(r.getPermissions()).containsExactly("platform:dict:save");
        });
        assertThat(rules).anySatisfy(r -> {
            assertThat(r.getMethod()).isEqualTo("*");
            assertThat(r.getPattern()).isEqualTo("/api/iam/tenants");
            assertThat(r.getPermissions()).containsExactly("platform:tenant:list");
        });
    }

    @Test
    void skipsMenusWithoutPatternOrPermission() {
        SysMenuRepository repo = mock(SysMenuRepository.class);
        when(repo.findByDeletedFalseOrderBySortNoAsc()).thenReturn(Arrays.asList(
                menu("platform:x:list", null),
                menu("platform:y:list", "   "),
                menu(null, "/api/z/**")));

        assertThat(new MenuUrlPermissionProvider(repo).loadRules()).isEmpty();
    }

    @Test
    void emptyTableYieldsNoRules() {
        SysMenuRepository repo = mock(SysMenuRepository.class);
        when(repo.findByDeletedFalseOrderBySortNoAsc()).thenReturn(Collections.emptyList());
        assertThat(new MenuUrlPermissionProvider(repo).loadRules()).isEmpty();
    }
}
