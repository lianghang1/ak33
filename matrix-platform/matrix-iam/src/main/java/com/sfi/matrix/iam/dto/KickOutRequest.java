package com.sfi.matrix.iam.dto;

import javax.validation.constraints.NotNull;

/**
 * 踢人请求：使目标账号全部会话失效。
 *
 * @author lianghang
 */
public class KickOutRequest {

    @NotNull(message = "账号 ID 不能为空")
    private Long accountId;

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }
}
