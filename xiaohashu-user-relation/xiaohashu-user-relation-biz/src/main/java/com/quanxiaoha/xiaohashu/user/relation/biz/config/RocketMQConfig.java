package com.quanxiaoha.xiaohashu.user.relation.biz.config;


import org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;


@Import(RocketMQAutoConfiguration.class)
@Configuration
public class RocketMQConfig {
}
