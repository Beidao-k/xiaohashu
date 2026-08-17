package com.quanxiaoha.xiaohashu.user.relation.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
public enum FollowUnFollowTypeEnum {
    FOLLOW(1),
    UN_FOLLOW(0)
    ;
    private final Integer code;
}
