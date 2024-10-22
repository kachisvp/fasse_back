package com.example.fasse_back.user.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.example.fasse_back.user.mapper.UserMapper;
import com.example.fasse_back.user.model.User;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final UserMapper userMapper;

    public List<User> findAll() {
        return userMapper.findAll();
    }

    public User get(Integer id) {
        return userMapper.get(id);
    }

    public int create(User user) {
        return userMapper.insert(user);
    }

    public int update(User user) {
        return userMapper.update(user);
    }

    public int delete(Integer id) {
        return userMapper.delete(id);
    }
}
