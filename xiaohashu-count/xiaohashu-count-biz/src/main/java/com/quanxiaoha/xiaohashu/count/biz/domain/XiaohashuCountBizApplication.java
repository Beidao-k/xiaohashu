package com.quanxiaoha.xiaohashu.count.biz.domain;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@MapperScan("com.quanxiaoha.xiaohashu.count.biz.domain.mapper")
@EnableDiscoveryClient
public class XiaohashuCountBizApplication {
    public static  void main(String[] args) {
        SpringApplication.run(XiaohashuCountBizApplication.class, args);
    }
}
