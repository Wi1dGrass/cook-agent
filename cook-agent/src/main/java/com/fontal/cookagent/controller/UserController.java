package com.fontal.cookagent.controller;

import com.fontal.cookagent.dto.RecipeSummaryResponse;
import com.fontal.cookagent.dto.SessionSummary;
import com.fontal.cookagent.entity.ChatHistory;
import com.fontal.cookagent.security.CurrentUser;
import com.fontal.cookagent.service.FavoriteService;
import com.fontal.cookagent.service.HistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户中心 — 收藏与查询历史。
 */
@Tag(name = "用户中心", description = "用户收藏 / 查询历史 / 会话列表")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final FavoriteService favoriteService;
    private final HistoryService historyService;

    // ==================== 收藏 ====================

    @Operation(summary = "列出当前用户的收藏菜品")
    @GetMapping("/favorites")
    public List<RecipeSummaryResponse> listFavorites() {
        return favoriteService.listFavorites(CurrentUser.requireUserId());
    }

    @Operation(summary = "添加收藏菜品")
    @PostMapping("/favorites/{recipeId}")
    public Map<String, Object> addFavorite(@PathVariable Long recipeId) {
        favoriteService.addFavorite(CurrentUser.requireUserId(), recipeId);
        return Map.of("recipeId", recipeId, "favorited", true);
    }

    @Operation(summary = "取消收藏菜品")
    @DeleteMapping("/favorites/{recipeId}")
    public Map<String, Object> removeFavorite(@PathVariable Long recipeId) {
        favoriteService.removeFavorite(CurrentUser.requireUserId(), recipeId);
        return Map.of("recipeId", recipeId, "favorited", false);
    }

    // ==================== 查询历史 ====================

    @Operation(summary = "列出当前用户的查询历史", description = "倒序最近 limit 条（最多50）")
    @GetMapping("/history")
    public List<ChatHistory> listHistory(@RequestParam(defaultValue = "20") int limit) {
        return historyService.listHistory(CurrentUser.requireUserId(), limit);
    }

    @Operation(summary = "按对话ID列出完整历史")
    @GetMapping("/history/{conversationId}")
    public List<ChatHistory> historyByConversation(@PathVariable String conversationId) {
        return historyService.listByConversation(CurrentUser.requireUserId(), conversationId);
    }

    @Operation(summary = "删除某对话完整历史")
    @DeleteMapping("/history/{conversationId}")
    public Map<String, Object> deleteHistory(@PathVariable String conversationId) {
        historyService.deleteByConversation(CurrentUser.requireUserId(), conversationId);
        return Map.of("conversationId", conversationId, "deleted", true);
    }

    // ==================== 会话列表（侧边栏 / 历史页） ====================

    @Operation(summary = "列出当前用户的所有会话", description = "服务端按 conversation_id 分组，返回会话摘要列表（标题/消息数/最后时间/来源），倒序")
    @GetMapping("/sessions")
    public List<SessionSummary> listSessions() {
        return historyService.listSessions(CurrentUser.requireUserId());
    }
}