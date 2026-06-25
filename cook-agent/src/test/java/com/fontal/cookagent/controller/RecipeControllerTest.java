package com.fontal.cookagent.controller;

import com.fontal.cookagent.config.RateLimitInterceptor;
import com.fontal.cookagent.dto.CategoryResponse;
import com.fontal.cookagent.dto.CreateRecipeRequest;
import com.fontal.cookagent.dto.PageResult;
import com.fontal.cookagent.dto.RecipeDetailResponse;
import com.fontal.cookagent.dto.RecipeSummaryResponse;
import com.fontal.cookagent.dto.UpdateRecipeRequest;
import com.fontal.cookagent.service.RecipeService;
import com.fontal.cookagent.service.RecipeService.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * RecipeController 单元测试 — 覆盖所有 REST 端点和校验逻辑。
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("pgvector")
@TestPropertySource(properties = {
        "DEEPSEEK_API_KEY=test-key",
        "ZHIPU_API_KEY=test-key",
        "PEXELS_API_KEY=test-key",
        "VOLCANO_SEARCH_API_KEY=test-key"
})
class RecipeControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private EmbeddingModel embeddingModel;
    @MockitoBean private VectorStore vectorStore;
    @MockitoBean private RecipeService recipeService;
    @MockitoBean private RateLimitInterceptor rateLimitInterceptor;

    @BeforeEach
    void setUp() throws Exception {
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    private static RecipeSummaryResponse sampleRecipe() {
        return new RecipeSummaryResponse(1L, "红烧肉", 1L, "东坡肉",
                null, "经典红烧肉，肥而不腻", LocalDateTime.now());
    }

    private static RecipeDetailResponse sampleDetail() {
        return new RecipeDetailResponse(1L, "红烧肉", 1L, "东坡肉", null,
                "经典红烧肉", null, null, "# 红烧肉\n...", "红烧肉.md",
                List.of(new RecipeDetailResponse.IngredientInfo(1L, "五花肉", null, "500g", null)),
                List.of(new RecipeDetailResponse.StepInfo(1, "五花肉切块焯水")),
                LocalDateTime.now());
    }

    private static CategoryResponse sampleCategory() {
        return new CategoryResponse(1L, "炒菜", "炒菜", 1, 25L);
    }

    // ==================== 搜索 GET /recipes/search ====================

    @Nested
    @DisplayName("搜索 GET /recipes/search")
    class SearchTest {
        @Test
        @DisplayName("有效关键词返回搜索结果")
        void validKeyword() throws Exception {
            when(recipeService.search(eq("红烧肉"), eq(5), isNull()))
                    .thenReturn(List.of(sampleRecipe()));

            mockMvc.perform(get("/recipes/search").param("keyword", "红烧肉"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(1))
                    .andExpect(jsonPath("$.recipes[0].name").value("红烧肉"));
        }

        @Test
        @DisplayName("空关键词返回 400")
        void emptyKeyword() throws Exception {
            mockMvc.perform(get("/recipes/search").param("keyword", "   "))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("PARAM_INVALID"));
        }

        @Test
        @DisplayName("缺少 keyword 参数返回 400")
        void missingKeyword() throws Exception {
            mockMvc.perform(get("/recipes/search"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("PARAM_MISSING"));
        }
    }

    // ==================== 推荐 GET /recipes/recommend ====================

    @Nested
    @DisplayName("推荐 GET /recipes/recommend")
    class RecommendTest {
        @Test
        @DisplayName("有效条件返回推荐结果")
        void validCriteria() throws Exception {
            when(recipeService.recommend(eq("春天的清淡菜"), eq(3), isNull()))
                    .thenReturn(List.of(sampleRecipe()));

            mockMvc.perform(get("/recipes/recommend").param("criteria", "春天的清淡菜"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(1));
        }

        @Test
        @DisplayName("空条件返回 400")
        void emptyCriteria() throws Exception {
            mockMvc.perform(get("/recipes/recommend").param("criteria", ""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("PARAM_INVALID"));
        }
    }

    // ==================== 菜谱详情 ====================

    @Nested
    @DisplayName("菜谱详情 GET /recipes/{id}")
    class GetByIdTest {
        @Test
        @DisplayName("有效ID返回菜谱")
        void validId() throws Exception {
            when(recipeService.getById(1L)).thenReturn(sampleRecipe());

            mockMvc.perform(get("/recipes/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("红烧肉"));
        }

        @Test
        @DisplayName("不存在的ID返回 400 + NOT_FOUND")
        void notFound() throws Exception {
            when(recipeService.getById(999L)).thenReturn(null);

            mockMvc.perform(get("/recipes/999"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("非数字ID返回 400")
        void invalidId() throws Exception {
            mockMvc.perform(get("/recipes/abc"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("菜谱详情 GET /recipes/{id}/detail")
    class GetDetailTest {
        @Test
        @DisplayName("有效ID返回完整详情")
        void validId() throws Exception {
            when(recipeService.getDetail(1L)).thenReturn(sampleDetail());

            mockMvc.perform(get("/recipes/1/detail"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("红烧肉"))
                    .andExpect(jsonPath("$.ingredients[0].name").value("五花肉"))
                    .andExpect(jsonPath("$.steps[0].description").value("五花肉切块焯水"));
        }

        @Test
        @DisplayName("不存在的ID返回 400 + NOT_FOUND")
        void notFound() throws Exception {
            when(recipeService.getDetail(999L)).thenReturn(null);

            mockMvc.perform(get("/recipes/999/detail"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }
    }

    // ==================== 分类 ====================

    @Nested
    @DisplayName("分类列表 GET /categories")
    class CategoriesTest {
        @Test
        @DisplayName("返回分类列表")
        void listCategories() throws Exception {
            when(recipeService.listCategories()).thenReturn(List.of(sampleCategory()));

            mockMvc.perform(get("/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("炒菜"))
                    .andExpect(jsonPath("$[0].recipeCount").value(25));
        }
    }

    // ==================== 食材反查 ====================

    @Nested
    @DisplayName("食材反查 GET /ingredients/search")
    class IngredientsSearchTest {
        @Test
        @DisplayName("有效食材返回匹配结果")
        void validIngredients() throws Exception {
            IngredientMatchResult result = new IngredientMatchResult(sampleRecipe(), List.of("鸡腿"));
            when(recipeService.searchByIngredients(eq(List.of("鸡腿")), eq(false)))
                    .thenReturn(List.of(result));

            mockMvc.perform(get("/ingredients/search").param("names", "鸡腿"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(1))
                    .andExpect(jsonPath("$.results[0].recipe.name").value("红烧肉"))
                    .andExpect(jsonPath("$.results[0].matchedIngredients[0]").value("鸡腿"));
        }

        @Test
        @DisplayName("空食材列表返回 400")
        void emptyIngredients() throws Exception {
            mockMvc.perform(get("/ingredients/search").param("names", ","))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("PARAM_INVALID"));
        }
    }

    // ==================== 菜谱对比 ====================

    @Nested
    @DisplayName("菜谱对比 GET /recipes/compare")
    class CompareTest {
        @Test
        @DisplayName("两个菜品返回对比结果")
        void validCompare() throws Exception {
            CompareResult compareResult = new CompareResult(
                    List.of(sampleRecipe()),
                    List.of(),
                    Map.of("红烧肉", List.of("五花肉"))
            );
            when(recipeService.compareRecipes(eq(List.of("红烧肉", "口水鸡"))))
                    .thenReturn(compareResult);

            mockMvc.perform(get("/recipes/compare").param("names", "红烧肉,口水鸡"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.recipes[0].name").value("红烧肉"));
        }

        @Test
        @DisplayName("少于两个菜品返回 400")
        void lessThanTwo() throws Exception {
            mockMvc.perform(get("/recipes/compare").param("names", "红烧肉"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("PARAM_INVALID"));
        }

        @Test
        @DisplayName("超过4个菜品返回 400")
        void moreThanFour() throws Exception {
            mockMvc.perform(get("/recipes/compare").param("names", "a,b,c,d,e"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("PARAM_INVALID"));
        }
    }

    // ==================== 每日推荐 ====================

    @Nested
    @DisplayName("每日推荐 GET /recipes/daily-recommend")
    class DailyRecommendTest {
        @Test
        @DisplayName("默认参数返回推荐结果")
        void defaultParams() throws Exception {
            DailyRecommendResult dailyResult = new DailyRecommendResult(
                    "夏季", "夏季宜清热解暑...",
                    List.of(new RecommendCombo(sampleRecipe(), null, null))
            );
            when(recipeService.dailyRecommend(isNull(), eq(1))).thenReturn(dailyResult);

            mockMvc.perform(get("/recipes/daily-recommend"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.season").value("夏季"))
                    .andExpect(jsonPath("$.dietAdvice").isString())
                    .andExpect(jsonPath("$.combos[0].meat.name").value("红烧肉"));
        }
    }

    // ==================== 营养成分 ====================

    @Nested
    @DisplayName("营养成分 GET /recipes/nutrition")
    class NutritionTest {
        @Test
        @DisplayName("有效菜名返回营养信息")
        void validName() throws Exception {
            when(recipeService.getNutrition("红烧肉")).thenReturn(sampleDetail());

            mockMvc.perform(get("/recipes/nutrition").param("name", "红烧肉"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("红烧肉"));
        }

        @Test
        @DisplayName("空菜名返回 400")
        void emptyName() throws Exception {
            mockMvc.perform(get("/recipes/nutrition").param("name", ""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("PARAM_INVALID"));
        }

        @Test
        @DisplayName("不存在的菜品返回 400 + SEARCH_NO_RESULTS")
        void notFound() throws Exception {
            when(recipeService.getNutrition("不存在")).thenReturn(null);

            mockMvc.perform(get("/recipes/nutrition").param("name", "不存在"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("SEARCH_NO_RESULTS"));
        }
    }

    // ==================== 模糊搜索 ====================

    @Nested
    @DisplayName("模糊搜索 GET /recipes")
    class FindByNameTest {
        @Test
        @DisplayName("有效名称返回匹配结果")
        void validName() throws Exception {
            when(recipeService.findByName("红烧")).thenReturn(List.of(sampleRecipe()));

            mockMvc.perform(get("/recipes").param("name", "红烧"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(1));
        }

        @Test
        @DisplayName("空名称返回 400")
        void emptyName() throws Exception {
            mockMvc.perform(get("/recipes").param("name", ""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("PARAM_INVALID"));
        }
    }

    // ==================== 分页 GET /recipes/page ====================

    @Nested
    @DisplayName("分页 GET /recipes/page")
    class PageTest {
        @Test
        @DisplayName("默认参数返回分页结果")
        void defaultParams() throws Exception {
            PageResult<RecipeSummaryResponse> page = new PageResult<>(1L, 10L, 25L, 3L, List.of(sampleRecipe()));
            when(recipeService.page(eq(1), eq(10), isNull(), isNull())).thenReturn(page);

            mockMvc.perform(get("/recipes/page"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(25))
                    .andExpect(jsonPath("$.pages").value(3))
                    .andExpect(jsonPath("$.records[0].name").value("红烧肉"));
        }

        @Test
        @DisplayName("带分类和关键词参数返回分页结果")
        void withFilters() throws Exception {
            PageResult<RecipeSummaryResponse> page = new PageResult<>(1L, 10L, 5L, 1L, List.of(sampleRecipe()));
            when(recipeService.page(eq(1), eq(10), eq(1L), eq("红"))).thenReturn(page);

            mockMvc.perform(get("/recipes/page")
                            .param("categoryId", "1")
                            .param("keyword", "红"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(5))
                    .andExpect(jsonPath("$.records[0].name").value("红烧肉"));
        }
    }

    // ==================== 新增 POST /recipes ====================

    @Nested
    @DisplayName("新增 POST /recipes")
    class CreateTest {
        @Test
        @DisplayName("有效请求体创建菜谱成功")
        void validCreate() throws Exception {
            when(recipeService.create(any(CreateRecipeRequest.class))).thenReturn(sampleDetail());

            String body = "{\"name\":\"红烧肉\",\"categoryId\":1,\"ingredients\":[{\"name\":\"五花肉\",\"quantity\":\"500g\"}],\"steps\":[{\"stepOrder\":1,\"description\":\"切块焯水\"}]}";
            mockMvc.perform(post("/recipes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("红烧肉"))
                    .andExpect(jsonPath("$.ingredients[0].name").value("五花肉"));
        }

        @Test
        @DisplayName("缺少必填字段返回 400 + VALIDATION_ERROR")
        void missingFields() throws Exception {
            String body = "{\"categoryId\":1}";
            mockMvc.perform(post("/recipes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }

    // ==================== 修改 PUT /recipes/{id} ====================

    @Nested
    @DisplayName("修改 PUT /recipes/{id}")
    class UpdateTest {
        @Test
        @DisplayName("有效请求体修改菜谱成功")
        void validUpdate() throws Exception {
            when(recipeService.update(eq(1L), any(UpdateRecipeRequest.class))).thenReturn(sampleDetail());

            String body = "{\"summary\":\"经典红烧肉\"}";
            mockMvc.perform(put("/recipes/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("红烧肉"));
        }
    }

    // ==================== 删除 DELETE /recipes/{id} ====================

    @Nested
    @DisplayName("删除 DELETE /recipes/{id}")
    class DeleteTest {
        @Test
        @DisplayName("有效ID删除成功")
        void validDelete() throws Exception {
            mockMvc.perform(delete("/recipes/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.deleted").value(true));
        }
    }
}
