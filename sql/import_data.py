#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
CookLikeHOC 数据一键导入脚本

功能：
  扫描 CookLikeHOC 仓库中所有菜品 Markdown 文件，
  解析菜品名称、分类、配料、步骤、营养成分，
  导入到 MySQL 数据库。

用法：
  python import_data.py [--host HOST] [--port PORT] [--user USER] [--password PWD] [--db DB]

依赖：
  pip install pymysql

注意：
  先执行 schema.sql 创建库表，再运行本脚本。
"""

import os
import re
import sys
import json
import argparse
from pathlib import Path
from datetime import datetime

try:
    import pymysql
except ImportError:
    print("请先安装 pymysql: pip install pymysql")
    sys.exit(1)

# ============================================================
# 配置
# ============================================================

# CookLikeHOC 仓库根目录（相对于本脚本的位置）
REPO_ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "CookLikeHOC")

# 需要跳过的文件名
SKIP_FILES = {"README.md", ".gitignore"}

# 分类目录名 → 分类名称 映射
CATEGORY_MAP = {
    "主食":   "主食",
    "凉拌":   "凉拌",
    "卤菜":   "卤菜",
    "早餐":   "早餐",
    "汤":     "汤",
    "炒菜":   "炒菜",
    "炖菜":   "炖菜",
    "炸品":   "炸品",
    "烤类":   "烤类",
    "烫菜":   "烫菜",
    "煮锅":   "煮锅",
    "砂锅菜": "砂锅菜",
    "蒸菜":   "蒸菜",
    "配料":   "配料",
    "饮品":   "饮品",
}

# 默认数据库连接参数
DB_CONFIG = {
    "host": "127.0.0.1",
    "port": 3306,
    "user": "root",
    "password": "root",
    "db": "cook_like_hoc",
    "charset": "utf8mb4",
}


# ============================================================
# Markdown 解析器
# ============================================================

class RecipeParser:
    """解析单个菜品 Markdown 文件"""

    def __init__(self, filepath: str, category_name: str):
        self.filepath = filepath
        self.category_name = category_name
        self.content = ""
        self.lines: list[str] = []

    def parse(self) -> dict | None:
        """解析文件，返回菜品数据字典，如果不是有效菜品文件则返回 None"""
        with open(self.filepath, "r", encoding="utf-8") as f:
            self.content = f.read()

        if not self.content.strip():
            return None

        self.lines = self.content.split("\n")

        name = self._extract_name()
        if not name:
            return None

        image_url = self._extract_image()
        ingredients = self._extract_ingredients()
        steps = self._extract_steps()
        nutrition = self._extract_nutrition()
        remark = self._extract_remark()

        return {
            "name": name.strip(),
            "category_name": self.category_name,
            "image_url": image_url,
            "summary": "",
            "remark": remark,
            "nutrition_json": json.dumps(nutrition, ensure_ascii=False) if nutrition else None,
            "raw_markdown": self.content.strip(),
            "source_file": self._relative_path(),
            "ingredients": ingredients,
            "steps": steps,
        }

    def _relative_path(self) -> str:
        """获取相对于 CookLikeHOC 根目录的路径"""
        try:
            return os.path.relpath(self.filepath, REPO_ROOT).replace("\\", "/")
        except ValueError:
            return os.path.basename(self.filepath)

    def _extract_name(self) -> str | None:
        """提取菜品名称（第一个 # 标题）"""
        for line in self.lines:
            stripped = line.strip()
            # 匹配一级标题: # 菜品名
            m = re.match(r"^#\s+(.+)$", stripped)
            if m:
                name = m.group(1).strip()
                # 排除分类索引页（README）的标题
                if name in CATEGORY_MAP.values():
                    return None
                return name
        return None

    def _extract_image(self) -> str | None:
        """提取图片路径: ![xxx](../images/xxx.png)"""
        m = re.search(r"!\[.*?\]\(([^)]+)\)", self.content)
        if m:
            path = m.group(1).strip()
            # 将相对路径标准化
            path = path.replace("../images/", "images/")
            return path
        return None

    def _extract_ingredients(self) -> list[dict]:
        """
        提取配料列表。
        支持 ## 配料 和 ## 原料 两种标题格式。
        格式：- 原料名（供应商信息）
        """
        ingredients = []
        in_section = False

        for line in self.lines:
            stripped = line.strip()

            # 检测配料/原料节开始
            if re.match(r"^##\s*(配料|原料)\s*:?\s*$", stripped):
                in_section = True
                continue

            # 检测下一节开始，结束配料解析
            if in_section and stripped.startswith("## "):
                break

            if in_section:
                # 匹配列表项: - xxx
                m = re.match(r"^[-*]\s+(.+)$", stripped)
                if m:
                    raw = m.group(1).strip()
                    if raw:
                        ingredient = self._parse_ingredient_line(raw)
                        ingredients.append(ingredient)

        # 重新编号 sort_order
        for i, ing in enumerate(ingredients):
            ing["sort_order"] = i + 1

        return ingredients

    def _parse_ingredient_line(self, raw: str) -> dict:
        """
        解析一条配料行。
        示例：
          '手工挂面（金寨先徽、陕西金沙河）' → name=手工挂面, brand=金寨先徽、陕西金沙河
          '老母鸡'                            → name=老母鸡
          '[鸡油料](/配料/鸡油料.md)'          → name=鸡油料, note=引用关联
          '卤油（大豆油、花椒、辣椒、生姜等）（与卤油都来自成都圣恩...）'
        """
        name = raw
        brand = None
        quantity = None
        note = None

        # 移除 Markdown 链接 [text](url) → text
        name = re.sub(r"\[([^\]]+)\]\([^)]+\)", r"\1", name)

        # 如果包含链接引用，记录为 note
        if re.search(r"\[([^\]]+)\]\([^)]+\)", raw):
            note = "引用关联"

        # 提取末尾括号中的品牌信息
        # 模式: xxx（供应商1、供应商2...）
        brand_matches = list(re.finditer(r"[（(]([^)）]+?供应商[^)）]*|[^)）]*?来自[^)）]*|[^)）]{3,40})[）)]$", name))
        # 更简单的方式：提取所有括号内容
        paren_matches = list(re.finditer(r"[（(]([^)）]*?)[）)]", name))
        if paren_matches:
            # 最后一个括号内容视为品牌/备注信息
            last_match = paren_matches[-1]
            potential_brand = last_match.group(1).strip()
            # 移除这个括号内容
            name = name[:last_match.start()] + name[last_match.end():]
            name = name.strip()

            # 判断是品牌还是备注
            if any(kw in potential_brand for kw in
                   ["供应商", "来自", "中央厨房", "自制", "未公布", "适量", "口味", "官方",
                    "蜀海", "温氏", "圣恩", "新雅轩", "家佳百味", "金沙河", "彩云", "甜甜雨",
                    "先徽", "食品", "品牌"]):
                brand = potential_brand
            else:
                note = potential_brand

        # 尝试提取用量（数字+单位模式在末尾）
        # e.g. "盐 2g" → name=盐, quantity=2g
        # e.g. "水 5000g" → name=水, quantity=5000g
        qty_match = re.search(r"\s+(\d+[～~\-]?\d*\s*(?:g|kg|mL|L|ml|l|个|只|颗|片|块|条|根|勺|汤匙|茶匙)\b.*)$", name)
        if qty_match and brand is None:
            quantity = qty_match.group(1).strip()
            name = name[:qty_match.start()].strip()

        return {
            "name": name.strip(),
            "brand": brand.strip() if brand else None,
            "quantity": quantity.strip() if quantity else None,
            "note": note.strip() if note else None,
            "sort_order": 0,
        }

    def _extract_steps(self) -> list[dict]:
        """
        提取制作步骤。
        支持 ## 步骤 和 ## 步骤： 两种标题格式。
        格式：
          - 1. 步骤一描述；
          - 2. 步骤二描述。
        或：
          - 每只鸡用 2g 盐均匀揉搓...
        """
        steps = []
        in_section = False

        for line in self.lines:
            stripped = line.strip()

            # 检测步骤节开始
            if re.match(r"^##\s*步骤\s*:?\s*$", stripped):
                in_section = True
                continue

            # 检测下一节开始
            if in_section and stripped.startswith("## "):
                break

            if in_section:
                m = re.match(r"^[-*]\s+(.+)$", stripped)
                if m:
                    raw = m.group(1).strip()
                    if raw:
                        # 提取步骤序号
                        order_match = re.match(r"^(\d+)[\.\、]\s*(.+)", raw)
                        if order_match:
                            step_order = int(order_match.group(1))
                            description = order_match.group(2).strip()
                        else:
                            # 无明确序号，自动编号
                            step_order = len(steps) + 1
                            description = raw

                        # 清理末尾分号
                        description = description.rstrip("；;")
                        steps.append({
                            "step_order": step_order,
                            "description": description,
                        })

        return steps

    def _extract_nutrition(self) -> dict | None:
        """
        提取营养成分表。
        格式：
          ## 营养成分
          | 项目 | 每 100g 含量 |
          | :--- | :--- |
          | 热量 | 61 Kcal |
        """
        in_section = False
        table_lines = []

        for line in self.lines:
            stripped = line.strip()

            if re.match(r"^##\s*营养成分\s*$", stripped):
                in_section = True
                continue

            if in_section and stripped.startswith("## "):
                break

            if in_section:
                if stripped.startswith("|") and "---" not in stripped:
                    table_lines.append(stripped)

        if not table_lines:
            return None

        nutrition = {}
        for row in table_lines:
            cells = [c.strip() for c in row.split("|") if c.strip()]
            if len(cells) >= 2:
                key = cells[0]
                value = cells[1]
                # 统一 key 名称
                key_map = {
                    "热量": "energy",
                    "蛋白质": "protein",
                    "脂肪": "fat",
                    "碳水化合物": "carbohydrate",
                    "钠": "sodium",
                }
                eng_key = key_map.get(key, key)
                nutrition[eng_key] = value

        return nutrition if nutrition else None

    def _extract_remark(self) -> str | None:
        """
        提取备注信息。
        位于配料/步骤之前或之后的独立段落，通常是补充说明。
        """
        # 简单策略：提取 ## 配料 或 ## 原料 之前且在 # 标题之后的非空段落
        in_intro = False
        remarks = []

        for line in self.lines:
            stripped = line.strip()

            if stripped.startswith("# ") and not stripped.startswith("## "):
                in_intro = True
                continue

            if stripped.startswith("## ") or stripped.startswith("!["):
                in_intro = False
                continue

            if in_intro and stripped and not stripped.startswith("[") and not stripped.startswith("<!--"):
                remarks.append(stripped)

        return "\n".join(remarks) if remarks else None


# ============================================================
# 文件扫描器
# ============================================================

def scan_recipes(repo_root: str) -> list[dict]:
    """扫描仓库中所有菜品 Markdown 文件"""
    recipes = []
    stats = {"scanned": 0, "skipped": 0, "error": 0}

    for dir_name, cat_name in CATEGORY_MAP.items():
        dir_path = os.path.join(repo_root, dir_name)

        if not os.path.isdir(dir_path):
            print(f"  [WARN] 目录不存在，跳过: {dir_name}")
            continue

        for filename in os.listdir(dir_path):
            if not filename.endswith(".md"):
                continue
            if filename in SKIP_FILES:
                stats["skipped"] += 1
                continue

            filepath = os.path.join(dir_path, filename)
            stats["scanned"] += 1

            try:
                parser = RecipeParser(filepath, cat_name)
                recipe = parser.parse()

                if recipe:
                    recipes.append(recipe)
                else:
                    stats["skipped"] += 1
                    print(f"  [SKIP] 非菜品文件: {parser._relative_path()}")

            except Exception as e:
                stats["error"] += 1
                print(f"  [ERROR] 解析失败 {filepath}: {e}")

    return recipes, stats


# ============================================================
# 数据库导入器
# ============================================================

class DatabaseImporter:
    """将解析后的菜品数据导入 MySQL"""

    def __init__(self, conn):
        self.conn = conn

    def get_category_id_map(self) -> dict[str, int]:
        """获取分类名 → ID 映射"""
        with self.conn.cursor() as cursor:
            cursor.execute("SELECT id, name FROM category")
            rows = cursor.fetchall()
            return {row[1]: row[0] for row in rows}

    def import_all(self, recipes: list[dict]) -> dict:
        """批量导入所有菜品"""
        stats = {"inserted": 0, "updated": 0, "error": 0}
        cat_map = self.get_category_id_map()

        with self.conn.cursor() as cursor:
            for recipe_data in recipes:
                try:
                    cat_name = recipe_data["category_name"]
                    cat_id = cat_map.get(cat_name)

                    if cat_id is None:
                        print(f"  [ERROR] 未知分类: {cat_name}, 菜品: {recipe_data['name']}")
                        stats["error"] += 1
                        continue

                    # 检查是否已存在（按名称+分类去重）
                    cursor.execute(
                        "SELECT id FROM recipe WHERE name = %s AND category_id = %s",
                        (recipe_data["name"], cat_id),
                    )
                    existing = cursor.fetchone()

                    if existing:
                        recipe_id = existing[0]
                        # 更新现有记录
                        self._update_recipe(cursor, recipe_id, recipe_data, cat_id)
                        stats["updated"] += 1
                    else:
                        # 插入新记录
                        recipe_id = self._insert_recipe(cursor, recipe_data, cat_id)
                        stats["inserted"] += 1

                    # 删除旧配料和步骤，重新插入
                    cursor.execute("DELETE FROM ingredient WHERE recipe_id = %s", (recipe_id,))
                    cursor.execute("DELETE FROM recipe_step WHERE recipe_id = %s", (recipe_id,))

                    # 插入配料
                    for ing in recipe_data["ingredients"]:
                        self._insert_ingredient(cursor, recipe_id, ing)

                    # 插入步骤
                    for step in recipe_data["steps"]:
                        self._insert_step(cursor, recipe_id, step)

                except Exception as e:
                    stats["error"] += 1
                    print(f"  [ERROR] 导入失败 [{recipe_data['name']}]: {e}")

        self.conn.commit()
        return stats

    def _insert_recipe(self, cursor, data: dict, cat_id: int) -> int:
        sql = """INSERT INTO recipe
            (name, category_id, alias, image_url, summary, remark, nutrition_json, raw_markdown, source_file)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)"""
        cursor.execute(sql, (
            data["name"],
            cat_id,
            data.get("alias"),
            data.get("image_url"),
            data.get("summary", ""),
            data.get("remark"),
            data.get("nutrition_json"),
            data.get("raw_markdown"),
            data.get("source_file"),
        ))
        return cursor.lastrowid

    def _update_recipe(self, cursor, recipe_id: int, data: dict, cat_id: int):
        sql = """UPDATE recipe SET
            category_id=%s, image_url=%s, summary=%s, remark=%s,
            nutrition_json=%s, raw_markdown=%s, source_file=%s, updated_at=NOW()
            WHERE id=%s"""
        cursor.execute(sql, (
            cat_id,
            data.get("image_url"),
            data.get("summary", ""),
            data.get("remark"),
            data.get("nutrition_json"),
            data.get("raw_markdown"),
            data.get("source_file"),
            recipe_id,
        ))

    def _insert_ingredient(self, cursor, recipe_id: int, ing: dict):
        sql = """INSERT INTO ingredient (recipe_id, name, brand, quantity, note, sort_order)
            VALUES (%s, %s, %s, %s, %s, %s)"""
        cursor.execute(sql, (
            recipe_id,
            ing["name"],
            ing.get("brand"),
            ing.get("quantity"),
            ing.get("note"),
            ing.get("sort_order", 0),
        ))

    def _insert_step(self, cursor, recipe_id: int, step: dict):
        sql = """INSERT INTO recipe_step (recipe_id, step_order, description)
            VALUES (%s, %s, %s)"""
        cursor.execute(sql, (
            recipe_id,
            step["step_order"],
            step["description"],
        ))


# ============================================================
# 主入口
# ============================================================

def parse_args():
    parser = argparse.ArgumentParser(description="CookLikeHOC 菜品数据一键导入工具")
    parser.add_argument("--host", default=DB_CONFIG["host"], help="MySQL 主机地址")
    parser.add_argument("--port", type=int, default=DB_CONFIG["port"], help="MySQL 端口")
    parser.add_argument("--user", default=DB_CONFIG["user"], help="MySQL 用户名")
    parser.add_argument("--password", default=DB_CONFIG["password"], help="MySQL 密码")
    parser.add_argument("--db", default=DB_CONFIG["db"], help="数据库名称")
    parser.add_argument("--repo", default=REPO_ROOT, help="CookLikeHOC 仓库路径")
    parser.add_argument("--dry-run", action="store_true", help="仅解析不导入（预览模式）")
    return parser.parse_args()


def main():
    args = parse_args()

    print("=" * 60)
    print("  CookLikeHOC 菜品数据一键导入工具")
    print("=" * 60)
    print(f"  数据源: {args.repo}")
    print(f"  数据库: {args.host}:{args.port}/{args.db}")
    print(f"  模式: {'预览 (不写入数据库)' if args.dry_run else '正式导入'}")
    print()

    # 1. 扫描文件
    print("[1/3] 扫描菜品文件...")
    recipes, scan_stats = scan_recipes(args.repo)
    print(f"  扫描: {scan_stats['scanned']} 个文件")
    print(f"  跳过: {scan_stats['skipped']} 个文件")
    print(f"  错误: {scan_stats['error']} 个文件")
    print(f"  有效菜品: {len(recipes)} 个")
    print()

    if not recipes:
        print("[ERROR] 未找到任何菜品数据，请检查 CookLikeHOC 仓库路径是否正确。")
        sys.exit(1)

    # 预览模式：打印前 5 个菜品
    if args.dry_run:
        print("[2/3] 预览解析结果（前 5 条）:")
        print("-" * 60)
        for r in recipes[:5]:
            print(f"  名称: {r['name']}")
            print(f"  分类: {r['category_name']}")
            print(f"  图片: {r['image_url']}")
            print(f"  配料 ({len(r['ingredients'])}):")
            for ing in r["ingredients"][:3]:
                print(f"    - {ing['name']} | {ing.get('brand', '')} | {ing.get('quantity', '')}")
            if len(r['ingredients']) > 3:
                print(f"    ... 共 {len(r['ingredients'])} 项")
            print(f"  步骤 ({len(r['steps'])}):")
            for s in r["steps"][:2]:
                print(f"    {s['step_order']}. {s['description'][:60]}...")
            if len(r['steps']) > 2:
                print(f"    ... 共 {len(r['steps'])} 步")
            print(f"  营养成分: {'有' if r['nutrition_json'] else '无'}")
            print()
        print(f"  ... 共 {len(recipes)} 个菜品（仅显示前 5 个）")
        print()
        print("[3/3] 预览完成，未写入数据库。")
        print("  使用 --dry-run 以外的参数进行正式导入。")
        return

    # 2. 连接数据库
    print("[2/3] 连接数据库...")
    try:
        conn = pymysql.connect(
            host=args.host,
            port=args.port,
            user=args.user,
            password=args.password,
            database=args.db,
            charset="utf8mb4",
        )
        print("  数据库连接成功！")
    except Exception as e:
        print(f"  [ERROR] 数据库连接失败: {e}")
        print(f"  请确保已执行 schema.sql 建库建表。")
        sys.exit(1)

    # 3. 导入数据
    print("[3/3] 导入菜品数据...")
    try:
        importer = DatabaseImporter(conn)
        import_stats = importer.import_all(recipes)
        print(f"  新增: {import_stats['inserted']} 个菜品")
        print(f"  更新: {import_stats['updated']} 个菜品")
        print(f"  错误: {import_stats['error']} 个菜品")
    except Exception as e:
        print(f"  [ERROR] 导入过程异常: {e}")
        conn.rollback()
        sys.exit(1)
    finally:
        conn.close()

    print()
    print("=" * 60)
    print("  导入完成！")
    print(f"  总计: {import_stats['inserted'] + import_stats['updated']} 个菜品")
    print("=" * 60)


if __name__ == "__main__":
    main()
