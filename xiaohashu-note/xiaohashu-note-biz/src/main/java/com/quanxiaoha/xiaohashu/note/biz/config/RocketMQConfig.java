package com.quanxiaoha.xiaohashu.note.biz.config;


import org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;


@Import(RocketMQAutoConfiguration.class)
@Configuration
public class RocketMQConfig {
}
