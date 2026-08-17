package com.quanxiaoha.xiaohashu.count.biz.domain.consumer;


import com.alibaba.nacos.common.utils.StringUtils;
import com.quanxiaoha.framework.common.util.JsonUtils;
import com.quanxiaoha.xiaohashu.count.biz.domain.constant.MQConstants;
import com.quanxiaoha.xiaohashu.count.biz.domain.enums.FollowUnfollowTypeEnum;
import com.quanxiaoha.xiaohashu.count.biz.domain.mapper.UserCountDOMapper;
import com.quanxiaoha.xiaohashu.count.biz.domain.model.CountFollowUnfollowMQDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import com.google.common.util.concurrent.RateLimiter;
import java.util.Objects;

@Component
@RocketMQMessageListener(consumerGroup = "xiaohashu_group_"+ MQConstants.TOPIC_COUNT_FOLLOWING_2_DB,
        topic = MQConstants.TOPIC_COUNT_FOLLOWING_2_DB
)
@Slf4j
public class CountFollowing2DBConsumer implements RocketMQListener<String> {


    @Resource
    UserCountDOMapper userCountDOMapper;

    // 每秒创建 5000 个令牌
    private RateLimiter rateLimiter = RateLimiter.create(5000);

    @Override
    public void onMessage(String message) {

        //限流
        rateLimiter.acquire();
        if(StringUtils.isBlank(message)){return;}

        CountFollowUnfollowMQDTO countFollowUnfollowMQDTO = JsonUtils.parseObject(message,CountFollowUnfollowMQDTO.class);
        Integer type = countFollowUnfollowMQDTO.getType();
        Long userId = countFollowUnfollowMQDTO.getUserId();
        Integer count = Objects.equals(type, FollowUnfollowTypeEnum.FOLLOW.getCode()) ? 1 : -1;
        userCountDOMapper.insertOrUpdateFollowingTotalByUserId(count,userId);

    }
}
