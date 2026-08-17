package com.quanxiaoha.xiaohashu.note.biz.consumer;

import com.quanxiaoha.xiaohashu.note.biz.constant.MQConstants;
import com.quanxiaoha.xiaohashu.note.biz.constant.RedisConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RocketMQMessageListener(consumerGroup = "xiaohashu_group"+ MQConstants.TOPIC_DELAY_DELETE_NOTE_REDIS_CACHE,
        topic = MQConstants.TOPIC_DELAY_DELETE_NOTE_REDIS_CACHE
)
public class DelayDeleteNoteRedisCacheConsumer implements RocketMQListener<String> {

    @Resource(name = "myStringRedisTemplate")
    private RedisTemplate<String,String> redisTemplate;

    @Override
    public void onMessage(String message) {
        Long noteId = Long.parseLong(message);
        String noteRedisKey= RedisConstants.buildNoteInfoKey(noteId);
        redisTemplate.delete(noteRedisKey);
        log.info("===========>redis延迟删除成功：{}",noteRedisKey);
    }
}
