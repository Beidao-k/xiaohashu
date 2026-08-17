package com.quanxiaoha.xiaohashu.gateway.xiaohashugateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
public class XiaohashuGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiaohashuGatewayApplication.class, args);
    }

}
