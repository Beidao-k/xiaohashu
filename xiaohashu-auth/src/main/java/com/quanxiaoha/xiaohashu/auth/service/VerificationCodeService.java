package com.quanxiaoha.xiaohashu.auth.service;

import com.quanxiaoha.framework.common.response.Response;
import com.quanxiaoha.xiaohashu.auth.model.vo.verificationcode.SendVerificationCodeReqVO;
import org.springframework.stereotype.Service;


public interface VerificationCodeService {

    Response<?> sendVerificationCode(SendVerificationCodeReqVO sendVerificationCodeReqVO);
}
