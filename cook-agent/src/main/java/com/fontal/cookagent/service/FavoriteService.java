package com.fontal.cookagent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fontal.cookagent.common.BizException;
import com.fontal.cookagent.common.ErrorCode;
import com.fontal.cookagent.dto.RecipeSummaryResponse;
import com.fontal.cookagent.entity.Recipe;
import com.fontal.cookagent.entity.UserFavorite;
import com.fontal.cookagent.mapper.RecipeMapper;
import com.fontal.cookagent.mapper.UserFavoriteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户收藏服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final UserFavoriteMapper userFavoriteMapper;
    private final RecipeMapper recipeMapper;

    /** 添加收藏 */
    @Transactional
    public void addFavorite(Long userId, Long recipeId) {
        Recipe recipe = recipeMapper.selectById(recipeId);
        if (recipe == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "菜品 ID " + recipeId + " 不存在");
        }
        Long exist = userFavoriteMapper.selectCount(
                new LambdaQueryWrapper<UserFavorite>()
                        .eq(UserFavorite::getUserId, userId)
                        .eq(UserFavorite::getRecipeId, recipeId));
        if (exist > 0) {
            log.info("已存在收藏: userId={}, recipeId={}", userId, recipeId);
            return;
        }
        UserFavorite fav = new UserFavorite();
        fav.setUserId(userId);
        fav.setRecipeId(recipeId);
        userFavoriteMapper.insert(fav);
        log.info("添加收藏: userId={}, recipeId={}", userId, recipeId);
    }

    /** 取消收藏 */
    @Transactional
    public void removeFavorite(Long userId, Long recipeId) {
        userFavoriteMapper.delete(
                new LambdaQueryWrapper<UserFavorite>()
                        .eq(UserFavorite::getUserId, userId)
                        .eq(UserFavorite::getRecipeId, recipeId));
        log.info("取消收藏: userId={}, recipeId={}", userId, recipeId);
    }

    /** 列出当前用户的所有收藏菜品 */
    public List<RecipeSummaryResponse> listFavorites(Long userId) {
        List<UserFavorite> favorites = userFavoriteMapper.selectList(
                new LambdaQueryWrapper<UserFavorite>()
                        .eq(UserFavorite::getUserId, userId)
                        .orderByDesc(UserFavorite::getCreatedAt));
        if (favorites.isEmpty()) {
            return List.of();
        }
        List<Long> recipeIds = favorites.stream().map(UserFavorite::getRecipeId).toList();
        Map<Long, Recipe> recipeMap = recipeMapper.selectBatchIds(recipeIds).stream()
                .collect(Collectors.toMap(Recipe::getId, r -> r));
        // 保持收藏顺序
        return favorites.stream()
                .map(f -> recipeMap.get(f.getRecipeId()))
                .filter(java.util.Objects::nonNull)
                .map(RecipeSummaryResponse::from)
                .toList();
    }
}