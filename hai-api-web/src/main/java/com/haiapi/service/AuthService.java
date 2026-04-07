package com.haiapi.service;

import com.haiapi.common.constant.SystemConstant;
import com.haiapi.common.exception.BusinessException;
import com.haiapi.entity.User;
import com.haiapi.mapper.UserMapper;
import com.haiapi.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public Map<String, Object> login(String username, String password) {
        log.info("用户登录: {}", username);

        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw BusinessException.of(401, "用户名或密码错误");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw BusinessException.of(401, "用户名或密码错误");
        }

        if (!SystemConstant.USER_STATUS_ACTIVE.equals(user.getStatus())) {
            throw BusinessException.of(403, "账号已被禁用");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("token", token);
        result.put("user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "role", user.getRole()
        ));

        log.info("用户登录成功: {}", username);
        return result;
    }

    public User register(String username, String password, String email) {
        log.info("用户注册: {}", username);

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
        user.setRole(SystemConstant.ROLE_USER);
        user.setStatus(SystemConstant.USER_STATUS_ACTIVE);

        userMapper.insert(user);

        log.info("用户注册成功: {}", username);
        return user;
    }

    public User getCurrentUser(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw BusinessException.of(401, "未授权");
        }

        token = token.substring(7);

        if (!jwtUtil.validateToken(token)) {
            throw BusinessException.of(401, "Token已过期");
        }

        String userId = jwtUtil.getUserIdFromToken(token);
        User user = userMapper.selectById(userId);

        if (user == null) {
            throw BusinessException.of(401, "用户不存在");
        }

        return user;
    }

    public void changePassword(String userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.of(404, "用户不存在");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw BusinessException.of(400, "原密码错误");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);

        log.info("密码修改成功: {}", user.getUsername());
    }
}
