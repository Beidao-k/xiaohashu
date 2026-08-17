package com.quanxiaoha.xiaohashu.user.relation.biz.service;

import cn.hutool.db.Page;
import com.quanxiaoha.framework.common.response.PageResponse;
import com.quanxiaoha.framework.common.response.Response;
import com.quanxiaoha.xiaohashu.user.relation.biz.model.vo.*;

import java.util.List;

public interface UserRelationService {


    Response<?> follow(FollowUserReqVO followUserReqVO);

    Response<?> unfollow(UnFollowUserReqVO unFollowUserReqVO);

    PageResponse<FindFollowingUserRspVO> findFollowingList(FindFollowingListReqVO findFollowingListReqVO);
    PageResponse<FindFansUserRspVO> findFansList(FindFansListReqVO findFansListReqVO);
}
