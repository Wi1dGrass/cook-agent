package com.fontal.cookagent.config;

import com.fontal.cookagent.security.JwtAuthFilter;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置 — 无状态 JWT + RBAC 角色控制。
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // SSE / Async 完成时 Tomcat 的 async dispatch 放行（避免已提交响应后 AuthorizationFilter 拒绝）
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                        // Knife4j / Swagger 资源放行
                        .requestMatchers(
                                "/doc.html", "/swagger-ui/**", "/v3/api-docs/**",
                                "/webjars/**", "/favicon.ico", "/error").permitAll()
                        // 健康检查 + 静态图片资源放行
                        .requestMatchers("/health", "/health/**", "/images/**").permitAll()
                        // 公开端点：登录/注册
                        .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login").permitAll()
                        // 所有 GET 查询端点放行（菜谱检索等公开服务）
                        .requestMatchers(HttpMethod.GET, "/recipes/**", "/categories/**",
                                "/ingredients/**").permitAll()
                        // 对话型 POST 端点公开（chat/new, chat/send 可匿名使用）
                        .requestMatchers(HttpMethod.POST, "/chat/new", "/chat/send").permitAll()
                        // Agent 对话 + 流式 + 会话管理（需登录，绑定用户身份实现多轮）
                        .requestMatchers("/agent/**").hasAnyRole("CHEF", "MANAGER", "ADMIN")
                        // 写操作需要登录（CHEF+ 以上）
                        .requestMatchers(HttpMethod.POST, "/recipes").hasAnyRole("CHEF", "MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/recipes/**").hasAnyRole("CHEF", "MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/recipes/**").hasAnyRole("MANAGER", "ADMIN")
                        // 管理后台仅 ADMIN
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // 用户中心（收藏/历史）需登录
                        .requestMatchers("/user/**").hasAnyRole("CHEF", "MANAGER", "ADMIN")
                        // 其余请求需要登录
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}