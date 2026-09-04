# 查询 Filter 使用指南（前端版）

> 适用后端：travelsky 查询框架（`DevQueryVO` + `QueryParseHelper`）
> 用途：前端拼接查询条件时，按本规范构造 `filter` 字符串，后端自动解析为 SQL。

---

## 1. 请求整体结构

前端只需提交一个 JSON 对象（字段名固定）：

```json
{
  "filter": "[[name,=,john]]",
  "sorts": "[{\"columns\":[\"createTime\"],\"direction\":\"DESC\"}]",
  "page": 1,
  "limit": 20
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `filter` | String | 过滤条件 DSL（见下文）。为空/不传表示不过滤 |
| `sorts` | String | 排序，JSON 数组字符串（见 §5） |
| `page` | Long | 页码，默认 1 |
| `limit` | Long | 每页条数，默认 20 |

---

## 2. filter 语法（核心）

filter 是一段**类 Lisp 的嵌套括号表达式**，用 `[ ]` 分组、`,` 分隔元素。

### 2.1 一个条件（三元组）

```
[字段名, 操作符, 值1, 值2, ...]
```

- **字段名**：实体的 **Java 属性名（camelCase）**，不是数据库列名。
  - 例：`userName`、`createTime`、`age`
  - 关联表字段：`表别名.属性名`，例：`u.userName`
- **操作符**：见 §3。
- **值**：字符串形式，多个值（IN/BETWEEN）依次罗列。

示例：
```
[name,=,john]
[age,>=,18]
[status,in,ACTIVE,CLOSED]
[createTime,between,2023-01-01 00:00:00,2023-12-31 23:59:59]
```

### 2.2 逻辑组合（AND / OR）

条件之间用 `AND` / `OR` 连接，**左右交替排列**：

```
[条件A, AND, 条件B]
[条件A, OR, 条件B]
```

可链式多条件：
```
[[sex,=,man], OR, [work,in,it,kj], OR, [bbb,=,ddd]]
```

### 2.3 分组 / 优先级

用**额外一层 `[]`** 表示括号，提升优先级或改变结合顺序：

```
[条件A, AND, [条件B, OR, 条件C]]
```
→ SQL：`A AND (B OR C)`

### 2.4 最外层必须是一对 `[ ]`

整个 filter 必须被一层 `[ ]` 包裹。即使只有一个条件也要包：

✅ `[[name,=,john]]`
❌ `[name,=,john]`

### 2.5 完整嵌套示例

```
[
  [name, =, john],
  AND,
  [
    [ [age, >=, 18], [createTime, between, 2023-01-01 00:00:00, 2024-01-01 00:00:00] ],
    OR,
    [ [sex, =, man], OR, [work, in, it, kj], OR, [dept, =, dev] ]
  ]
]
```

等价 SQL：
```sql
WHERE name = 'john'
  AND ( (age >= 18 AND createTime BETWEEN '2023-01-01 00:00:00' AND '2024-01-01 00:00:00')
     OR (sex = 'man' OR work IN ('it','kj') OR dept = 'dev') )
```

> ⚠️ **重要约定**：不要在顶层平铺混用 `AND` 与 `OR`（如 `[A, OR, B, AND, C]`），其 SQL 优先级不明确。凡是涉及不同优先级，**务必用嵌套 `[]` 显式分组**。

---

## 3. 操作符一览

| 语义 | filter 中写法 | 后端映射 | 值个数 |
|------|---------------|----------|--------|
| 等于 | `=` | eq | 1 |
| 不等于 | `!=` 或 `<>` | ne | 1 |
| 大于 | `>` | gt | 1 |
| 大于等于 | `>=` | ge | 1 |
| 小于 | `<` | lt | 1 |
| 小于等于 | `<=` | le | 1 |
| 模糊包含（前后%） | `CONTAINS` | like | 1 |
| 左模糊（%xxx） | `LEFT` | likeLeft | 1 |
| 右模糊（xxx%） | `RIGHT` | likeRight | 1 |
| 区间 | `BETWEEN` | between | 2 |
| 在集合中 | `IN` | in | ≥1 |
| 不在集合中 | `NOT_IN`（或 `NOTIN`） | notIn | ≥1 |
| 为空 | `NULL`（或 `ISNULL`） | isNull | 0 |
| 不为空 | `NOT_NULL`（或 `NOTNULL`） | isNotNull | 0 |
| 逻辑与 | `AND` | — | — |
| 逻辑或 | `OR` | — | — |

> 操作符大小写不敏感，`=`/`!=`/`<>`，`IN`/`NOTIN` 等均为合法别名。

---

## 4. 值的格式约定

值一律写成**字符串**，后端按实体字段真实类型自动转换：

| 字段类型 | 值的写法 | 示例 |
|----------|----------|------|
| String | 原样 | `john` |
| 数字（Long/Integer/BigDecimal） | 数字字符串 | `18`、`3.5` |
| 日期（Date） | `yyyy-MM-dd HH:mm:ss` | `2023-01-01 00:00:00` |
| 枚举（Enum） | 枚举名（大写通常） | `ACTIVE` |
| 布尔 | 视实体定义为字符串（`true`/`false` 或 `1`/`0`） | `true` |

> 字段名不存在或类型转换失败时，值会原样透传（可能匹配不到数据），请在联调时确认字段名与类型。

---

## 5. 排序（sorts）

`sorts` 是 JSON 数组字符串，每个对象含 `columns`（字段数组）与 `direction`（`ASC`/`DESC`）：

```json
[
  {"columns":["createTime"],"direction":"DESC"},
  {"columns":["userName"],"direction":"ASC"}
]
```

多字段：同一对象 `columns` 放多个字段即多列同序；多个对象即多级排序。

---

## 6. 常见 UI 场景 → filter 速查

| 前端场景 | filter |
|----------|--------|
| 关键字搜索（模糊） | `[[name, CONTAINS, 张]]` |
| 精确匹配某状态 | `[[status, =, ACTIVE]]` |
| 多状态筛选（下拉多选） | `[[status, IN, A, B, C]]` |
| 日期范围 | `[[createTime, BETWEEN, 2023-01-01 00:00:00, 2023-12-31 23:59:59]]` |
| 数值区间（价格/年龄） | `[[age, BETWEEN, 18, 60]]` |
| 多个条件并列（AND） | `[[name, CONTAINS, 张], AND, [status, =, ACTIVE]]` |
| 任一满足（OR） | `[[status, =, A], OR, [status, =, B]]` |
| 复杂组合 | `[[a, =, 1], AND, [[b, =, 2], OR, [c, =, 3]]]` |
| 空值筛选 | `[[deletedAt, NULL]]` |

---

## 7. 给 AI / 低代码平台的「提示词」（可直接复制）

> 把下面这段文本作为 system/user 提示词，让 AI 帮你把自然语言查询需求转成正确的 `filter` 字符串。

```
你是一个帮助前端构造后端查询 filter 字符串的助手。后端使用如下 DSL（类 Lisp 嵌套括号表达式）：

【字段】用实体的 Java 属性名（camelCase），如 userName、createTime；关联表用 "表别名.属性名"。
【值】一律写成字符串；日期用 "yyyy-MM-dd HH:mm:ss"；数字直接写数字串；枚举写枚举名。
【操作符】=, != (或<>), >, >=, <, <=, CONTAINS(模糊), LEFT(左模糊), RIGHT(右模糊),
          BETWEEN(2个值), IN(≥1个值), NOT_IN, NULL, NOT_NULL, 逻辑符 AND / OR。
【语法】
  1) 单条件: [字段, 操作符, 值1, 值2...]
  2) 组合:   [条件A, AND, 条件B] 或 [条件A, OR, 条件B]，可链式
  3) 分组:   用额外一层 [] 表示括号，如 [A, AND, [B, OR, C]] → A AND (B OR C)
  4) 整个 filter 最外层必须是一对 []
【排序 sorts】JSON 数组字符串: [{"columns":["createTime"],"direction":"DESC"}]
【请求】最终输出 JSON: { "filter": "...", "sorts": "...", "page": 1, "limit": 20 }

规则：
 - 涉及不同优先级时，必须显式用嵌套 [] 分组，禁止顶层平铺混用 AND/OR。
 - 只输出 filter 字符串与完整请求 JSON，不要编造不存在的字段。
 - 用户描述需求时，先澄清字段名；字段名未知时提示用户确认。

示例：用户说"用户名包含张三，且状态是 ACTIVE 或 VIP，按创建时间倒序"
输出：
{
  "filter": "[[userName, CONTAINS, 张三], AND, [[status, =, ACTIVE], OR, [status, =, VIP]]]",
  "sorts": "[{\"columns\":[\"createTime\"],\"direction\":\"DESC\"}]",
  "page": 1,
  "limit": 20
}
```

---

## 8. 调试技巧

1. 后端解析失败会抛 `RuntimeException("Failed to parse input: ...")`，看到该报错先检查：
   - 最外层是否成对 `[]`
   - 逗号、括号是否成对标点（不要中文全角 `，」「】`）
   - 操作符是否拼写正确（参考 §3）
2. 字段名对照实体 Java 类（camelCase），不是数据库列名。
3. 联调时先在 §6 速查表挑最接近的场景做最小复现。
