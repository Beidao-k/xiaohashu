package com.quanxiaoha.xiaohashu.count.biz.domain.consumer;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.nacos.shaded.com.google.common.util.concurrent.RateLimiter;
import com.quanxiaoha.framework.common.util.JsonUtils;
import com.quanxiaoha.xiaohashu.count.biz.domain.constant.MQConstants;
import com.quanxiaoha.xiaohashu.count.biz.domain.mapper.UserCountDOMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RocketMQMessageListener(consumerGroup = "xiaohashu_group_"+ MQConstants.TOPIC_COUNT_FANS_2_DB,
        topic = MQConstants.TOPIC_COUNT_FANS_2_DB
)
@Slf4j
public class CountFans2DBConsumer implements RocketMQListener<String> {

    @Resource
    private UserCountDOMapper userCountDOMapper;

    //限流每秒5000请求
    private RateLimiter rateLimiter = RateLimiter.create(5000);

    @Override
    public void onMessage(String message) {
        log.info("==========>粉丝数量开始写入数据库:{}",message);

        //令牌桶削峰限流
        rateLimiter.acquire();
        Map<Long,Integer> countMap = null;
        try {
            countMap = JsonUtils.parseMap(message,Long.class,Integer.class);

        }catch (Exception e){
            log.error("String-->Map异常：{}",e);
        }
        if(CollUtil.isNotEmpty(countMap)){
            countMap.forEach((k,v)->{
                userCountDOMapper.insertOrUpdateFansTotalByUserId(v,k);
            });
        }

    }
}
