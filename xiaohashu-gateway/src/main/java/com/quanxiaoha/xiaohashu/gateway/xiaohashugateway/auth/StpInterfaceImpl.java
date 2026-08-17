package com.quanxiaoha.xiaohashu.gateway.xiaohashugateway.auth;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.collection.CollUtil;
import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.nacos.shaded.com.google.common.collect.Lists;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.quanxiaoha.xiaohashu.gateway.xiaohashugateway.constant.RedisKeyConstants;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

@Component
@Slf4j
public class StpInterfaceImpl implements StpInterface {

    @Resource(name = "myStringRedisTemplate")
    RedisTemplate<String, String> redisTemplate;

    @Autowired
    private  ObjectMapper objectMapper;
    /**
     * 获取用户权限列表
     * @param loginId  账号id
     * @param loginType 账号类型
     * @return
     */
    @Override
    @SneakyThrows
    public List<String> getPermissionList(Object loginId, String loginType) {
        log.info("## 获取用户权限列表, loginId: {}", loginId);

        String userRoleKey = RedisKeyConstants.buildUserRoleKey(Long.valueOf(loginId.toString()));
        log.info("===========userRoleKey: {}", userRoleKey);


        //redis中获取用户角色
        String userRoleValue = redisTemplate.opsForValue().get(userRoleKey);
        log.info("================================userRoleValue: {}", userRoleValue);
        if(StringUtils.isEmpty(userRoleValue)){
            return null;
        }

        //将JSON字符转为List<String>角色集合
        List<String> userRoleKeys = objectMapper.readValue(userRoleValue, new TypeReference<List<String>>() {});
        log.info("==========================================userRoleKeys: {}", userRoleKeys);
        if(CollUtil.isNotEmpty(userRoleKeys)){
            //查询角色拥有的权限
            //构建 角色-权限 redis key集合
            List<String> rolePermissionsKeys = userRoleKeys.stream()
                    .map(RedisKeyConstants::buildRolePermissionsKey)
                    .toList();

            //通过key批量查询权限
            List<String> rolePermissionsValues = redisTemplate.opsForValue().multiGet(rolePermissionsKeys);

            if(CollUtil.isNotEmpty(rolePermissionsValues)){
                List<String> permissions = Lists.newArrayList();

                //遍历所有用户的权限集合，并添加到permissions当中
                rolePermissionsValues.forEach(jsonValue -> {
                    try{
                        List<String> rolePermission = objectMapper.readValue(jsonValue,new TypeReference<>() {});
                        permissions.addAll(rolePermission);
                    }catch (JsonProcessingException e){
                        log.error("==>JSON解析错误",e);
                    }
                });

                return permissions;
            }

        }
        return null;


    }

    /**
     * 获取用户列表
     * @param loginId  账号id
     * @param loginType 账号类型
     * @return
     */
    @Override
    @SneakyThrows
    public List<String> getRoleList(Object loginId, String loginType) {
        log.info("## 获取用户角色列表, loginId: {}", loginId);
        String userRoleKey = RedisKeyConstants.buildUserRoleKey(Long.valueOf(loginId.toString()));

        //redis中获取用户角色
        String userRoleValue = redisTemplate.opsForValue().get(userRoleKey);
        if(StringUtils.isEmpty(userRoleValue)){
            return null;
        }

        //JSON转List<String>
        return objectMapper.readValue(userRoleValue, new TypeReference<>() {});
    }
}
