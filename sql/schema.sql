-- ============================================================
-- CookLikeHOC 厨师 Agent 数据库建表脚本
-- 数据库: MySQL 8.0+
-- 编码: utf8mb4
-- ============================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `cook_like_hoc`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;
USE `cook_like_hoc`;

-- ============================================================
-- 1. 菜品分类表
-- ============================================================
CREATE TABLE IF NOT EXISTS `category` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(32)  NOT NULL COMMENT '分类名称（如：主食、卤菜）',
    `dir_name`    VARCHAR(32)  NOT NULL COMMENT 'CookLikeHOC 目录名',
    `sort_order`  INT          NOT NULL DEFAULT 0 COMMENT '排序序号',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品分类表';

-- ============================================================
-- 2. 菜品主表
-- ============================================================
CREATE TABLE IF NOT EXISTS `recipe` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`           VARCHAR(128) NOT NULL COMMENT '菜品名称',
    `category_id`    BIGINT       NOT NULL COMMENT '分类ID',
    `alias`          VARCHAR(256)          DEFAULT NULL COMMENT '别名，多个用逗号分隔',
    `image_url`      VARCHAR(512)          DEFAULT NULL COMMENT '图片路径',
    `summary`        VARCHAR(512)          DEFAULT NULL COMMENT '一句话简介',
    `remark`         TEXT                  DEFAULT NULL COMMENT '备注/补充说明',
    `nutrition_json` JSON                  DEFAULT NULL COMMENT '营养成分JSON',
    `raw_markdown`   MEDIUMTEXT            DEFAULT NULL COMMENT '原始Markdown内容',
    `source_file`    VARCHAR(256)          DEFAULT NULL COMMENT '源文件相对路径',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name_category` (`name`, `category_id`),
    INDEX `idx_category_id` (`category_id`),
    CONSTRAINT `fk_recipe_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品主表';

-- ============================================================
-- 3. 配料表
-- ============================================================
CREATE TABLE IF NOT EXISTS `ingredient` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `recipe_id`   BIGINT       NOT NULL COMMENT '菜品ID',
    `name`        VARCHAR(256) NOT NULL COMMENT '原料名称',
    `brand`       VARCHAR(256)          DEFAULT NULL COMMENT '品牌/供应商',
    `quantity`    VARCHAR(128)          DEFAULT NULL COMMENT '用量',
    `note`        VARCHAR(512)          DEFAULT NULL COMMENT '备注',
    `sort_order`  INT          NOT NULL DEFAULT 0 COMMENT '排序',
    PRIMARY KEY (`id`),
    INDEX `idx_recipe_id` (`recipe_id`),
    CONSTRAINT `fk_ingredient_recipe` FOREIGN KEY (`recipe_id`) REFERENCES `recipe` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='配料表';

-- ============================================================
-- 4. 制作步骤表
-- ============================================================
CREATE TABLE IF NOT EXISTS `recipe_step` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `recipe_id`   BIGINT       NOT NULL COMMENT '菜品ID',
    `step_order`  INT          NOT NULL COMMENT '步骤序号',
    `description` TEXT         NOT NULL COMMENT '步骤描述',
    PRIMARY KEY (`id`),
    INDEX `idx_recipe_id` (`recipe_id`),
    CONSTRAINT `fk_step_recipe` FOREIGN KEY (`recipe_id`) REFERENCES `recipe` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='制作步骤表';

-- ============================================================
-- 5. 用户表（扩展）
-- ============================================================
CREATE TABLE IF NOT EXISTS `user` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`      VARCHAR(64)  NOT NULL COMMENT '用户名',
    `password_hash` VARCHAR(256) NOT NULL COMMENT '密码哈希',
    `role`          VARCHAR(32)  NOT NULL DEFAULT 'CHEF' COMMENT '角色: CHEF/MANAGER/ADMIN',
    `phone`         VARCHAR(20)           DEFAULT NULL COMMENT '手机号',
    `enabled`       TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================
-- 6. 用户收藏表（扩展）
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_favorite` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
    `recipe_id`   BIGINT   NOT NULL COMMENT '菜品ID',
    `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_recipe` (`user_id`, `recipe_id`),
    INDEX `idx_user_id` (`user_id`),
    CONSTRAINT `fk_favorite_user`   FOREIGN KEY (`user_id`)   REFERENCES `user` (`id`)   ON DELETE CASCADE,
    CONSTRAINT `fk_favorite_recipe` FOREIGN KEY (`recipe_id`) REFERENCES `recipe` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收藏表';

-- ============================================================
-- 7. 每日推荐表（扩展）
-- ============================================================
CREATE TABLE IF NOT EXISTS `daily_recommend` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `recommend_date` DATE  NOT NULL COMMENT '推荐日期',
    `recipe_ids`  JSON     NOT NULL COMMENT '推荐菜品ID列表',
    `reason`      TEXT              DEFAULT NULL COMMENT '推荐理由',
    `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_date` (`recommend_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日推荐表';

-- ============================================================
-- 初始化分类数据
-- ============================================================
INSERT INTO `category` (`name`, `dir_name`, `sort_order`) VALUES
    ('主食',   '主食',   1),
    ('凉拌',   '凉拌',   2),
    ('卤菜',   '卤菜',   3),
    ('早餐',   '早餐',   4),
    ('汤',     '汤',     5),
    ('炒菜',   '炒菜',   6),
    ('炖菜',   '炖菜',   7),
    ('炸品',   '炸品',   8),
    ('烤类',   '烤类',   9),
    ('烫菜',   '烫菜',   10),
    ('煮锅',   '煮锅',   11),
    ('砂锅菜', '砂锅菜', 12),
    ('蒸菜',   '蒸菜',   13),
    ('配料',   '配料',   14),
    ('饮品',   '饮品',   15);
