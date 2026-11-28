package com.cbooking.campusbooking.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cbooking.campusbooking.user.pojo.UserEntity;
import org.apache.catalina.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {

}
