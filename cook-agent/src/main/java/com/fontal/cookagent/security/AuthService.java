package com.fontal.cookagent.security;

import com.fontal.cookagent.common.BizException;
import com.fontal.cookagent.common.ErrorCode;
import com.fontal.cookagent.dto.LoginRequest;
import com.fontal.cookagent.dto.LoginResponse;
import com.fontal.cookagent.dto.RegisterRequest;
import com.fontal.cookagent.dto.UserResponse;
import com.fontal.cookagent.entity.User;
import com.fontal.cookagent.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户鉴权服务 — 注册/登录。
 *
 * <p>密码使用 Spring Security {@link PasswordEncoder}（BCrypt）哈希存储；登录成功颁发 JWT。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /** 注册新用户，默认角色 CHEF */
    public UserResponse register(RegisterRequest request) {
        Long exist = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.username()));
        if (exist > 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "用户名「" + request.username() + "」已存在");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.CHEF.name());
        user.setPhone(request.phone());
        user.setEnabled(1);
        userMapper.insert(user);
        log.info("用户注册成功: id={}, username={}", user.getId(), user.getUsername());
        return UserResponse.from(user);
    }

    /** 登录，返回 JWT */
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.username()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "用户名或密码错误");
        }
        if (user.getEnabled() == null || user.getEnabled() == 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "账户已被禁用");
        }
        String token = jwtUtil.generate(user.getId(), user.getUsername(), user.getRole());
        log.info("用户登录成功: id={}, username={}", user.getId(), user.getUsername());
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getRole());
    }
}