package com.quanxiaoha.xiaohashu.auth.service.impl;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.google.common.base.Preconditions;
import com.quanxiaoha.framework.biz.context.holer.LoginUserContextHolder;
import com.quanxiaoha.framework.common.enums.DeletedEnum;
import com.quanxiaoha.framework.common.enums.StatusEnum;
import com.quanxiaoha.framework.common.exception.BizException;
import com.quanxiaoha.framework.common.response.Response;
import com.quanxiaoha.framework.common.util.JsonUtils;
import com.quanxiaoha.xiaohashu.auth.constant.RedisKeyConstants;
import com.quanxiaoha.xiaohashu.auth.constant.RoleConstants;

import com.quanxiaoha.xiaohashu.auth.enums.LoginTypeEnum;
import com.quanxiaoha.xiaohashu.auth.enums.ResponseCodeEnum;
import com.quanxiaoha.xiaohashu.auth.model.vo.user.UpdatePasswordReqVO;
import com.quanxiaoha.xiaohashu.auth.model.vo.user.UserLoginReqVO;
import com.quanxiaoha.xiaohashu.auth.rpc.UserRpcService;
import com.quanxiaoha.xiaohashu.auth.service.AuthService;
import com.quanxiaoha.xiaohashu.user.dto.resp.FindUserByPhoneRespDTO;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import java.util.Objects;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {


    @Resource
    private UserRpcService  userRpcService;

    @Resource(name = "myStringRedisTemplate")
    private RedisTemplate<String, String> redisTemplate;


    @Resource
    LoginUserContextHolder loginUserContexHolder;

    @Resource
    private PasswordEncoder passwordEncoder;





    Long userId=null;


    /**
     * 登录或在注册
     * @param userLoginReqVO
     * @return
     */
    @Override
    public Response<String> loginAndRegister(UserLoginReqVO userLoginReqVO) {
        String phone = userLoginReqVO.getPhone();
        Integer type = userLoginReqVO.getType();
        LoginTypeEnum loginTypeEnum = LoginTypeEnum.valueOf(type);
        if (Objects.isNull(loginTypeEnum)) {
            throw new BizException(ResponseCodeEnum.LOGIN_TYPE_ERROR);
        }

        switch (loginTypeEnum) {
            case VERIFICATION_CODE ->  {
                //验证码登录

                //判断验证码是否为空
                String verificationCode=userLoginReqVO.getCode();
                Preconditions.checkArgument(StringUtils.isNotBlank(verificationCode),"验证码不能为空");

                String key = RedisKeyConstants.buileVerificationCode(phone);
                String sendCode = redisTemplate.opsForValue().get(key);

                //判断验证码是否过期
                Preconditions.checkArgument(StringUtils.isNotBlank(sendCode),"验证码过期");

                //判断验证码是否正确
                if(!sendCode.equals(verificationCode)){
                    return Response.fail(ResponseCodeEnum.VERIFICATION_CODE_ERROR);
                }


                FindUserByPhoneRespDTO findUserByPhoneRespDTO = userRpcService.findByPhone(phone);
                //未注册
                if(Objects.isNull(findUserByPhoneRespDTO)){
                    log.info("未注册");
                    Long userIdTemp = userRpcService.registerUser(phone);
                    if(Objects.isNull(userIdTemp)){
                        throw new BizException(ResponseCodeEnum.LOGIN_FAIL);
                    }
                    userId = userIdTemp;

                }else {

                   //已注册获取id
                    userId = findUserByPhoneRespDTO.getId();
                }

            }
            //====================  密码登录
            case PASSWORD ->  {
                String password = userLoginReqVO.getPassword();

                //查询用户
                FindUserByPhoneRespDTO findUserByPhoneRespDTO = userRpcService.findByPhone(phone);

                if(Objects.isNull(findUserByPhoneRespDTO)){
                    throw new BizException(ResponseCodeEnum.USER_NOT_FOUND);
                }

                String encodePassword =  findUserByPhoneRespDTO.getPassword();


                //账号或密码不正确
                boolean isPasswordCorrect = passwordEncoder.matches(password,encodePassword);
                if(!isPasswordCorrect){
                    throw  new BizException(ResponseCodeEnum.PHONE_OR_PASSWORD_ERROR);
                }
                userId = findUserByPhoneRespDTO.getId();

            }

        }


        //todo
        StpUtil.login(userId);
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        //返回token
       return Response.success(tokenInfo.tokenValue);
    }

    /**
     * 退出登录
     * @param
     * @return
     */

    @Override
    public Response<?> logout() {
        Long userId = loginUserContexHolder.getUserId();
        StpUtil.logout(userId);
        return Response.success();
    }

    @Override
    public Response<?> updatePassword(UpdatePasswordReqVO updatePasswordReqVO) {
        String encodePassword = passwordEncoder.encode(updatePasswordReqVO.getNewPassword());
        updatePasswordReqVO.setNewPassword(encodePassword);
        userRpcService.updatePassword(updatePasswordReqVO.getNewPassword());
        return Response.success();

    }




}
