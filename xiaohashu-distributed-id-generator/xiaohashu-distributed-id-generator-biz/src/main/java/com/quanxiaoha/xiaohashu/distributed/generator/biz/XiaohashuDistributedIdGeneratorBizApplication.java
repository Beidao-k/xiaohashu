package com.quanxiaoha.xiaohashu.distributed.generator.biz;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Arrays;

@SpringBootApplication
@EnableDiscoveryClient
public class XiaohashuDistributedIdGeneratorBizApplication {
    public static void main(String[] args){
        SpringApplication.run(XiaohashuDistributedIdGeneratorBizApplication.class, args);
    }
}
