package com.quanxiaoha.xiaohashu.user.biz.domain.mapper;


import com.quanxiaoha.xiaohashu.user.biz.domain.dataobject.UserRoleDO;

import java.util.List;

public interface UserRoleDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(UserRoleDO record);

    int insertSelective(UserRoleDO record);

    UserRoleDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(UserRoleDO record);

    int updateByPrimaryKey(UserRoleDO record);
    List<Long> selectRoleByUserId(Long UserId);
}