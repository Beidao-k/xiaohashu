package com.quanxiaoha.xiaohashu.note.biz.rpc;

import com.quanxiaoha.framework.common.exception.BizException;
import com.quanxiaoha.framework.common.response.Response;
import com.quanxiaoha.xiaohashu.note.biz.enums.ResponseCodeEnum;
import com.quanxiaoha.xiaohashu.user.api.UserFeignApi;
import com.quanxiaoha.xiaohashu.user.dto.req.FindUserByIdReqDTO;
import com.quanxiaoha.xiaohashu.user.dto.resp.FindUserByIdRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class UserRpcService {


    @Resource
    UserFeignApi userFeignApi;
    public FindUserByIdRespDTO findUserByIdRespDTO(Long userId){
        FindUserByIdReqDTO reqDTO = FindUserByIdReqDTO.builder().userId(userId).build();
        Response<FindUserByIdRespDTO> userById = userFeignApi.findUserById(reqDTO);
        if(Objects.isNull(userById)||!userById.isSuccess()){
            throw  new BizException(ResponseCodeEnum.FIND_NOTE_BY_ID);
        }
        return userById.getData();
    }

}
