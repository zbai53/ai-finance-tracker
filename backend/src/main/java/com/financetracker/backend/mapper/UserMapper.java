package com.financetracker.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.financetracker.backend.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User>{

}
