package com.quanxiaoha.xiaohashu.user.relation.biz.constant;

public class RedisConstants {

    private static final String FOLLOWING_LIST_KEY_PREFIX = "following:";
    private static final String USER_FANS_KEY_PREFIX = "fans:";

    public static String buildFollowingListKey(Long userId) {
        return FOLLOWING_LIST_KEY_PREFIX + userId;
    }

    public static String buildUserFansKey(Long userId) {
        return USER_FANS_KEY_PREFIX + userId;
    }

}
