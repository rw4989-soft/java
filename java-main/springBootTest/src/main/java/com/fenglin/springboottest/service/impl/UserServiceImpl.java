package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户 Service 实现
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public User login(String username, String password) {
        User user = lambdaQuery()
                .eq(User::getUsername, username)
                .one();
        // 比对 BCrypt 哈希（不直接 equals 明文）
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return null;
        }
        return user;
    }

    @Override
    public User register(String username, String password, String nickname) {
        // 检查用户名是否已存在
        Long count = lambdaQuery()
                .eq(User::getUsername, username)
                .count();
        if (count != null && count > 0) {
            return null;
        }

        User user = new User();
        user.setUsername(username);
        // 密码加密存储（BCrypt）
        user.setPassword(passwordEncoder.encode(password));
        // 昵称为空时默认用用户名
        user.setNickname(nickname == null || nickname.isBlank() ? username : nickname);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        save(user);
        return user;
    }
}
