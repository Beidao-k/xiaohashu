package com.quanxiaoha.xiaohashu.count.biz.domain.consumer;

import com.github.phantomthief.collection.BufferTrigger;
import com.google.common.collect.Maps;
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

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
@RocketMQMessageListener(consumerGroup = "xiaohashu_group_"+ MQConstants.TOPIC_COUNT_FANS,
        topic = MQConstants.TOPIC_COUNT_FANS
)
public class CountFansConsumer implements RocketMQListener<String> {

    @Resource
    RedisTemplate<String,Object> redisTemplate;

    @Resource
    private RocketMQTemplate rocketMQTemplate;


    //BufferTrigger:消息聚合,batchBlocking阻塞超容量消息等待，不丢失消息
    private BufferTrigger<String> bufferTrigger = BufferTrigger.<String>batchBlocking()
            .bufferSize(50000) // 缓存队列的最大容量
            .batchSize(1000)   // 一批次最多聚合 1000 条
            .linger(Duration.ofSeconds(1)) // 多久聚合一次
            .setConsumerEx(this::consumeMessage) // 设置消费者方法
            .build();

    @Override
    public void onMessage(String message) {

        //消息入队
        bufferTrigger.enqueue(message);
    }

    private void consumeMessage(List<String> bodys){

        //消息列表构建
        List<CountFollowUnfollowMQDTO> countFollowUnfollowMqDTOS = bodys
                .stream()
                .map(value->JsonUtils.parseObject(value,CountFollowUnfollowMQDTO.class))
                .toList();

        // 按目标用户进行分组
        Map<Long, List<CountFollowUnfollowMQDTO>> groupMap = countFollowUnfollowMqDTOS.stream()
                .collect(Collectors.groupingBy(CountFollowUnfollowMQDTO::getTargetUserId));


        // 按组汇总数据，统计出最终的计数
        // key 为目标用户ID, value 为最终操作的计数
        Map<Long, Integer> countMap = Maps.newHashMap();
        for (Map.Entry<Long, List<CountFollowUnfollowMQDTO>> entry : groupMap.entrySet()) {
            List<CountFollowUnfollowMQDTO> list = entry.getValue();
            // 最终的计数值，默认为 0
            int finalCount = 0;
            for (CountFollowUnfollowMQDTO countFollowUnfollowMqDTO : list) {
                // 获取操作类型
                Integer type = countFollowUnfollowMqDTO.getType();

                // 根据操作类型，获取对应枚举
                FollowUnfollowTypeEnum followUnfollowTypeEnum = FollowUnfollowTypeEnum.valueOf(type);

                // 若枚举为空，跳到下一次循环
                if (Objects.isNull(followUnfollowTypeEnum)) continue;

                switch (followUnfollowTypeEnum) {
                    case FOLLOW -> finalCount += 1; // 如果为关注操作，粉丝数 +1
                    case UNFOLLOW -> finalCount -= 1; // 如果为取关操作，粉丝数 -1
                }
            }
            // 将分组后统计出的最终计数，存入 countMap 中
            countMap.put(entry.getKey(), finalCount);
        }

        //粉丝数更新写入redis
        // 粉丝数更新写入 Redis
        countMap.forEach((k, v) -> {
            if (v == 0) {
                return;
            }

            String redisKey = RedisConstants.buildCountUserKey(k);

            redisTemplate.opsForHash().increment(
                    redisKey,
                    RedisConstants.FIELD_FANS_TOTAL,
                    v.longValue()
            );
        });

        //TODO 发送消息写入数据库
        //粉丝数更新写入数据库
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(countMap)).build();

        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_FANS_2_DB, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数服务：粉丝数入库】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable e) {
                log.error("==> 【计数服务：粉丝数入库】MQ 发送异常: ", e);
            }
        });

        return ;
    }
}
