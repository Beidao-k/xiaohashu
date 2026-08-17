package com.quanxiaoha.xiaohashu.note.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = {
        "com.quanxiaoha.xiaohashu.distributed.id.generator.api",
        "com.quanxiaoha.xiaohashu.kv.dto.api",
        "com.quanxiaoha.xiaohashu.user.api"
})
@EnableDiscoveryClient
@MapperScan("com.quanxiaoha.xiaohashu.note.biz.domain.mapper")
public class XiaohashuNoteBizApplication {
    public static void main(String[] args) {
        SpringApplication.run(XiaohashuNoteBizApplication.class, args);
    }
}
