package com.quanxiaoha.xiaohashu.user.relation.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.nacos.common.utils.StringUtils;
import com.quanxiaoha.framework.biz.context.holer.LoginUserContextHolder;
import com.quanxiaoha.framework.common.exception.BizException;
import com.quanxiaoha.framework.common.response.PageResponse;
import com.quanxiaoha.framework.common.response.Response;
import com.quanxiaoha.framework.common.util.DateUtils;
import com.quanxiaoha.framework.common.util.JsonUtils;
import com.quanxiaoha.xiaohashu.user.dto.req.FindUserByIdReqDTO;
import com.quanxiaoha.xiaohashu.user.dto.resp.FindUserByIdsRespDTO;
import com.quanxiaoha.xiaohashu.user.relation.biz.constant.MQConstants;
import com.quanxiaoha.xiaohashu.user.relation.biz.constant.RedisConstants;
import com.quanxiaoha.xiaohashu.user.relation.biz.domain.dataobject.FansDO;
import com.quanxiaoha.xiaohashu.user.relation.biz.domain.dataobject.FollowingDO;
import com.quanxiaoha.xiaohashu.user.relation.biz.domain.mapper.FansDOMapper;
import com.quanxiaoha.xiaohashu.user.relation.biz.domain.mapper.FollowingDOMapper;
import com.quanxiaoha.xiaohashu.user.relation.biz.enums.LuaResultEnum;
import com.quanxiaoha.xiaohashu.user.relation.biz.enums.ResponseCodeEnum;
import com.quanxiaoha.xiaohashu.user.relation.biz.model.dto.FollowUserMqDTO;
import com.quanxiaoha.xiaohashu.user.relation.biz.model.dto.UnfollowUserMqDTO;
import com.quanxiaoha.xiaohashu.user.relation.biz.model.vo.*;
import com.quanxiaoha.xiaohashu.user.relation.biz.rpc.UserRpcService;
import com.quanxiaoha.xiaohashu.user.relation.biz.service.UserRelationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;


@Service
@Slf4j
public class UserRelationServiceImpl implements UserRelationService {

    @Resource
    private FollowingDOMapper  followingDOMapper;

    @Resource(name = "myStringRedisTemplate")
    private RedisTemplate<String, String> redisTemplate;


    @Resource
    private UserRpcService userRpcService;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Resource
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Resource
    private FansDOMapper fansDOMapper;





    @Override
    public Response<?> follow(FollowUserReqVO followUserReqVO) {

        //被关注者id
        Long followedUserId = followUserReqVO.getUserId();

        //关注者id
        Long followingUserId = LoginUserContextHolder.getUserId();


        //校验，无法关注自己
        if(Objects.equals(followedUserId,followingUserId)){
            throw  new BizException(ResponseCodeEnum.CANT_FOLLOW_YOUR_SELF);
        }


        boolean userExist = userRpcService.isUserExist(followedUserId);
        if(!userExist){
            throw new BizException(ResponseCodeEnum.FOLLOW_USER_NOT_EXISTED);
        }



        String followingRedisKey = RedisConstants.buildFollowingListKey(followingUserId);
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/Lua/follow_check_and_add.lua")));
        script.setResultType(Long.class);
        long timestamp = DateUtils.localDateTime2Timestamp(LocalDateTime.now());

        //执行Lua脚本
        Long result = redisTemplate.execute(
                script,
                Collections.singletonList(followingRedisKey),
                String.valueOf(followedUserId),
                String.valueOf(timestamp)
        );

        LuaResultEnum luaResultEnum = LuaResultEnum.valueOf(result);



        checkLuaScriptResult(result);

        // ZSET 不存在
        if (Objects.equals(result, LuaResultEnum.ZSET_NOT_EXIST.getCode())) {
            // 从数据库查询当前用户的关注关系记录
            List<FollowingDO> followingDOS = followingDOMapper.selectByUserId(followingUserId);

            // 随机过期时间
            // 保底1天+随机秒数
            long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);

            // 若记录为空，直接 ZADD 对象, 并设置过期时间
            if (CollUtil.isEmpty(followingDOS)) {

                DefaultRedisScript<Long> script2 = new DefaultRedisScript<>();
                script2.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_add_and_expire.lua")));
                script2.setResultType(Long.class);
                redisTemplate.execute(
                        script2,
                        Collections.singletonList(followingRedisKey),
                        String.valueOf(followedUserId),
                        String.valueOf(timestamp),
                        String.valueOf(expireSeconds)
                );

            } else { // 若记录不为空，则将关注关系数据全量同步到 Redis 中，并设置过期时间；
                // 构建 Lua 参数
                Object[] luaArgs = buildLuaArgs(followingDOS, expireSeconds);

                // 执行 Lua 脚本，批量同步关注关系数据到 Redis 中
                DefaultRedisScript<Long> script3 = new DefaultRedisScript<>();
                script3.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_batch_add_and_expire.lua")));
                script3.setResultType(Long.class);
                redisTemplate.execute(script3, Collections.singletonList(followingRedisKey), luaArgs);

                // 再次调用上面的 Lua 脚本：follow_check_and_add.lua , 将最新的关注关系添加进去
                result = redisTemplate.execute(
                        script,
                        Collections.singletonList(followingRedisKey),
                        followedUserId,
                        timestamp
                );

                checkLuaScriptResult(result);
            }
            }

        //TODO,发送MQ写入数据库
        FollowUserMqDTO followUserMqDTO = FollowUserMqDTO
                .builder()
                .userId(followingUserId)
                .followUserId(followedUserId)
                .createTime(LocalDateTime.now())
                .build();
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(followUserMqDTO))
                .build();

        String destination = MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW+":"+MQConstants.TAG_FOLLOW;
        log.info("======================>开始发送关注操作 MQ,消息体:{}",followUserMqDTO);
        String hashKey = String.valueOf(followingUserId);
        SendResult sendResult = rocketMQTemplate.syncSendOrderly(destination, message, hashKey);
        log.info("==> MQ 发送结果，SendResult: {}", sendResult);

        return Response.success();


    }

    /**
     * 取消关注
     * @param unFollowUserReqVO
     * @return
     */

    @Override
    public Response<?> unfollow(UnFollowUserReqVO unFollowUserReqVO) {


        //想要取关的用户id
        Long unfollowUserId = unFollowUserReqVO.getId();
        //当前发送请求的用户id
        Long userId = LoginUserContextHolder.getUserId();
        //无法取关自己
        if(Objects.equals(unfollowUserId,userId)){
            throw new BizException(ResponseCodeEnum.CANT_UNFOLLOW_YOUR_SELF);
        }



        //判断是否已经关注
        boolean userExist = userRpcService.isUserExist(unfollowUserId);
        if(!userExist){
            throw new BizException(ResponseCodeEnum.FOLLOW_USER_NOT_EXISTED);
        }


        //当前用户的关注列表Redis Key
        String followingRedisKey = RedisConstants.buildFollowingListKey(userId);
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        //Lua脚本路径
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/unfollow_check_and_delete.lua")));
        // 返回值类型
        script.setResultType(Long.class);
        Long result = redisTemplate.execute(script,Collections.singletonList(followingRedisKey),String.valueOf(unfollowUserId));


        //检验Lua脚本执行结果
        //取关的用户不在关注列表
        if(Objects.equals(result,LuaResultEnum.NOT_FOLLOWED.getCode())){
            throw  new BizException(ResponseCodeEnum.NOT_FOLLOWED);
        }

        if(Objects.equals(result,LuaResultEnum.ZSET_NOT_EXIST.getCode())){
            //缓存不存在，改用数据库查询判断

            // 从数据库查询当前用户的关注关系记录
            List<FollowingDO> followingDOS = followingDOMapper. selectByUserId(userId);

            // 随机过期时间
            // 保底1天+随机秒数
            long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);

            // 若记录为空，则表示还未关注任何人，提示还未关注对方
            if (CollUtil.isEmpty(followingDOS)) {
                throw new BizException(ResponseCodeEnum.NOT_FOLLOWED);
            } else { // 若记录不为空，则将关注关系数据全量同步到 Redis 中，并设置过期时间；
                // 构建 Lua 参数
                Object[] luaArgs = buildLuaArgs(followingDOS, expireSeconds);

                // 执行 Lua 脚本，批量同步关注关系数据到 Redis 中
                DefaultRedisScript<Long> script3 = new DefaultRedisScript<>();
                script3.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_batch_add_and_expire.lua")));
                script3.setResultType(Long.class);
                redisTemplate.execute(script3, Collections.singletonList(followingRedisKey), luaArgs);

                // 再次调用上面的 Lua 脚本：unfollow_check_and_delete.lua , 将取关的用户删除
                result = redisTemplate.execute(script, Collections.singletonList(followingRedisKey),String.valueOf(unfollowUserId));
                // 再次校验结果
                if (Objects.equals(result, LuaResultEnum.NOT_FOLLOWED.getCode())) {
                    throw new BizException(ResponseCodeEnum.NOT_FOLLOWED);
                }
        }}

        //发送消息，修改关注数据库
        UnfollowUserMqDTO unfollowUserMqDTO = UnfollowUserMqDTO
                .builder()
                .userId(userId)
                .createTime(LocalDateTime.now())
                .unfollowUserId(unfollowUserId)
                .build();

        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(unfollowUserMqDTO)).build();

        //消息 TOPIC，TAG
        String destination = MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW+":"+MQConstants.TAG_UNFOLLOW;

        log.info("=============>开始发送取关操作消息,消息体:{}",unfollowUserMqDTO);

        //数据库操作消息
        String hashKey = String.valueOf(userId);
        // 发送 MQ 消息
        SendResult sendResult = rocketMQTemplate.syncSendOrderly(destination, message, hashKey);

        return Response.success();

    }

    /**
     * 查询用户关注列表
     * @param findFollowingListReqVO
     * @return
     */

    @Override
    public PageResponse<FindFollowingUserRspVO> findFollowingList(FindFollowingListReqVO findFollowingListReqVO) {

        //用户ID
        Long userId =findFollowingListReqVO.getUserId();
        //查询页号
        Integer pageNo = findFollowingListReqVO.getPageNo();

        String followingListRedisKey = RedisConstants.buildFollowingListKey(userId);

        //用户关注列表ZSet的总大小
        long total = redisTemplate.opsForZSet().zCard(followingListRedisKey);

        long limit = 10;//每页展示10条数据

        //返回参数
        List<FindFollowingUserRspVO> findFollowingUserRspVOS = null;
        log.info("==================total=:{}",total);
        if(total>0){ //缓存中有数据，直接从缓存中读取
            log.info("进入成功");

            long totalPage =PageResponse.getTotalPage(total,limit);//计算总页数
            if(pageNo>totalPage) return PageResponse.success(null,pageNo,totalPage);//请求页码超出总页数

            //从Redis中查询ZSet分页数据
            //每页10个元素，计算偏移量
            long offset = PageResponse.getOffset(totalPage,limit);

            Set<String> followingUserIdsSet = redisTemplate.opsForZSet()
                    .reverseRangeByScore(followingListRedisKey, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, offset, limit);

            if(CollUtil.isNotEmpty(followingUserIdsSet)){
                //查询结果不为空
                //获取所有关注用户id集合
                List<Long> followingUserIds = followingUserIdsSet.stream()
                        .map(value-> Long.valueOf(value))
                        .toList();

                //调用UserRpc批量获取用户信息
                List<FindUserByIdsRespDTO> findUserByIdRspDTOS = userRpcService.findByIds(followingUserIds);
                if(CollUtil.isNotEmpty(findUserByIdRspDTOS)){
                    //若不为空,将查询结果转为VO
                    findFollowingUserRspVOS = findUserByIdRspDTOS.stream()
                            .map(dto->FindFollowingUserRspVO
                                    .builder()
                                    .userId(dto.getId())
                                    .avatar(dto.getAvatar())
                                    .nickname(dto.getNickName())
                                    .introduction(dto.getIntroduction())
                                    .build()
                            )
                            .toList();
                }
            }
        }else {
            //TODO:若 Redis无缓存,从数据库查询并同步到Redis
            //记录总数
            total = followingDOMapper.selectCountByUserId(userId);
            //总页数
            long totalPage = PageResponse.getTotalPage(total,limit);
            if(pageNo>totalPage) return PageResponse.success(null,pageNo,totalPage);//查找页数>总页数

            long offset = PageResponse.getOffset(pageNo,limit);

            //分页查询
            List<FollowingDO> followingDOS = followingDOMapper.selectPageListByUserId(userId, offset, limit);
            if(CollUtil.isEmpty(followingDOS)){
                return PageResponse.success(null,pageNo,totalPage);
            }
            List<Long> followingIds = followingDOS.stream().map(dto->Long.valueOf(dto.getFollowingUserId())).toList();
            List<FindUserByIdsRespDTO> byIds = userRpcService.findByIds(followingIds);
            if(CollUtil.isEmpty(byIds)) return PageResponse.success(null,pageNo,totalPage);
            findFollowingUserRspVOS = byIds.stream()
                    .map(dto->FindFollowingUserRspVO
                            .builder()
                            .userId(dto.getId())
                            .avatar(dto.getAvatar())
                            .nickname(dto.getNickName())
                            .introduction(dto.getIntroduction())
                            .build()
                    )
                    .toList();

            //异步将关注列表全量同步到Redis
            threadPoolTaskExecutor.submit(()->{
                syncFollowingList2Redis(userId);
            });
        }


        return PageResponse.success(findFollowingUserRspVOS,pageNo,total);
    }

    /**
     * 分页查询用户粉丝列表
     * @param findFansListReqVO
     * @return
     */
    @Override
    public PageResponse<FindFansUserRspVO> findFansList(FindFansListReqVO findFansListReqVO) {
        Long userId = findFansListReqVO.getUserId();
        long pageNo = findFansListReqVO.getPageNo();

        //先从redis中查询
        String fansListRedisKey = RedisConstants.buildUserFansKey(userId);
        long redisTotal = redisTemplate.opsForZSet().zCard(fansListRedisKey);


        List<FindFansUserRspVO> findFansUserRspVO = null;
        long total =0;
        long limit =10; //每页展示数据量
         if(redisTotal>0){
             //从redis中获取
             //计算总页数
             total =PageResponse.getTotalPage(redisTotal,limit);
             if(pageNo>total)return PageResponse.success(null,pageNo,total);

             //计算偏移量
             long offset = PageResponse.getOffset(pageNo,limit);
             // 使用 ZREVRANGEBYSCORE 命令按 score 降序获取元素，同时使用 LIMIT 子句实现分页
             Set<String> followingUserIdsSet = redisTemplate.opsForZSet()
                     .reverseRangeByScore(fansListRedisKey, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, offset, limit);
             if(CollUtil.isNotEmpty(followingUserIdsSet)){
                 List<Long> userIds = followingUserIdsSet.stream()
                         .map(value-> Long.valueOf(value))
                         .toList();
                 List<FindUserByIdsRespDTO> byIds = userRpcService.findByIds(userIds);
                 findFansUserRspVO = byIds.stream()
                         .map(dto->FindFansUserRspVO
                                 .builder()
                                 .userId(dto.getId())
                                 .nickname(dto.getNickName())
                                 .avatar(dto.getAvatar())
                                 .noteTotal(0L)//TODO//补充查询笔记数
                                 .fansTotal(0L)//TODO 补充查询粉丝数
                                 .build())
                         .toList();
             }
             return PageResponse.success(null,pageNo,total);

         }else {
             //从数据库中查询
             long databaseTotal = fansDOMapper.selectCountByUserId(userId);

             total= PageResponse.getTotalPage(databaseTotal,limit);
             if(pageNo>total)return PageResponse.success(null,pageNo,total);

             long offset = PageResponse.getOffset(pageNo,limit);

             List<FansDO> fansDOS = fansDOMapper.selectPageListByUserId(userId, offset, limit);
             if(CollUtil.isEmpty(fansDOS)){return PageResponse.success(null,pageNo,total);}
             List<Long> fansIds = fansDOS.stream().map(dto->Long.valueOf(dto.getFansUserId())).toList();

             List<FindUserByIdsRespDTO> byIds = userRpcService.findByIds(fansIds);
             findFansUserRspVO = byIds.stream()
                     .map(dto->FindFansUserRspVO
                             .builder()
                             .userId(dto.getId())
                             .nickname(dto.getNickName())
                             .avatar(dto.getAvatar())
                             .noteTotal(0L)//TODO//补充查询笔记数
                             .fansTotal(0L)//TODO 补充查询粉丝数
                             .build())
                     .toList();

             //同步至Redis
             threadPoolTaskExecutor.submit(()->{

             });


         }


        return PageResponse.success(findFansUserRspVO,pageNo,total);
    }

    private void syncFansList2Redis(Long userId) {
        // 同步粉丝列表至Redis
        List<FansDO> fansDOS = fansDOMapper.select5000FansByUserId(userId);
        if(CollUtil.isEmpty(fansDOS))return;
        String fansListRedisKey = RedisConstants.buildUserFansKey(userId);
        long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);
        // 构建 Lua 参数
        Object[] luaArgs = buildFansZSetLuaArgs(fansDOS, expireSeconds);

        // 执行 Lua 脚本，批量同步关注关系数据到 Redis 中
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_batch_add_and_expire.lua")));
        script.setResultType(Long.class);
        redisTemplate.execute(script, Collections.singletonList(fansListRedisKey), luaArgs);

    }

    /**
     * 构建 Lua 脚本参数
     * @param fansDOS
     * @param expireSeconds
     * @return
     */
    private static Object[] buildFansZSetLuaArgs(List<FansDO> fansDOS, long expireSeconds) {
        int argsLength = fansDOS.size() * 2 + 1; // 每个粉丝关系有 2 个参数（score 和 value），再加一个过期时间
        Object[] luaArgs = new Object[argsLength];

        int i = 0;
        for (FansDO fansDO : fansDOS) {
            luaArgs[i] = DateUtils.localDateTime2Timestamp(fansDO.getCreateTime()); // 粉丝的关注时间作为 score
            luaArgs[i + 1] = fansDO.getFansUserId();          // 粉丝的用户 ID 作为 ZSet value
            i += 2;
        }

        luaArgs[argsLength - 1] = expireSeconds; // 最后一个参数是 ZSet 的过期时间
        return luaArgs;
    }



    /**
     * 将用户全部关注同步至Redis缓存
     * @param userId
     */
    private void syncFollowingList2Redis(Long userId){
        //用户全部关注同步至Redis

        List<FollowingDO> followingDOS = followingDOMapper.selectAllByUserId(userId);
        if(CollUtil.isNotEmpty(followingDOS)){
            String followingListRedisKey = RedisConstants.buildFollowingListKey(userId);
            // 随机过期时间
            // 保底1天+随机秒数
            long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);
            // 构建 Lua 参数
            Object[] luaArgs = buildLuaArgs(followingDOS, expireSeconds);

            // 执行 Lua 脚本，批量同步关注关系数据到 Redis 中
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_batch_add_and_expire.lua")));
            script.setResultType(Long.class);
            redisTemplate.execute(script, Collections.singletonList(followingListRedisKey), luaArgs);
        }

    }


    /**
     * 校验 Lua 脚本结果，根据状态码抛出对应的业务异常
     * @param result
     */
    private static void checkLuaScriptResult(Long result) {
        LuaResultEnum luaResultEnum = LuaResultEnum.valueOf(result);

        if (Objects.isNull(luaResultEnum)) throw new RuntimeException("Lua 返回结果错误");
        // 校验 Lua 脚本执行结果
        switch (luaResultEnum) {
            // 关注数已达到上限
            case FOLLOW_LIMIT -> throw new BizException(ResponseCodeEnum.FOLLOWING_COUNT_LIMIT);
            // 已经关注了该用户
            case ALREADY_FOLLOWED -> throw new BizException(ResponseCodeEnum.ALREADY_FOLLOWED);
        }
    }


    /**
     * 构建 Lua 脚本参数
     *
     * @param followingDOS
     * @param expireSeconds
     * @return
     */
    private static Object[] buildLuaArgs(List<FollowingDO> followingDOS, long expireSeconds) {
        int argsLength = followingDOS.size() * 2 + 1; // 每个关注关系有 2 个参数（score 和 value），再加一个过期时间
        Object[] luaArgs = new Object[argsLength];

        int i = 0;
        for (FollowingDO following : followingDOS) {
            luaArgs[i] = String.valueOf(
                    DateUtils.localDateTime2Timestamp(following.getCreateTime())
            );
            luaArgs[i + 1] = String.valueOf(following.getFollowingUserId());
            i += 2;
        }

        luaArgs[argsLength - 1] = String.valueOf(expireSeconds); // 最后一个参数是 ZSet 的过期时间
        return luaArgs;
    }


}
