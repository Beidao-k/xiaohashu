package com.quanxiaoha.xiaohashu.auth.constant;

public class RedisKeyConstants {


    private static final String REDIS_KEY_PREFIX = "verification_code:";
    public static final String XIAOHASHU_ID_GENERATOR_KEY = "xiaohashu_id_generator";
    private static final String USER_ROLES_KEY_PREFIX = "user:roles:";
    private static final String ROLE_PERMISSIONS_KEY_PREFIX = "role:permissions:";
    /**
     * 构建验证码 KEY
     * @param phone
     * @return
     */
    public static  String buileVerificationCode(String phone){
        return REDIS_KEY_PREFIX + phone;
    }
    public static String buildUserRoleKey(Long userId) {
        return USER_ROLES_KEY_PREFIX  + userId;
    }
    public static String buildRolePermissionsKey(String rolekey) {
        return ROLE_PERMISSIONS_KEY_PREFIX + rolekey;
    }
}
