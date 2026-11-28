package com.sfi.matrix.iam.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/** 重置用户密码请求（管理员动作）。 */
public class ResetPasswordRequest {

    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    @NotBlank(message = "新密码不能为空")
    private String password;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
