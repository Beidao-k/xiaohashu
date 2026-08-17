package com.quanxiaoha.xiaohashu.user.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.nacos.shaded.com.google.common.base.Preconditions;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Lists;
import com.quanxiaoha.framework.biz.context.holer.LoginUserContextHolder;
import com.quanxiaoha.framework.common.enums.DeletedEnum;
import com.quanxiaoha.framework.common.enums.StatusEnum;
import com.quanxiaoha.framework.common.exception.BizException;
import com.quanxiaoha.framework.common.response.Response;
import com.quanxiaoha.framework.common.util.JsonUtils;
import com.quanxiaoha.framework.common.util.ParamUtils;
import com.quanxiaoha.xiaohashu.user.biz.constant.RedisKeyConstants;
import com.quanxiaoha.xiaohashu.user.biz.constant.RoleConstants;
import com.quanxiaoha.xiaohashu.user.biz.domain.dataobject.UserDO;
import com.quanxiaoha.xiaohashu.user.biz.domain.dataobject.UserRoleDO;
import com.quanxiaoha.xiaohashu.user.biz.domain.mapper.*;
import com.quanxiaoha.xiaohashu.user.biz.enums.ResponseCodeEnum;
import com.quanxiaoha.xiaohashu.user.biz.enums.SexEnum;
import com.quanxiaoha.xiaohashu.user.biz.model.vo.UpdateUserInfoReqVO;
import com.quanxiaoha.xiaohashu.user.biz.rpc.DistributedIdGeneratorRpcService;
import com.quanxiaoha.xiaohashu.user.biz.rpc.OssRpcService;
import com.quanxiaoha.xiaohashu.user.biz.service.UserService;
import com.quanxiaoha.xiaohashu.user.dto.req.*;
import com.quanxiaoha.xiaohashu.user.dto.resp.FindUserByIdRespDTO;
import com.quanxiaoha.xiaohashu.user.dto.resp.FindUserByIdsRespDTO;
import com.quanxiaoha.xiaohashu.user.dto.resp.FindUserByPhoneRespDTO;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Resource
    private UserDOMapper userDOMapper;

    @Resource
    private OssRpcService  ossRpcService;

    @Resource
    private UserRoleDOMapper userRoleDOMapper;
    @Resource
    private RoleDOMapper roleDOMapper;
    @Resource(name = "myStringRedisTemplate")
    private RedisTemplate<String, String> redisTemplate;

    @Resource
    private DistributedIdGeneratorRpcService  distributedIdGeneratorRpcService;

    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;


    /**
     * 用户信息本地缓存
     */
    private static final Cache<Long, FindUserByIdRespDTO> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(10000) // 设置初始容量为 10000 个条目
            .maximumSize(10000) // 设置缓存的最大容量为 10000 个条目
            .expireAfterWrite(1, TimeUnit.HOURS) // 设置缓存条目在写入后 1 小时过期
            .build();



    @Override
    public Response<?> updateUserInfo(UpdateUserInfoReqVO updateUserInfoReqVO) {
        log.info("=================================================开始修改=========================");

        UserDO userDO = new UserDO();

        //设置当前需要更新的用户的ID
        userDO.setId(LoginUserContextHolder.getUserId());

        //标识位：是否要更新
        boolean needUpdate = false;

        //头像
        MultipartFile avatarFile = updateUserInfoReqVO.getAvatar();
        if(avatarFile!=null){
            String avatar = ossRpcService.uploadFile(avatarFile);
            log.info("==> 调用 oss 服务成功，上传头像，url：{}", avatar);

            // 若上传头像失败，则抛出业务异常
            if (StringUtils.isBlank(avatar)) {
                throw new BizException(ResponseCodeEnum.UPLOAD_AVATAR_FAIL);
            }

            userDO.setAvatar(avatar);
            needUpdate = true;
        }

        // 昵称
        String nickname = updateUserInfoReqVO.getNickname();
        if (StringUtils.isNotBlank(nickname)) {
            Preconditions.checkArgument(ParamUtils.checkNickname(nickname), ResponseCodeEnum.NICK_NAME_VALID_FAIL.getErrorMessage());
            userDO.setNickname(nickname);
            needUpdate = true;
        }


        // 小哈书号
        String xiaohashuId = updateUserInfoReqVO.getXiaohashuId();
        if (StringUtils.isNotBlank(xiaohashuId)) {
            Preconditions.checkArgument(ParamUtils.checkXiaohashuId(xiaohashuId), ResponseCodeEnum.XIAOHASHU_ID_VALID_FAIL.getErrorMessage());
            userDO.setXiaohashuId(xiaohashuId);
            needUpdate = true;
        }

        // 性别
        Integer sex = updateUserInfoReqVO.getSex();
        if (Objects.nonNull(sex)) {
            Preconditions.checkArgument(SexEnum.isValid(sex), ResponseCodeEnum.SEX_VALID_FAIL.getErrorMessage());
            userDO.setSex(sex);
            needUpdate = true;
        }

        // 生日
        LocalDate birthday = updateUserInfoReqVO.getBirthday();
        if (Objects.nonNull(birthday)) {
            userDO.setBirthday(birthday);
            needUpdate = true;
        }

        // 个人简介
        String introduction = updateUserInfoReqVO.getIntroduction();
        if (StringUtils.isNotBlank(introduction)) {
            Preconditions.checkArgument(ParamUtils.checkLength(introduction, 100), ResponseCodeEnum.INTRODUCTION_VALID_FAIL.getErrorMessage());
            userDO.setIntroduction(introduction);
            needUpdate = true;
        }

        // 背景图
        MultipartFile backgroundImgFile = updateUserInfoReqVO.getBackgroundImg();
        if (Objects.nonNull(backgroundImgFile)) {
            // todo: 调用对象存储服务上传文件
            if (Objects.nonNull(backgroundImgFile)) {
                String backgroundImg = ossRpcService.uploadFile(backgroundImgFile);
                log.info("==> 调用 oss 服务成功，上传背景图，url：{}", backgroundImg);

                // 若上传背景图失败，则抛出业务异常
                if (StringUtils.isBlank(backgroundImg)) {
                    throw new BizException(ResponseCodeEnum.UPLOAD_BACKGROUND_IMG_FAIL);
                }

                userDO.setBackgroundImg(backgroundImg);
                needUpdate = true;
            }
        }

        if (needUpdate) {
            // 更新用户信息
            userDO.setUpdateTime(LocalDateTime.now());
            userDOMapper.updateByPrimaryKeySelective(userDO);
        }
        return Response.success();

    }

    @Override
    @Transactional
    public Response<Long> register(RegisterUserReqDTO registerUserReqDTO) {
        String phone = registerUserReqDTO.getPhone();

        // 先判断该手机号是否已被注册
        UserDO userDO1 = userDOMapper.selectByPhone(phone);

        log.info("==> 用户是否注册, phone: {}, userDO: {}", phone, JsonUtils.toJsonString(userDO1));

        // 若已注册，则直接返回用户 ID
        if (Objects.nonNull(userDO1)) {
            return Response.success(userDO1.getId());
        }

        // 否则注册新用户
        // 获取全局自增的小哈书 ID
        //Long xiaohashuId = redisTemplate.opsForValue().increment(RedisKeyConstants.XIAOHASHU_ID_GENERATOR_KEY);

        String xiaohashuId = distributedIdGeneratorRpcService.getXiaohashuId();
        Long userIds = Long.valueOf(distributedIdGeneratorRpcService.getUserId());





        UserDO userDO = UserDO.builder()
                .id(userIds)
                .phone(phone)
                .xiaohashuId(xiaohashuId) // 自动生成小红书号 ID
                .nickname("小红薯" + xiaohashuId) // 自动生成昵称, 如：小红薯10000
                .status(StatusEnum.ENABLE.getValue()) // 状态为启用
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .isDeleted(DeletedEnum.NO.getValue()) // 逻辑删除
                .build();

        // 添加入库
        userDOMapper.insert(userDO);

        // 获取刚刚添加入库的用户 ID
        Long userId = userDO.getId();
        log.info("===================userId : {}", userId);

        // 给该用户分配一个默认角色
        UserRoleDO userRoleDO = UserRoleDO.builder()
                .userId(userId)
                .roleId(RoleConstants.COMMON_USER_ROLE_ID)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .isDeleted(DeletedEnum.NO.getValue())
                .build();
        userRoleDOMapper.insert(userRoleDO);

        //查询用户所有角色
        List<Long> userRoleList = userRoleDOMapper.selectRoleByUserId(userId);

//        //获取权限列表
//        List<RolePermissionDO> rolePermissionDOS = rolePermissionDOMapper.selectByRoleIds(userRoleList);
//        //权限id
//        List<Long> permissionList = new ArrayList<>();
//        for (RolePermissionDO rolePermissionDO : rolePermissionDOS) {
//            permissionList.add(rolePermissionDO.getPermissionId());
//        }
//
//
//        //获取该用户所有权限
//        List<String> userPermissions = permissionDOMapper.selectPermissionListById(permissionList);
//

        // 将该用户的角色 ID 存入 Redis 中

        List<String> userRoleNames = roleDOMapper.selectAllRoleNameByPrimaryKey(userRoleList);


        String userRolesKey = RedisKeyConstants.buildUserRoleKey(userId);
        redisTemplate.opsForValue().set(userRolesKey,JsonUtils.toJsonString(userRoleNames));

        return Response.success(userId);
    }



    @Override
    public Response<FindUserByPhoneRespDTO> findUserByPhone(FindUserByPhoneReqDTO findUserByPhoneReqDTO) {
        //根据手机查询用户信息
        UserDO userDO = userDOMapper.selectByPhone(findUserByPhoneReqDTO.getPhone());

        if(Objects.isNull(userDO)){
            throw new BizException(ResponseCodeEnum.USER_NOT_FOUND);
        }
        FindUserByPhoneRespDTO  findUserByPhoneRespDTO = FindUserByPhoneRespDTO.builder()
                .id(userDO.getId())
                .password(userDO.getPassword())
                .build();
        return Response.success(findUserByPhoneRespDTO);
    }


    /**
     * 更新用户密码
     * @param updatePasswordReqDTO
     * @return
     */
    @Override
    public Response<?> updatePassword(UpdatePasswordReqDTO updatePasswordReqDTO) {
        Long userId = LoginUserContextHolder.getUserId();
        UserDO userDO = UserDO.builder()
                .id(userId)
                .password(updatePasswordReqDTO.getEncodePassword())
                .updateTime(LocalDateTime.now())
                .build();
        userDOMapper.updateByPrimaryKeySelective(userDO);
        return Response.success();
    }

    @Override
    public Response<FindUserByIdRespDTO> findUserById(FindUserByIdReqDTO findUserByIdReqDTO) {
        Long userId = findUserByIdReqDTO.getUserId();

        //查询本地缓存
        FindUserByIdRespDTO findUserByIdRespDTOlocalCache = LOCAL_CACHE.getIfPresent(userId);
        if(Objects.nonNull(findUserByIdRespDTOlocalCache)){
            //命中缓存
            log.info("本地缓存命中:{}", findUserByIdRespDTOlocalCache);
            return Response.success(findUserByIdRespDTOlocalCache);

        }


        //查询redis缓存
        String userInfoRedisKey = RedisKeyConstants.buildUserInfoKey(userId);
        String userInfoRedisValue = (String) redisTemplate.opsForValue().get(userInfoRedisKey);
        if(StringUtils.isNotBlank(userInfoRedisValue)){
            //redis命中缓存
            FindUserByIdRespDTO findUserByIdRespDTO = JsonUtils.parseObject(userInfoRedisValue, FindUserByIdRespDTO.class);
            //异步线程写入本地缓存
            threadPoolTaskExecutor.submit(()->{
               LOCAL_CACHE.put(userId,findUserByIdRespDTO);
            });

            return Response.success(findUserByIdRespDTO);

        }


        UserDO userDO = userDOMapper.selectByPrimaryKey(userId);

        if(Objects.isNull(userDO)){
            threadPoolTaskExecutor.submit(()->{
                redisTemplate.opsForValue().set(userInfoRedisKey,"",3,TimeUnit.MINUTES);
            });

            throw new BizException(ResponseCodeEnum.USER_NOT_FOUND);
        }

        FindUserByIdRespDTO findUserByIdRespDTO = FindUserByIdRespDTO
                .builder()
                .id(userId)
                .avatar(userDO.getAvatar())
                .nickName(userDO.getNickname())
                .introduction(userDO.getIntroduction())
                .build();

        threadPoolTaskExecutor.submit(()->{
            redisTemplate.opsForValue().set(userInfoRedisKey,
                    JsonUtils.toJsonString(findUserByIdRespDTO),
                    1,
                    TimeUnit.DAYS);
        });

        return Response.success(findUserByIdRespDTO);
    }

    @Override
    public Response<List<FindUserByIdsRespDTO>> findByIds(FindUserByIdsReqDTO findUserByIdsReqDTO) {
        //需要查询的用户 ID 集合
        List<Long> userIds = findUserByIdsReqDTO.getIds();

        //构建Redis Key集合
        List<String> redisKeys = userIds
                .stream()
                .map(RedisKeyConstants::buildUserInfoKey)
                .toList();

        // 直接传 redisKeys，它本身就是 List<String>，满足 Collection<String>
        List<String> redisValues = redisTemplate.opsForValue().multiGet(redisKeys);
        if(CollUtil.isNotEmpty(redisValues)){
            //过滤没有命中缓存的
            redisValues = redisValues.stream()
                    .filter(Objects::nonNull)
                    .toList();
        }

        //反参
        List<FindUserByIdsRespDTO> findUserByIdsRespDTOS = Lists.newArrayList();

        //将过滤后的缓存集合，转换为DTO反参实体
        if(CollUtil.isNotEmpty(redisValues)){
            findUserByIdsRespDTOS = redisValues
                    .stream()
                    .map(value-> JsonUtils.parseObject(value,FindUserByIdsRespDTO.class)
                    )
                    .collect(Collectors.toList());

        }

        //如果全部命中缓存直接返回
        if(CollUtil.size(userIds)==CollUtil.size(findUserByIdsRespDTOS)){
            return Response.success(findUserByIdsRespDTOS);
        }

        //缓存消息不全，需要去数据库中查询补全
        List<Long> userIdsNeedQuery = null;

        if(CollUtil.isNotEmpty(findUserByIdsRespDTOS)){
            Map<Long,FindUserByIdsRespDTO> map = findUserByIdsRespDTOS
                    .stream()
                    .collect(Collectors.toMap(FindUserByIdsRespDTO::getId, p -> p));

            //筛选出需要查 DB 的用户 ID
            userIdsNeedQuery = userIds.stream()
                    .filter(id->Objects.isNull(map.get(id)))
                    .toList();
        }else {
            //缓存全没命中，全部查数据库
            userIdsNeedQuery = userIds;
        }

        //数据库批量查询
        List<UserDO> userDOS = userDOMapper.selectByIds(userIdsNeedQuery);

        List<FindUserByIdsRespDTO> findUserByIdsRespDTOS1= null;
        if(CollUtil.isNotEmpty(userDOS)) {
            //数据库记录不为空
            findUserByIdsRespDTOS1 = userDOS
                    .stream()
                    .map(userDO -> FindUserByIdsRespDTO
                            .builder()
                            .id(userDO.getId())
                            .nickName(userDO.getNickname())
                            .avatar(userDO.getAvatar())
                            .introduction(userDO.getIntroduction())
                            .build())
                    .collect(Collectors.toList());

            // 异步将查询数据同步至Redis中
            List<FindUserByIdsRespDTO> findUserByIdsRespDTOS2=findUserByIdsRespDTOS1;

            redisTemplate.executePipelined(new SessionCallback<>() {
                @Override
                public Object execute(RedisOperations operations) throws DataAccessException {
                    for (FindUserByIdsRespDTO findUserByIdsRespDTO : findUserByIdsRespDTOS2) {
                        String userInfoRedisKey = RedisKeyConstants.buildUserInfoKey(findUserByIdsRespDTO.getId());
                        //DTO转JSON字符
                        String value = JsonUtils.toJsonString(findUserByIdsRespDTO);
                        long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
                        operations.opsForValue().set(userInfoRedisKey, value, expireSeconds, TimeUnit.SECONDS);
                    }
                    return null;
                }});
        }



        //合并数据
        if(CollUtil.isNotEmpty(findUserByIdsRespDTOS1)){
            findUserByIdsRespDTOS.addAll(findUserByIdsRespDTOS1);
        }
        return  Response.success(findUserByIdsRespDTOS);
    }
}
