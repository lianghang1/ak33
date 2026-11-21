package com.sfi.ak33.apps.cemetery.controller;

import com.sfi.ak33.platform.iam.service.IamService;
import com.sfi.ak33.platform.settle.service.SettleService;
import com.sfi.ak33.platform.settle.entity.SettleBill;
import com.sfi.scaffold.common.result.R;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * 公墓业务组装示例：演示 app 同时复用 IAM + 结算两个平台能力。
 *
 * <p>这些能力来自独立的 jar，公墓系统只管引依赖、调接口，不关心实现。
 * 寄存系统、殡仪系统引同样的 jar 即可获得同样能力。</p>
 *
 * @author ak33
 */
@RestController
@RequestMapping("/api/cemetery/biz")
public class CemeteryBizController {

    private final IamService iamService;
    private final SettleService settleService;

    public CemeteryBizController(IamService iamService, SettleService settleService) {
        this.iamService = iamService;
        this.settleService = settleService;
    }

    /** 示例：购墓收费——用结算能力创建结算单，业务类型为 tomb */
    @PostMapping("/charge")
    public R<SettleBill> charge(@RequestParam Long tombId, @RequestParam BigDecimal amount) {
        // 实际应先校验墓位可售并锁定，此处演示能力复用
        return R.ok(settleService.createBill("tomb", tombId, amount));
    }

    /** 示例：查某用户的权限（用 IAM 能力） */
    @GetMapping("/my-permissions")
    public R<Set<String>> myPermissions(@RequestParam Long userId) {
        return R.ok(iamService.userPermissions(userId));
    }

    /** 示例：角色列表（来自 IAM 能力） */
    @GetMapping("/roles")
    public R<List<String>> roles() {
        return R.ok(iamService.listRoles().stream()
                .map(r -> r.getRoleCode()).collect(java.util.stream.Collectors.toList()));
    }
}
