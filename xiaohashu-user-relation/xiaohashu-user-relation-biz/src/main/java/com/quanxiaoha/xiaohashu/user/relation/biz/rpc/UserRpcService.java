package com.quanxiaoha.xiaohashu.user.relation.biz.rpc;


import com.alibaba.nacos.common.utils.StringUtils;
import com.quanxiaoha.framework.common.response.Response;
import com.quanxiaoha.xiaohashu.user.api.UserFeignApi;
import com.quanxiaoha.xiaohashu.user.dto.req.FindUserByIdReqDTO;
import com.quanxiaoha.xiaohashu.user.dto.req.FindUserByIdsReqDTO;
import com.quanxiaoha.xiaohashu.user.dto.resp.FindUserByIdRespDTO;
import com.quanxiaoha.xiaohashu.user.dto.resp.FindUserByIdsRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class UserRpcService {


    @Resource
    private UserFeignApi  userFeignApi;


    public boolean isUserExist(Long userId) {

        FindUserByIdReqDTO findUserByIdReqDTO = FindUserByIdReqDTO
                .builder()
                .userId(userId)
                .build();

        Response<FindUserByIdRespDTO> userById = userFeignApi.findUserById(findUserByIdReqDTO);

        FindUserByIdRespDTO data = userById.getData();

        if(Objects.isNull(data)){
            return false;
        }
        return true;
    }

    /**
     * 批量查询用户信息
     * @param userIds
     * @return
     */
    public List<FindUserByIdsRespDTO> findByIds(List<Long> userIds){
        FindUserByIdsReqDTO findUserByIdsReqDTO = new FindUserByIdsReqDTO();
        findUserByIdsReqDTO.setIds(userIds);

        Response<List<FindUserByIdsRespDTO>> response = userFeignApi.findUserByIds(findUserByIdsReqDTO);

        if(Objects.isNull(response)||!response.isSuccess()){
            return null;
        }
        return response.getData();
    }


}
