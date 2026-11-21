package com.sfi.matrix.support.dto;

import javax.validation.constraints.NotNull;

/** 通用主键请求。 */
public class IdRequest {

    @NotNull(message = "ID 不能为空")
    private Long id;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
}
