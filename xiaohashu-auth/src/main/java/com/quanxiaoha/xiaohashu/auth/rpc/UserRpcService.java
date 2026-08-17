package com.quanxiaoha.xiaohashu.auth.rpc;

import com.quanxiaoha.framework.common.response.Response;
import com.quanxiaoha.xiaohashu.user.api.UserFeignApi;
import com.quanxiaoha.xiaohashu.user.dto.req.FindUserByPhoneReqDTO;
import com.quanxiaoha.xiaohashu.user.dto.req.RegisterUserReqDTO;
import com.quanxiaoha.xiaohashu.user.dto.req.UpdatePasswordReqDTO;
import com.quanxiaoha.xiaohashu.user.dto.resp.FindUserByPhoneRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

@Component
public class UserRpcService {

    @Resource
    private UserFeignApi userFeignApi;


    public Long registerUser(String phone){
        RegisterUserReqDTO registerUserReqDTO = new RegisterUserReqDTO();
        registerUserReqDTO.setPhone(phone);

        Response<Long> response = userFeignApi.registerUser(registerUserReqDTO);
        if(!response.isSuccess()){
            return null;
        }
        return response.getData();

    }


    /**
     * 根据手机号查询用户id,psd;
     * @param phone
     * @return
     */
    public FindUserByPhoneRespDTO findByPhone(String phone){
        FindUserByPhoneReqDTO findByPhoneReqDTO = FindUserByPhoneReqDTO
                .builder()
                .phone(phone)
                .build();
        Response<FindUserByPhoneRespDTO> response = userFeignApi.findByPhone(findByPhoneReqDTO);
        if(!response.isSuccess()){
            return null;
        }
        return response.getData();

    }

    public void updatePassword(String encodePassword){
        UpdatePasswordReqDTO updatePasswordReqDTO = new UpdatePasswordReqDTO(encodePassword);
        userFeignApi.updatePassword(updatePasswordReqDTO);
        return;

    }
}
