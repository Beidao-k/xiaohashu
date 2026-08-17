package com.quanxiaoha.xiaohashu.user.biz.constant;

public class RedisKeyConstants {



    public static final String XIAOHASHU_ID_GENERATOR_KEY = "xiaohashu_id_generator";
    private static final String USER_ROLES_KEY_PREFIX = "user:roles:";
    private static final String ROLE_PERMISSIONS_KEY_PREFIX = "role:permissions:";
    private static final String USER_INFO_KEY_PREFIX = "user:info:";




    public static String buildUserRoleKey(Long userId) {
        return USER_ROLES_KEY_PREFIX  + userId;
    }

    public static String buildRolePermissionsKey(String rolekey) {
        return ROLE_PERMISSIONS_KEY_PREFIX + rolekey;
    }

    public static String buildUserInfoKey(Long userId) {
        return USER_INFO_KEY_PREFIX + userId;
    }

}
