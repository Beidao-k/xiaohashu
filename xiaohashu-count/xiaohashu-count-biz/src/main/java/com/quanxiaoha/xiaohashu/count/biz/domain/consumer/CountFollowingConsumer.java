package com.quanxiaoha.xiaohashu.count.biz.domain.consumer;

import com.quanxiaoha.framework.common.util.JsonUtils;
import com.quanxiaoha.xiaohashu.count.biz.domain.constant.MQConstants;
import com.quanxiaoha.xiaohashu.count.biz.domain.constant.RedisConstants;
import com.quanxiaoha.xiaohashu.count.biz.domain.enums.FollowUnfollowTypeEnum;
import com.quanxiaoha.xiaohashu.count.biz.domain.model.CountFollowUnfollowMQDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.Objects;


@Slf4j
@Component
@RocketMQMessageListener(consumerGroup = "xiaohashu_group_"+ MQConstants.TOPIC_COUNT_FOLLOWING,
        topic =  MQConstants.TOPIC_COUNT_FOLLOWING
)
public class CountFollowingConsumer implements RocketMQListener<String> {





    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Override
    public void onMessage(String body) {

        CountFollowUnfollowMQDTO countFollowUnfollowMQDTO = JsonUtils.parseObject(body,CountFollowUnfollowMQDTO.class);
        Integer type = countFollowUnfollowMQDTO.getType();
        Long userId = countFollowUnfollowMQDTO.getUserId();

        long count = Objects.equals(type, FollowUnfollowTypeEnum.FOLLOW.getCode()) ? 1 : -1;


        //写入redis
        String redisKey = RedisConstants.buildCountUserKey(userId);
        redisTemplate.opsForHash().increment(redisKey,RedisConstants.FIELD_FOLLOWING_TOTAL,count);
        //写入数据库

        Message<String> message = MessageBuilder.withPayload(body).build();

        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_FOLLOWING_2_DB, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数服务：关注数入库】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable e) {
                log.error("==> 【计数服务：关注数入库】MQ 发送异常: ", e);
            }
        });



    }
}
