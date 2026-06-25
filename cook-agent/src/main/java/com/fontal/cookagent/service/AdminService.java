package com.fontal.cookagent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fontal.cookagent.common.BizException;
import com.fontal.cookagent.common.ErrorCode;
import com.fontal.cookagent.dto.PageResult;
import com.fontal.cookagent.dto.UserResponse;
import com.fontal.cookagent.entity.User;
import com.fontal.cookagent.mapper.UserMapper;
import com.fontal.cookagent.rag.etl.CookEtlPipeline;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 管理后台服务 — 用户管理、ETL 触发、向量索引重建。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserMapper userMapper;
    private final CookEtlPipeline etlPipeline;
    private final PasswordEncoder passwordEncoder;

    // ========== ETL ==========

    /** 触发增量导入（直接 add 与现有数据合并） */
    public String sync() {
        log.info("管理后台触发 ETL 同步");
        long start = System.currentTimeMillis();
        etlPipeline.run(false);
        long cost = System.currentTimeMillis() - start;
        return "ETL 同步完成，耗时 " + cost + " ms";
    }

    /** 重建向量索引：清空 → 重新全量导入 */
    public String rebuildIndex() {
        log.info("管理后台触发向量索引重建");
        long start = System.currentTimeMillis();
        etlPipeline.run(true);
        long cost = System.currentTimeMillis() - start;
        return "向量索引重建完成，耗时 " + cost + " ms";
    }

    // ========== 用户管理 ==========

    public PageResult<UserResponse> pageUsers(int pageNum, int pageSize, String keyword) {
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .orderByDesc(User::getCreatedAt);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(User::getUsername, keyword);
        }
        IPage<User> result = userMapper.selectPage(page, wrapper);
        List<UserResponse> records = result.getRecords().stream()
                .map(UserResponse::from).toList();
        return PageResult.from(result, records);
    }

    public UserResponse getUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户 ID " + id + " 不存在");
        }
        return UserResponse.from(user);
    }

    public void toggleEnabled(Long id, Integer enabled) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户 ID " + id + " 不存在");
        }
        user.setEnabled(enabled);
        userMapper.updateById(user);
        log.info("管理员更新用户启用状态: id={}, enabled={}", id, enabled);
    }

    public void updateRole(Long id, String role) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户 ID " + id + " 不存在");
        }
        user.setRole(role);
        userMapper.updateById(user);
        log.info("管理员更新用户角色: id={}, role={}", id, role);
    }

    public void changePassword(Long id, String newPassword) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户 ID " + id + " 不存在");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        log.info("管理员重置用户密码: id={}", id);
    }
}