package com.quanxiaoha.xiaohashu.user.biz.rpc;


import com.quanxiaoha.xiaohashu.distributed.id.generator.api.DistributedIdGeneratorFeignApi;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DistributedIdGeneratorRpcService {

    private static final String BIZ_TAG_XIAOHASHU_ID = "leaf-segment-xiaohashu-id";
    private static final String BIZ_TAG_USER_ID = "leaf-segment-user-id";

    @Resource
    DistributedIdGeneratorFeignApi distributedIdGeneratorFeignApi;

    public String getXiaohashuId(){
        return distributedIdGeneratorFeignApi.getSegmentId(BIZ_TAG_XIAOHASHU_ID);
    }

    public String getUserId(){
        return  distributedIdGeneratorFeignApi.getSegmentId(BIZ_TAG_USER_ID);
    }
}
