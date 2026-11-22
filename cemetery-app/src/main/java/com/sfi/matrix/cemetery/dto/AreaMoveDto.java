package com.sfi.matrix.cemetery.dto;

import javax.validation.constraints.NotNull;

/**
 * 墓区移动请求。newParentId 为 null 表示提升为根。
 *
 * @author matrix
 */
public class AreaMoveDto {

    @NotNull(message = "墓区ID不能为空")
    private Long id;

    private Long newParentId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getNewParentId() { return newParentId; }
    public void setNewParentId(Long newParentId) { this.newParentId = newParentId; }
}
