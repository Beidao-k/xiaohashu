package com.quanxiaoha.xiaohashu.user.biz.controller;

import com.quanxiaoha.framework.biz.operationlog.aspect.ApiOperationLog;
import com.quanxiaoha.framework.common.response.Response;
import com.quanxiaoha.xiaohashu.user.biz.model.vo.UpdateUserInfoReqVO;
import com.quanxiaoha.xiaohashu.user.biz.service.UserService;
import com.quanxiaoha.xiaohashu.user.dto.req.*;
import com.quanxiaoha.xiaohashu.user.dto.resp.FindUserByIdRespDTO;
import com.quanxiaoha.xiaohashu.user.dto.resp.FindUserByIdsRespDTO;
import com.quanxiaoha.xiaohashu.user.dto.resp.FindUserByPhoneRespDTO;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    UserService userService;


    @ApiOperationLog(description = "修改用用户信息")
    @PostMapping(value = "/update",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Response<?> updateUserInfo(@Validated  UpdateUserInfoReqVO updateUserInfoReqVO){
        return  userService.updateUserInfo(updateUserInfoReqVO);
    }

    @ApiOperationLog(description = "用户注册")
    @PostMapping( "/register")
    public Response<Long> register(@Validated @RequestBody RegisterUserReqDTO registerUserReqDTO){
        return userService.register(registerUserReqDTO);
    }

    @ApiOperationLog(description = "根据手机号查询用户id,密码")
    @PostMapping("/findByPhone")
    public Response<FindUserByPhoneRespDTO> findByPhone(@Validated @RequestBody FindUserByPhoneReqDTO findUserByPhoneReqDTO){
        return userService.findUserByPhone(findUserByPhoneReqDTO);
    }

    @ApiOperationLog(description = "更新密码")
    @PostMapping("/password/update")
    public Response<?>  updatePassword(@Validated @RequestBody UpdatePasswordReqDTO updatePasswordReqDTO){
        return userService.updatePassword(updatePasswordReqDTO);
    }

    @ApiOperationLog(description = "根据用户id查询用户基础信息")
    @PostMapping("/findById")
    public Response<FindUserByIdRespDTO> findUserById(@Validated @RequestBody FindUserByIdReqDTO findUserByIdReqDTO){
        return userService.findUserById(findUserByIdReqDTO);
    }

    @ApiOperationLog(description = "批量查询用户信息")
    @PostMapping("/findByIds")
    public Response<List<FindUserByIdsRespDTO>> findUserByIds(@Validated @RequestBody FindUserByIdsReqDTO findUserByIdsReqDTO){
        return userService.findByIds(findUserByIdsReqDTO);
    }


}
