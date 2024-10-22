package com.example.fasse_back.user.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.fasse_back.user.model.User;

@Mapper
public interface UserMapper {
    List<User> findAll();
    User get(@Param("id") Integer id);
    int insert(@Param("user") User user);
    int update(@Param("user") User user);
    int delete(@Param("id") Integer id);
}
