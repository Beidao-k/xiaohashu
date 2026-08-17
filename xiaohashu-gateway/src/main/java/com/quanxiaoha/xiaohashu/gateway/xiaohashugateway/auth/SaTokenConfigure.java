package com.quanxiaoha.xiaohashu.gateway.xiaohashugateway.auth;


import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * [Sa-Token 权限认证] 配置类
 */

@Configuration
public class SaTokenConfigure {

    @Bean
    public SaReactorFilter getSaReactorFilter(){
        return new SaReactorFilter()

                //拦截地址
                .addInclude("/**")
                //开发地址
                .addExclude("/favicon.ico")
                //鉴权方法：每次访问进入
                .setAuth(obj -> {
                    //登录拦截
                    SaRouter.match("/**")//拦截所有路由
                            .notMatch("/auth/login") //排除登录接口
                            .notMatch("/auth/verification/code/send")//排除验证码发送接口
                            .check(result -> {StpUtil.checkLogin();});

                    //权限认证  --不同模块，校验不同权限
                   SaRouter.match("/auth/logout",r->StpUtil.checkPermission("app:comment:publish"));

                })
                .setError(e -> {
                    if (e instanceof NotLoginException) { // 未登录异常
                        throw new NotLoginException(e.getMessage(), null, null);
                    } else if (e instanceof NotPermissionException || e instanceof NotRoleException) { // 权限不足，或不具备角色，统一抛出权限不足异常
                        throw new NotPermissionException(e.getMessage());
                    } else { // 其他异常，则抛出一个运行时异常
                        throw new RuntimeException(e.getMessage());
                    }
                })
                ;
    }
}
