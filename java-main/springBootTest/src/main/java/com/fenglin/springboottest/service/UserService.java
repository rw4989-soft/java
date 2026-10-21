package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fenglin.springboottest.entity.User;

/**
 * 用户 Service 接口
 */
public interface UserService extends IService<User> {

    /**
     * 登录校验
     * @return 登录成功返回用户实体（含昵称），失败返回 null
     */
    User login(String username, String password);

    /**
     * 注册新用户
     * @return 注册成功返回用户实体；用户名已存在返回 null
     */
    User register(String username, String password, String nickname);
}
