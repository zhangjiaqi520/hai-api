package com.haiapi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.haiapi.common.constant.SystemConstant;
import com.haiapi.common.exception.BusinessException;
import com.haiapi.entity.User;
import com.haiapi.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public Page<User> listUsers(int page, int size, String role, String status) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        if (role != null && !role.isEmpty()) {
            wrapper.eq(User::getRole, role);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(User::getStatus, status);
        }

        wrapper.orderByDesc(User::getCreatedAt);

        Page<User> pageResult = new Page<>(page, size);
        return userMapper.selectPage(pageResult, wrapper);
    }

    public User getUserById(String id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw BusinessException.of(404, "用户不存在");
        }
        return user;
    }

    public User createUser(String username, String password, String email, String role) {
        if (userMapper.selectByUsername(username) != null) {
            throw BusinessException.of(400, "用户名已存在");
        }

        if (userMapper.selectByEmail(email) != null) {
            throw BusinessException.of(400, "邮箱已被注册");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setRole(role != null ? role : SystemConstant.ROLE_USER);
        user.setStatus(SystemConstant.USER_STATUS_ACTIVE);

        userMapper.insert(user);
        log.info("创建用户: {}", username);

        return user;
    }

    public User updateUser(String id, String email, String role, String status) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw BusinessException.of(404, "用户不存在");
        }

        if (email != null && !email.equals(user.getEmail())) {
            if (userMapper.selectByEmail(email) != null) {
                throw BusinessException.of(400, "邮箱已被注册");
            }
            user.setEmail(email);
        }

        if (role != null) {
            user.setRole(role);
        }

        if (status != null) {
            user.setStatus(status);
        }

        userMapper.updateById(user);
        log.info("更新用户: {}", id);

        return user;
    }

    public void deleteUser(String id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw BusinessException.of(404, "用户不存在");
        }

        userMapper.deleteById(id);
        log.info("删除用户: {}", id);
    }

    public void resetPassword(String id, String newPassword) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw BusinessException.of(404, "用户不存在");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        log.info("重置密码: {}", id);
    }

    public long countUsers() {
        return userMapper.selectCount(null);
    }

    public long countUsersByRole(String role) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRole, role);
        return userMapper.selectCount(wrapper);
    }
}
