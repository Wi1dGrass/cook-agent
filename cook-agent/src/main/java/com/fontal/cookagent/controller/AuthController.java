package com.fontal.cookagent.controller;

import com.fontal.cookagent.dto.LoginRequest;
import com.fontal.cookagent.dto.LoginResponse;
import com.fontal.cookagent.dto.RegisterRequest;
import com.fontal.cookagent.dto.UserResponse;
import com.fontal.cookagent.security.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户认证", description = "注册/登录")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户注册", description = "默认角色 CHEF，密码 BCrypt 哈希存储")
    @PostMapping("/register")
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @Operation(summary = "用户登录", description = "校验通过后返回 JWT")
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}