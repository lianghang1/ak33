package com.sfi.matrix.iam.dto;

import javax.validation.constraints.NotEmpty;

import java.util.List;

/** 用户批量操作请求（删除/批量改状态）。 */
public class UserBatchRequest {

    @NotEmpty(message = "用户 ID 列表不能为空")
    private List<Long> ids;

    /** 批量改状态时的目标状态：1启用 0停用 */
    private Integer status;

    public List<Long> getIds() { return ids; }
    public void setIds(List<Long> ids) { this.ids = ids; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
