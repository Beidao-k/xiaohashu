package com.quanxiaoha.xiaohashu.user.api;

import com.quanxiaoha.framework.common.response.Response;
import com.quanxiaoha.xiaohashu.user.constant.ApiConstants;
import com.quanxiaoha.xiaohashu.user.dto.req.*;
import com.quanxiaoha.xiaohashu.user.dto.resp.FindUserByIdRespDTO;
import com.quanxiaoha.xiaohashu.user.dto.resp.FindUserByIdsRespDTO;
import com.quanxiaoha.xiaohashu.user.dto.resp.FindUserByPhoneRespDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = ApiConstants.SERVICE_NAME)
public interface UserFeignApi {

    String PREFIX = "/user";

    @PostMapping(value = PREFIX + "/register")
    Response<Long> registerUser(@RequestBody RegisterUserReqDTO registerUserReqDTO);

    @PostMapping(value = PREFIX + "/findByPhone")
    public Response<FindUserByPhoneRespDTO> findByPhone(@Validated @RequestBody FindUserByPhoneReqDTO findUserByPhoneReqDTO);

    @PostMapping(value = PREFIX + "/password/update")
    public Response<?> updatePassword(@Validated @RequestBody UpdatePasswordReqDTO updatePasswordReqDTO);


    @PostMapping(value = PREFIX + "/findById")
    public Response<FindUserByIdRespDTO> findUserById(@Validated @RequestBody FindUserByIdReqDTO findUserByIdReqDTO);


    @PostMapping(value = PREFIX + "/findByIds")
    public Response<List<FindUserByIdsRespDTO>> findUserByIds(@Validated @RequestBody FindUserByIdsReqDTO findUserByIdsReqDTO);

}