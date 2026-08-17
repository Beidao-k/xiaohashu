package com.quanxiaoha.xiaohashu.gateway.xiaohashugateway.filter;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class AddUserId2HeaderFilter implements GlobalFilter {
    public static final String HEADER_USER_ID = "userId";
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        Long userId = null;
        try{
            userId = StpUtil.getLoginIdAsLong();
        }catch (Exception e){

            //若没有登录直接放行
            return chain.filter(exchange);
        }

        Long finalUserId = userId;

        ServerWebExchange newExchange = exchange.mutate()
                .request(builder -> builder.header(HEADER_USER_ID,String.valueOf(finalUserId)))
                .build();


        return chain.filter(newExchange);
    }
}
