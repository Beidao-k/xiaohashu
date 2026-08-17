package com.quanxiaoha.xiaohashu.gateway.xiaohashugateway.enums;

import com.quanxiaoha.framework.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {

    SYSTEM_ERROR("500", "系统繁忙，请稍后再试"),
    UNAUTHORIZED("401", "权限不足"),

    ;
    // 异常码
    private final String errorCode;
    // 错误信息
    private final String errorMessage;
}
