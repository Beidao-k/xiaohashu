package com.quanxiaoha.xiaohashu.note.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.nacos.common.utils.StringUtils;
import com.alibaba.nacos.shaded.com.google.common.base.Preconditions;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.quanxiaoha.framework.biz.context.holer.LoginUserContextHolder;
import com.quanxiaoha.framework.common.exception.BizException;
import com.quanxiaoha.framework.common.response.Response;
import com.quanxiaoha.framework.common.util.JsonUtils;
import com.quanxiaoha.xiaohashu.note.biz.constant.MQConstants;
import com.quanxiaoha.xiaohashu.note.biz.constant.RedisConstants;
import com.quanxiaoha.xiaohashu.note.biz.domain.dataobject.NoteDO;
import com.quanxiaoha.xiaohashu.note.biz.domain.mapper.NoteDOMapper;
import com.quanxiaoha.xiaohashu.note.biz.domain.mapper.TopicDOMapper;
import com.quanxiaoha.xiaohashu.note.biz.enums.NoteStatusEnum;
import com.quanxiaoha.xiaohashu.note.biz.enums.NoteTypeEnum;
import com.quanxiaoha.xiaohashu.note.biz.enums.NoteVisibleEnum;
import com.quanxiaoha.xiaohashu.note.biz.enums.ResponseCodeEnum;
import com.quanxiaoha.xiaohashu.note.biz.model.vo.*;
import com.quanxiaoha.xiaohashu.note.biz.rpc.DistributedIdGeneratorRpcService;
import com.quanxiaoha.xiaohashu.note.biz.rpc.KeyValueRpcService;
import com.quanxiaoha.xiaohashu.note.biz.rpc.UserRpcService;
import com.quanxiaoha.xiaohashu.note.biz.service.NoteService;
import com.quanxiaoha.xiaohashu.user.dto.req.FindUserByIdReqDTO;
import com.quanxiaoha.xiaohashu.user.dto.resp.FindUserByIdRespDTO;
import com.quanxiaoha.xiaohashu.user.dto.resp.FindUserByPhoneRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class NoteServiceImpl implements NoteService {

    @Resource
    private DistributedIdGeneratorRpcService distributedIdGeneratorRpcService;

    @Resource
    private KeyValueRpcService keyValueRpcService;

    @Resource
    private TopicDOMapper topicDOMapper;

    @Resource
    private NoteDOMapper noteDOMapper;

    @Resource
    private UserRpcService  userRpcService;

    @Resource(name = "myStringRedisTemplate")
    private RedisTemplate<String,String> redisTemplate;


    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Resource
    RocketMQTemplate rocketMQTemplate;



    private static final Cache<Long, FindNoteDetailRspVO> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(10000) // 设置初始容量为 10000 个条目
            .maximumSize(10000) // 设置缓存的最大容量为 10000 个条目
            .expireAfterWrite(1, TimeUnit.HOURS) // 设置缓存条目在写入后 1 小时过期
            .build();



    @Override
    public Response<?> publishNote(PublishNoteReqVO publishNoteReqVO) {
        log.info("publishNoteReqVO:{}", publishNoteReqVO);
        Integer type = publishNoteReqVO.getType();
        NoteTypeEnum noteTypeEnum = NoteTypeEnum.valueOf(type);

        if(Objects.isNull(noteTypeEnum)){
            throw new BizException(ResponseCodeEnum.NOTE_PUBLISH_FAIL);
        }

        String imgUris = null;
        Boolean isContentEmpty = true;
        String videoUri = null;

        switch (noteTypeEnum){
            case IMAGE_TEXT:
                List<String> imgUriList = publishNoteReqVO.getImgUris();
                Preconditions.checkArgument(CollUtil.isNotEmpty(imgUriList),ResponseCodeEnum.NOTE_PUBLISH_FAIL);
                Preconditions.checkArgument(imgUriList.size() <=8,"笔记图片数量不能超过8");
                imgUris = StringUtils.join(imgUriList,",");
                break;


            case VIDEO:
                videoUri = publishNoteReqVO.getVideoUri();
                Preconditions.checkArgument(StringUtils.isNotBlank(videoUri));
                break;

            default:
                break;

        }

        String snowFlakeIdId = distributedIdGeneratorRpcService.getSnowFlakeId();
        String contentUuid = null;

        String content = publishNoteReqVO.getContent();

        if(StringUtils.isNotBlank(content)){
            isContentEmpty = false;
            contentUuid = UUID.randomUUID().toString();
            boolean isSaveSuccess = keyValueRpcService.svaeNoteContent(contentUuid,content);
            if(!isSaveSuccess){
                throw new BizException(ResponseCodeEnum.NOTE_PUBLISH_FAIL);
            }
        }

        Long topicId = publishNoteReqVO.getTopicId();
        String topicName = null;
        if(Objects.nonNull(topicId)){
            topicName = topicDOMapper.selectNameByPrimaryKey(topicId);
        }

        Long creatorId = LoginUserContextHolder.getUserId();

        NoteDO noteDO = NoteDO.builder()
                .id(Long.valueOf(snowFlakeIdId))
                .isContentEmpty(isContentEmpty)
                .creatorId(creatorId)
                .imgUris(imgUris)
                .title(publishNoteReqVO.getTitle())
                .topicId(publishNoteReqVO.getTopicId())
                .topicName(topicName)
                .type(type)
                .visible(NoteVisibleEnum.PUBLIC.getCode())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .status(NoteStatusEnum.NORMAL.getCode())
                .isTop(Boolean.FALSE)
                .videoUri(videoUri)
                .contentUuid(contentUuid)
                .build();

        try {
            noteDOMapper.insert(noteDO);
        }catch (Exception e){
            log.error("===========>笔记存储失败",e);


            //笔记存储失败则删除笔记内容
            if(StringUtils.isNotBlank(contentUuid)){
                keyValueRpcService.deleteNoteContent(contentUuid);
            }
        }


        return Response.success();
    }

    @Override
    public Response<FindNoteDetailRspVO> findNoteDetailById(FindNoteDetailReqVO findNoteDetailReqVO) {

        //查询的笔记ID
        Long noteId = findNoteDetailReqVO.getId();

        //查询本地缓存
        FindNoteDetailRspVO findNoteDetailRspVOByLocalCache = LOCAL_CACHE.getIfPresent(noteId);
        if(Objects.nonNull(findNoteDetailRspVOByLocalCache)){
            //命中本地缓存
            log.info("============>本地缓存命中:{}",findNoteDetailReqVO);
            return Response.success(findNoteDetailRspVOByLocalCache);
        }

        //查询redis
        String noteRedisKey = RedisConstants.buildNoteInfoKey(noteId);
        String findNoteDetailRspVOByRedis = (String) redisTemplate.opsForValue().get(noteRedisKey);
        if (Objects.nonNull(findNoteDetailRspVOByRedis)) {

            if (StringUtils.isBlank(findNoteDetailRspVOByRedis)) {
                throw new BizException(ResponseCodeEnum.NOTE_ISDELETED);
            }

            FindNoteDetailRspVO findNoteDetailRspVO =
                    JsonUtils.parseObject(findNoteDetailRspVOByRedis, FindNoteDetailRspVO.class);

            return Response.success(findNoteDetailRspVO);
        }



        //当前登录用户id
        Long userId = LoginUserContextHolder.getUserId();

        //根据noteId查询对应笔记
        NoteDO noteDO = noteDOMapper.selectByPrimaryKey(noteId);
        if(Objects.isNull(noteDO)){

            //笔记为空，抛出异常，并写入redis：null
            redisTemplate.opsForValue().set(noteRedisKey,"");
            throw new BizException(ResponseCodeEnum.NOTE_ISDELETED);
        }

        //笔记查看权限校验
        Integer visible = noteDO.getVisible();
        checkNoteVisible(visible,userId,noteDO.getCreatorId());




        //获取笔记用户id
        Long creatorId = noteDO.getCreatorId();
        //根据创建者id查询创建者信息

        CompletableFuture<FindUserByIdRespDTO> userResultFuture = CompletableFuture
                .supplyAsync(()->userRpcService.findUserByIdRespDTO(creatorId),threadPoolTaskExecutor);



        CompletableFuture<String> contentResultFuture = CompletableFuture.completedFuture(null);
        if(Objects.equals(noteDO.getIsContentEmpty(),Boolean.FALSE)){
            contentResultFuture=CompletableFuture
                    .supplyAsync(()->keyValueRpcService.findNoteContent(noteDO.getContentUuid()));

        }

        CompletableFuture<String> finalContentResultFuture = contentResultFuture;
        CompletableFuture<FindNoteDetailRspVO> resultFuture = CompletableFuture
                .allOf(userResultFuture,contentResultFuture)
                .thenApply(s->{
                    FindUserByIdRespDTO findUserByIdRespDTO =userResultFuture.join();
                    String content = finalContentResultFuture.join();
                    //笔记类型
                    Integer type = noteDO.getType();
                    String imgUrisStr = noteDO.getImgUris();
                    List<String> imgUris = null;
                    //如果查询的式图文笔记，需要将图片连接逗号分隔
                    if(Objects.equals(type,NoteTypeEnum.IMAGE_TEXT.getCode())&&StringUtils.isNotBlank(imgUrisStr)){
                        imgUris = List.of(imgUrisStr.split(","));
                    }
                    FindNoteDetailRspVO findNoteDetailRspVO = FindNoteDetailRspVO.builder()
                            .id(noteDO.getId())
                            .type(noteDO.getType())
                            .title(noteDO.getTitle())
                            .content(content)
                            .imgUris(imgUris)
                            .topicId(noteDO.getTopicId())
                            .topicName(noteDO.getTopicName())
                            .creatorId(noteDO.getCreatorId())
                            .creatorName(findUserByIdRespDTO.getNickName())
                            .avatar(findUserByIdRespDTO.getAvatar())
                            .videoUri(noteDO.getVideoUri())
                            .updateTime(noteDO.getUpdateTime())
                            .visible(noteDO.getVisible())
                            .build();

                    return  findNoteDetailRspVO;
                });

        FindNoteDetailRspVO findNoteDetailRspVO = resultFuture.join();

        //写入redis缓存
        redisTemplate.opsForValue().set(noteRedisKey,JsonUtils.toJsonString(findNoteDetailRspVO));

        return Response.success(findNoteDetailRspVO);
    }

    @Override
    public Response<?> UpdateNote(UpdateNoteReqVO updateNoteReqVO) {

        Long noteId = updateNoteReqVO.getId();

        Integer noteType = updateNoteReqVO.getType();


        //日记类型
        NoteTypeEnum noteTypeEnum = NoteTypeEnum.valueOf(noteType);
        if(Objects.isNull(noteTypeEnum)){
            throw  new BizException(ResponseCodeEnum.NOTE_UPDATE_FAIL);

        }

        String imgUris = null;
        String videoUri = null;
        switch (noteTypeEnum){
            case IMAGE_TEXT:
                List<String> imgUriList = updateNoteReqVO.getImgUris();
                Preconditions.checkArgument(CollUtil.isNotEmpty(imgUriList),ResponseCodeEnum.NOTE_PUBLISH_FAIL);
                Preconditions.checkArgument(imgUriList.size() <=8,"笔记图片数量不能超过8");
                imgUris = StringUtils.join(imgUriList,",");
                break;


            case VIDEO:
                videoUri = updateNoteReqVO.getVideoUri();
                Preconditions.checkArgument(StringUtils.isNotBlank(videoUri));
                break;

            default:
                break;

        }


        // 根据话题id获取话题名
        Long topicId = updateNoteReqVO.getTopicId();
        String topicName = null;
        if (Objects.nonNull(topicId)) {
            topicName = topicDOMapper.selectNameByPrimaryKey(topicId);

            // 判断一下提交的话题, 是否是真实存在的
            if (StringUtils.isBlank(topicName)) throw new BizException(ResponseCodeEnum.TOPIC_NOT_FOUND);
        }


        // 笔记内容更新
        // 查询此篇笔记内容对应的 UUID
        NoteDO noteDO1 = noteDOMapper.selectByPrimaryKey(noteId);
        String contentUuid = noteDO1.getContentUuid();

        // 笔记内容是否更新成功
        boolean isUpdateContentSuccess = false;
        if (StringUtils.isBlank(updateNoteReqVO.getContent())) {
            // 若笔记内容为空，则删除 K-V 存储
            isUpdateContentSuccess = keyValueRpcService.deleteNoteContent(contentUuid);
            contentUuid = null;
        } else {
            // 若将无内容的笔记，更新为了有内容的笔记，需要重新生成 UUID
            contentUuid = StringUtils.isBlank(contentUuid) ? UUID.randomUUID().toString() : contentUuid;
            // 调用 K-V 更新短文本
            isUpdateContentSuccess = keyValueRpcService.svaeNoteContent(contentUuid, updateNoteReqVO.getContent());
        }


        //笔记更新完成后删除redis缓存
        String noteDetailRedisKey = RedisConstants.buildNoteInfoKey(noteId);
        redisTemplate.delete(noteDetailRedisKey);


        NoteDO noteDO = NoteDO.builder()
                .id(noteId)
                .isContentEmpty(StringUtils.isBlank(updateNoteReqVO.getContent()))
                .imgUris(imgUris)
                .title(updateNoteReqVO.getTitle())
                .topicId(updateNoteReqVO.getTopicId())
                .topicName(topicName)
                .type(noteType)
                .updateTime(LocalDateTime.now())
                .videoUri(videoUri)
                .contentUuid(contentUuid)
                .build();

        noteDOMapper.updateByPrimaryKeySelective(noteDO);


        //双删缓存保存一致性
        //String noteDetailRedisKey = RedisConstants.buildNoteInfoKey(noteId);
        Message<String> message = MessageBuilder.withPayload(String.valueOf(noteId))
                .build();
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_DELAY_DELETE_NOTE_REDIS_CACHE, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("## 延时删除 Redis 笔记缓存消息发送成功...");
            }

            @Override
            public void onException(Throwable e) {
                log.error("## 延时删除 Redis 笔记缓存消息发送失败...", e);
            }
        },3000,1);
        redisTemplate.delete(noteDetailRedisKey);



        //删除本地缓存

        rocketMQTemplate.syncSend(MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE,String.valueOf(noteId));
        log.info("==============>生产者消息发送成功");



        // 如果更新失败，抛出业务异常，回滚事务
        if (!isUpdateContentSuccess) {
            throw new BizException(ResponseCodeEnum.NOTE_UPDATE_FAIL);
        }

        return Response.success();
    }

    @Override
    public void deleteNoteLocalCache(Long noteId) {

        LOCAL_CACHE.invalidate(noteId);

    }

    @Override
    public Response<?> deleteNote(DeleteNoteReqVO deleteNoteReqVO) {
        Long noteId = deleteNoteReqVO.getId();

        //逻辑删除
        NoteDO noteDO = NoteDO.builder()
                .id(noteId)
                .status(NoteStatusEnum.DELETED.getCode())
                .updateTime(LocalDateTime.now())
                .build();
        int count = noteDOMapper.updateByPrimaryKeySelective(noteDO);
        if(count==0){
            //影响行数为0，该笔记不存在
            throw  new BizException(ResponseCodeEnum.NOTE_UPDATE_FAIL);
        }

        String noteRedisKey = RedisConstants.buildNoteInfoKey(noteId);
        //删除redis缓存
        redisTemplate.delete(noteRedisKey);

        //同步发送广播模式MQ,删除所有本地缓存
        rocketMQTemplate.syncSend(MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE,String.valueOf(noteId));
        log.info("=================>MQ:删除笔记本地缓存发送成功");
        return Response.success();
    }

    @Override
    public Response<?> visibleOnlyMe(VisibleOnlyMeReqVO visibleOnlyMeReqVO) {
        Long loginUserId = LoginUserContextHolder.getUserId();
        Long noteId = visibleOnlyMeReqVO.getId();
        NoteDO noteDO = noteDOMapper.selectByPrimaryKey(noteId);
        Long creatorUserId = noteDO.getCreatorId();
        if(!Objects.equals(loginUserId,creatorUserId)){
            throw new BizException(ResponseCodeEnum.NO_PERMISSION);
        }
        noteDO.setVisible(NoteVisibleEnum.PRIVATE.getCode());
        noteDO.setUpdateTime(LocalDateTime.now());
        try {
            noteDOMapper.updateByPrimaryKeySelective(noteDO);
        }catch (Exception e){
            log.info("数据库更新异常");
        }
        //删除redis缓存
        String noteRedisKey = RedisConstants.buildNoteInfoKey(noteId);
        redisTemplate.delete(noteRedisKey);

        //广播删除本地缓存
        rocketMQTemplate.syncSend(MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE,String.valueOf(noteId));


        return Response.success();
    }

    @Override
    public Response<?> TopNote(TopNoteReqVO topNoteReqVO) {
        Long noteId = topNoteReqVO.getId();

        NoteDO noteDO = NoteDO.builder()
                .id(topNoteReqVO.getId())
                .isTop(topNoteReqVO.getIsTop())
                .updateTime(LocalDateTime.now())
                .creatorId(LoginUserContextHolder.getUserId())
                .build();
        int count = noteDOMapper.updateIsTop(noteDO);
        log.info("================>loginUserId:{}",LoginUserContextHolder.getUserId());
        if(count==0){
            throw new BizException(ResponseCodeEnum.NO_PERMISSION);
        }

        //删除redis缓存
        String noteRedisKey = RedisConstants.buildNoteInfoKey(noteId);
        redisTemplate.delete(noteRedisKey);

        //广播删除本地缓存
        rocketMQTemplate.syncSend(MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE,String.valueOf(noteId));


        return Response.success();
    }


    /**
     * 判断笔记是否可见
     * @param visible
     * @param currUserId
     * @param creatorId
     */

    private  void checkNoteVisible(Integer visible,Long currUserId,Long creatorId){
        if (Objects.equals(visible, NoteVisibleEnum.PRIVATE.getCode())
                && !Objects.equals(currUserId, creatorId)) { // 仅自己可见, 并且访问用户为笔记创建者才能访问，非本人则抛出异常
            throw new BizException(ResponseCodeEnum.NOTE_PRIVATE);
        }
    }
}
