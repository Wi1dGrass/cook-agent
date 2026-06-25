package com.fontal.cookagent.controller;

import com.fontal.cookagent.config.RateLimit;
import com.fontal.cookagent.dto.PageResult;
import com.fontal.cookagent.dto.UserResponse;
import com.fontal.cookagent.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理后台 — 仅 ADMIN 角色可访问（已由 SecurityConfig 限制路径 /admin/**）。
 */
@Tag(name = "管理后台", description = "ETL 触发 / 索引重建 / 用户管理")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ========== ETL ==========

    @Operation(summary = "触发 ETL 同步", description = "增量导入 CookLikeHOC 菜谱到向量库")
    @RateLimit(limit = 2)
    @PostMapping("/etl/sync")
    public Map<String, Object> sync() {
        return Map.of("message", adminService.sync());
    }

    @Operation(summary = "重建向量索引", description = "清空向量库后做全量重新导入")
    @RateLimit(limit = 1)
    @PostMapping("/etl/rebuild-index")
    public Map<String, Object> rebuildIndex() {
        return Map.of("message", adminService.rebuildIndex());
    }

    // ========== 用户管理 ==========

    @Operation(summary = "分页查询用户")
    @GetMapping("/users")
    public PageResult<UserResponse> pageUsers(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        return adminService.pageUsers(pageNum, pageSize, keyword);
    }

    @Operation(summary = "查询用户详情")
    @GetMapping("/users/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        return adminService.getUser(id);
    }

    @Operation(summary = "切换用户启用状态")
    @PutMapping("/users/{id}/enabled")
    public Map<String, Object> toggleEnabled(@PathVariable Long id, @RequestParam Integer enabled) {
        adminService.toggleEnabled(id, enabled);
        return Map.of("id", id, "enabled", enabled);
    }

    @Operation(summary = "更新用户角色")
    @PutMapping("/users/{id}/role")
    public Map<String, Object> updateRole(@PathVariable Long id, @RequestParam String role) {
        adminService.updateRole(id, role);
        return Map.of("id", id, "role", role);
    }

    @Operation(summary = "重置用户密码")
    @PutMapping("/users/{id}/password")
    public Map<String, Object> changePassword(@PathVariable Long id, @RequestParam String newPassword) {
        adminService.changePassword(id, newPassword);
        return Map.of("id", id, "updated", true);
    }
}