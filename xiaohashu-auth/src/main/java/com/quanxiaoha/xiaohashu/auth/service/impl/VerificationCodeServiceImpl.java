package com.quanxiaoha.xiaohashu.auth.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.quanxiaoha.framework.common.exception.BizException;
import com.quanxiaoha.framework.common.response.Response;
import com.quanxiaoha.xiaohashu.auth.component.MailSend;
import com.quanxiaoha.xiaohashu.auth.constant.RedisKeyConstants;
import com.quanxiaoha.xiaohashu.auth.enums.ResponseCodeEnum;
import com.quanxiaoha.xiaohashu.auth.model.vo.verificationcode.SendVerificationCodeReqVO;
import com.quanxiaoha.xiaohashu.auth.service.VerificationCodeService;
import com.quanxiaoha.xiaohashu.auth.sms.AliyunSmsHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class VerificationCodeServiceImpl implements VerificationCodeService {

    @Resource(name = "myStringRedisTemplate")
    private RedisTemplate<String, String> redisTemplate;

    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Resource
    private AliyunSmsHelper  aliyunSmsHelper;

    @Resource
    private MailSend mailSend;

    @Override
    public Response<?> sendVerificationCode(SendVerificationCodeReqVO sendVerificationCodeReqVO) {
        //获取手机号
        String phone = sendVerificationCodeReqVO.getPhone();
        //构建验证码redis key
        String key = RedisKeyConstants.buileVerificationCode(phone);


        //判断是否已经发送
        boolean isSend = redisTemplate.hasKey(key);

        //已发送验证码，提示频繁
        if(isSend){
            throw new BizException(ResponseCodeEnum.VERIFICATION_CODE_SEND_FREQUENTLY);
        }

        //生成验证码
        String verificationCode = RandomUtil.randomNumbers(6);



        //todo，调用第三方短信服务发送短信服务
        mailSend.sendCode("2237532138@qq.com",verificationCode);

        log.info("======>手机号：{}，已发送验证吗：【{}】",phone,verificationCode);
        threadPoolTaskExecutor.submit(()->{
            String signName = "速通互联验证码"; // 签名，个人测试签名无法修改
            String templateCode = "100001"; // 短信模板编码
            // 短信模板参数，code 表示要发送的验证码；min 表示验证码有时间时长，即 1 分钟
            String templateParam = String.format("{\"code\":\"%s\",\"min\":\"1\"}", verificationCode);
            aliyunSmsHelper.sendMessage(signName, templateCode, phone, templateParam);
        });

        redisTemplate.opsForValue().set(key,verificationCode, 3,TimeUnit.MINUTES);



        return Response.success();
    }
}
