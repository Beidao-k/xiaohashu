package com.quanxiaoha.xiaohashu.user.relation.biz.consumer;


import com.alibaba.nacos.common.utils.StringUtils;
import com.alibaba.nacos.shaded.com.google.common.util.concurrent.RateLimiter;
import com.quanxiaoha.framework.biz.context.holer.LoginUserContextHolder;
import com.quanxiaoha.framework.common.exception.BizException;
import com.quanxiaoha.framework.common.util.DateUtils;
import com.quanxiaoha.framework.common.util.JsonUtils;
import com.quanxiaoha.xiaohashu.user.relation.biz.constant.MQConstants;
import com.quanxiaoha.xiaohashu.user.relation.biz.constant.RedisConstants;
import com.quanxiaoha.xiaohashu.user.relation.biz.domain.dataobject.FansDO;
import com.quanxiaoha.xiaohashu.user.relation.biz.domain.dataobject.FollowingDO;
import com.quanxiaoha.xiaohashu.user.relation.biz.domain.mapper.FansDOMapper;
import com.quanxiaoha.xiaohashu.user.relation.biz.domain.mapper.FollowingDOMapper;
import com.quanxiaoha.xiaohashu.user.relation.biz.enums.ResponseCodeEnum;
import com.quanxiaoha.xiaohashu.user.relation.biz.model.dto.FollowUserMqDTO;
import com.quanxiaoha.xiaohashu.user.relation.biz.model.dto.UnfollowUserMqDTO;
import jakarta.annotation.Resource;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Objects;

@Component
@RocketMQMessageListener(consumerGroup = "xiaohashu_group"+MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW,
        topic = MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW,
        consumeMode = ConsumeMode.ORDERLY //顺序消费
)
@Slf4j
public class FollowUnfollowConsumer implements RocketMQListener<Message> {

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private FollowingDOMapper followingDOMapper;

    @Resource
    private FansDOMapper fansDOMapper;


    private RateLimiter rateLimiter = RateLimiter.create(MQConstants.RATE_LIMIT);


    @Resource(name = "myStringRedisTemplate")
    private RedisTemplate<String, String> redisTemplate;


    @Override
    public void onMessage(Message message) {

        //流量削峰，有令牌消费，无令牌阻塞
        rateLimiter.acquire();


        //消息体
        String messageBody = new String(message.getBody());
        //标签
        String tag = message.getTags();

        if(Objects.equals(tag,MQConstants.TAG_FOLLOW)){
            //关注
            handleFollowTagMessage(messageBody);

        }else  if(Objects.equals(tag,MQConstants.TAG_UNFOLLOW)){
            //取消关注 TODO
            handleUnFollowTagMessage(messageBody);

        }else {
            //消息标签错误
            throw  new BizException(ResponseCodeEnum.MESSAGE_TAGS_ERROR);
        }


        log.info("===============>消费者消费消息成功:{}",messageBody);

    }


    /**
     * 新增关注事务
     * @param bodyJsonStr
     */
    private void handleFollowTagMessage(String bodyJsonStr){
        FollowUserMqDTO followUserMqDTO = JsonUtils.parseObject(bodyJsonStr,FollowUserMqDTO.class);
        if(Objects.isNull(followUserMqDTO)){
            return;
        }

        Long userId=followUserMqDTO.getUserId();
        Long followrdUserId = followUserMqDTO.getFollowUserId();
        LocalDateTime localDateTime = LocalDateTime.now();
        FollowingDO followingDO = FollowingDO
                .builder()
                .userId(userId)
                .followingUserId(followrdUserId)
                .createTime(localDateTime)
                .build();
        FansDO fansDO = FansDO
                .builder()
                .userId(followrdUserId)
                .fansUserId(userId)
                .createTime(localDateTime)
                .build();


        //编程式事务提交
        boolean isSuccess = Boolean.TRUE.equals(transactionTemplate.execute( status -> {

            try{
                int count = followingDOMapper.insert(followingDO);
                if(count>0){
                    fansDOMapper.insert(fansDO);
                }
                return true;
            }catch (Exception e){
                status.setRollbackOnly();
                log.error("",e);
                return false;
            }

                }));
        log.info("###############关注操作数据库添加记录结果：{}",isSuccess);


        //更新博主粉丝列表
        // 若数据库操作成功，更新 Redis 中被关注用户的 ZSet 粉丝列表
        if (isSuccess) {
            // Lua 脚本
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_check_and_update_fans_zset.lua")));
            script.setResultType(Long.class);

            // 时间戳
            long timestamp = DateUtils.localDateTime2Timestamp(LocalDateTime.now());

            // 构建被关注用户的粉丝列表 Redis Key
            String fansRedisKey = RedisConstants.buildUserFansKey(followrdUserId);
            // 执行脚本
            redisTemplate.execute(script, Collections.singletonList(fansRedisKey), String.valueOf(userId), String.valueOf(timestamp));
        }



    }

    /**
     * 取消关注事务
     * @param bodyJsonStr
     */
    private void handleUnFollowTagMessage(String bodyJsonStr){
        //取关对象
        UnfollowUserMqDTO unfollowUserMqDTO = JsonUtils.parseObject(bodyJsonStr,UnfollowUserMqDTO.class);

        if(Objects.isNull(unfollowUserMqDTO)){
            return ;
        }
        Long userId = unfollowUserMqDTO.getUserId();
        Long unfollowUserId = unfollowUserMqDTO.getUnfollowUserId();
        LocalDateTime localDateTime = LocalDateTime.now();


        //编程式事务
        boolean isSuccess = Boolean.TRUE.equals(transactionTemplate.execute( status -> {
            try{
                //取关,
                int count  = followingDOMapper.deleteByUserIdAndFollowingUserId(userId,unfollowUserId);
                if(count>0){
                    fansDOMapper.deleteByUserIdAndFansUserId(unfollowUserId,userId);
                }
                return true;
            }catch (Exception e){
                //捕获数据库操作异常
                status.setRollbackOnly();
                log.error("",e);
            }
            return false;
        }));

        //更新对方粉丝缓存,保证原子操作
        if(isSuccess){
            String fnasRedisKey = RedisConstants.buildUserFansKey(unfollowUserId);
            //删除指定粉丝
            redisTemplate.opsForZSet().remove(fnasRedisKey, String.valueOf(userId));
        }



    }

}
