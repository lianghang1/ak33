package com.sfi.matrix.iam.controller.support;

import com.sfi.scaffold.common.request.PageParam;
import com.sfi.scaffold.common.result.PageResult;
import com.sfi.scaffold.query.QueryUtilsX;
import com.sfi.scaffold.query.filter.QueryFilterX;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * IAM 列表查询的分页装配：与 BaseCrudController 共用同一套 1 基语义与 PageResult 契约。
 *
 * <p>从 request 直接提取参数 map，走 QueryUtilsX 通路②（JPA 实体），屏蔽 Spring Data Page，
 * 避免 0 基裸语义与框架类型沿调用链外泄。供不继承 BaseCrudController 的特化 Controller 复用。</p>
 *
 * @author lianghang
 */
public final class IamPageQuery {

    private IamPageQuery() {
    }

    /** 从 request 解析查询条件与 1 基分页参数，返回统一分页结果。 */
    public static <T> PageResult<T> query(HttpServletRequest request, Class<T> entityClass) {
        List<QueryFilterX> filters = QueryUtilsX.parse(request.getParameterMap());
        PageParam page = pageParamOf(request);
        Pageable pageable = page.isUnpaged()
                ? Pageable.unpaged()
                : PageRequest.of(page.getPageNum() - 1, page.getPageSize());
        Page<T> paged = QueryUtilsX.doQuery(filters, pageable, entityClass);
        long total = paged.getTotalElements();
        long size = pageable.isPaged() ? page.getPageSize() : (total == 0 ? 0 : total);
        return PageResult.of(paged.getContent(), total, page.getPageNum(), size);
    }

    private static PageParam pageParamOf(HttpServletRequest request) {
        PageParam p = new PageParam();
        int pageNum = toInt(first(request, "pageNum", "page"), 1);
        int pageSize = toInt(first(request, "pageSize", "size"), 10);
        p.setPageNum(pageNum < 1 ? 1 : pageNum);
        p.setPageSize(pageSize == PageParam.PAGE_SIZE_NONE || pageSize > 0 ? pageSize : 10);
        return p;
    }

    private static String first(HttpServletRequest request, String... keys) {
        for (String key : keys) {
            String v = request.getParameter(key);
            if (v != null && !v.isEmpty()) {
                return v;
            }
        }
        return null;
    }

    private static int toInt(String v, int def) {
        try {
            return v == null || v.isEmpty() ? def : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
