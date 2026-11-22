package com.sfi.matrix.iam.audit.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfi.scaffold.common.annotation.CrudResource;
import com.sfi.scaffold.common.annotation.CrudResourceMeta;
import com.sfi.matrix.iam.audit.entity.SysOperateLog;
import com.sfi.matrix.iam.audit.service.AuditService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 验证 OperateLogAspect 对 @CrudResource 的元数据驱动审计：
 * 动作归约、module/operation 取值、只审计写操作。
 *
 * @author matrix
 */
class CrudResourceAuditTest {

    @CrudResource(entity = Object.class, dto = Object.class, module = "测试模块",
            listPerm = "m:list", savePerm = "m:save")
    static class DemoResource {
        public void save() {}
        public void delete() {}
        public void list() {}
        public void get() {}
    }

    private AuditService auditService;
    private OperateLogAspect aspect;
    private DemoResource target;

    @BeforeEach
    void setUp() {
        auditService = mock(AuditService.class);
        aspect = new OperateLogAspect(auditService, new ObjectMapper());
        target = new DemoResource();
    }

    private ProceedingJoinPoint pjp(String method) throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        Signature sig = mock(Signature.class);
        when(sig.getName()).thenReturn(method);
        when(pjp.getSignature()).thenReturn(sig);
        when(pjp.getTarget()).thenReturn(target);
        when(pjp.getArgs()).thenReturn(new Object[0]);
        when(pjp.proceed()).thenReturn(null);
        return pjp;
    }

    @Test
    void actionMapping() {
        assertEquals(CrudResourceMeta.Action.LIST, CrudResourceMeta.actionOf("list"));
        assertEquals(CrudResourceMeta.Action.GET, CrudResourceMeta.actionOf("get"));
        assertEquals(CrudResourceMeta.Action.SAVE, CrudResourceMeta.actionOf("save"));
        assertEquals(CrudResourceMeta.Action.DELETE, CrudResourceMeta.actionOf("delete"));
        assertEquals(CrudResourceMeta.Action.NONE, CrudResourceMeta.actionOf("other"));
    }

    @Test
    void saveIsAuditedWithModuleAndOperation() throws Throwable {
        aspect.aroundCrudResource(pjp("save"));
        ArgumentCaptor<SysOperateLog> cap = ArgumentCaptor.forClass(SysOperateLog.class);
        verify(auditService).recordOperate(cap.capture());
        assertEquals("测试模块", cap.getValue().getModule());
        assertEquals("保存", cap.getValue().getOperation());
    }

    @Test
    void deleteIsAudited() throws Throwable {
        aspect.aroundCrudResource(pjp("delete"));
        ArgumentCaptor<SysOperateLog> cap = ArgumentCaptor.forClass(SysOperateLog.class);
        verify(auditService).recordOperate(cap.capture());
        assertEquals("删除", cap.getValue().getOperation());
    }

    @Test
    void listAndGetAreNotAudited() throws Throwable {
        aspect.aroundCrudResource(pjp("list"));
        aspect.aroundCrudResource(pjp("get"));
        verify(auditService, never()).recordOperate(any());
    }

    @Test
    void permissionFallbackRules() {
        CrudResource cr = DemoResource.class.getAnnotation(CrudResource.class);
        assertArrayEquals(new String[]{"m:list"}, CrudResourceMeta.permissionsOf(cr, CrudResourceMeta.Action.GET));
        assertArrayEquals(new String[]{"m:save"}, CrudResourceMeta.permissionsOf(cr, CrudResourceMeta.Action.DELETE));
    }
}
