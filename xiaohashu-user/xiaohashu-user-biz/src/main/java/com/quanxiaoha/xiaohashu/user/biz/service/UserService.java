package com.quanxiaoha.xiaohashu.user.biz.service;

import com.quanxiaoha.framework.common.response.Response;
import com.quanxiaoha.xiaohashu.user.biz.model.vo.UpdateUserInfoReqVO;
import com.quanxiaoha.xiaohashu.user.dto.req.*;
import com.quanxiaoha.xiaohashu.user.dto.resp.FindUserByIdRespDTO;
import com.quanxiaoha.xiaohashu.user.dto.resp.FindUserByIdsRespDTO;
import com.quanxiaoha.xiaohashu.user.dto.resp.FindUserByPhoneRespDTO;

import java.util.List;

public interface UserService {

    Response<?> updateUserInfo(UpdateUserInfoReqVO  updateUserInfoReqVO);
    Response<Long> register(RegisterUserReqDTO registerUserReqDTO);
    Response<FindUserByPhoneRespDTO> findUserByPhone(FindUserByPhoneReqDTO findUserByPhoneReqDTO);
    Response<?> updatePassword(UpdatePasswordReqDTO updatePasswordReqDTO);

    Response<FindUserByIdRespDTO>  findUserById(FindUserByIdReqDTO findUserByIdReqDTO);

    Response<List<FindUserByIdsRespDTO>> findByIds(FindUserByIdsReqDTO findUserByIdsReqDTO);


}
