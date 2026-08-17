package com.quanxiaoha.xiaohashu.note.biz.enums;

import com.quanxiaoha.framework.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {

    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR("NOTE-10000", "出错啦，后台小哥正在努力修复中..."),
    PARAM_NOT_VALID("NOTE-10001", "参数错误"),
    // ----------- 业务异常状态码 -----------
    NOTE_TYPE_ERROR("NOTE-20000", "未知的笔记类型"),
    NOTE_PUBLISH_FAIL("NOTE-20001", "笔记发布失败"),
    FIND_NOTE_BY_ID("NOTE-20002", "笔记查询失败"),
    NOTE_PRIVATE("NOTE_2003","无权限查看该笔记"),
    NOTE_UPDATE_FAIL("NOTE-20004", "笔记更新失败"),
    TOPIC_NOT_FOUND("NOTE-20005","话题非法"),
    NOTE_ISDELETED("NOTEE-20005","该笔记不可见"),
    NO_PERMISSION("NOTE-10002","无权限修改"),
    // ----------- 业务异常状态码 -----------
    ;


    // 异常码
    private final String errorCode;
    // 错误信息
    private final String errorMessage;
}
