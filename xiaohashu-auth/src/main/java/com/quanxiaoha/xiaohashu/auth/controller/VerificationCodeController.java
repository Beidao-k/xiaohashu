package com.quanxiaoha.xiaohashu.auth.controller;

import com.quanxiaoha.framework.biz.operationlog.aspect.ApiOperationLog;
import com.quanxiaoha.framework.common.response.Response;
import com.quanxiaoha.xiaohashu.auth.model.vo.verificationcode.SendVerificationCodeReqVO;
import com.quanxiaoha.xiaohashu.auth.service.VerificationCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@RequestMapping("/verification/code")
public class VerificationCodeController {

    @Autowired
    VerificationCodeService verificationCodeService;


    @PostMapping("/send")
    @ApiOperationLog(description = "发送短信验证码")
    public Response<?> sendCode(@Validated @RequestBody SendVerificationCodeReqVO sendVerificationCodeReqVO){
       log.info("=====================号码{}",sendVerificationCodeReqVO.getPhone());
        return verificationCodeService.sendVerificationCode(sendVerificationCodeReqVO);
    }
}
