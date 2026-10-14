# 查询 Filter 使用指南（前端版）

> 适用后端：wymix 查询框架（`DevQueryVO` + `FilterParser` + 翻译层）
> 用途：前端拼接查询条件时，按本规范构造 `filter` 字符串，后端自动解析为 SQL。
>
> **事实基准**：本文描述以 `common` / `repository` / `jpa_repository` / `mybatis_repository` 的**源码**为准，
> 冲突时源码优先。解析器源码：`repository` 的 `FilterStringParser`（词法）、`FilterParser`（语法→AST）；
> 翻译层：`mybatis_repository` 的 `QueryParseHelper`、`jpa_repository` 的 `SpecificationHelper`。

---

## 1. 请求整体结构

### 1.1 传输方式（重要）

`DevQueryVO` 在 `BaseController#findAll` 上**没有 `@RequestBody`**，由 Spring MVC 按**平铺 query 参数**绑定，
因此 `filter` 与 `sorts` 都是**字符串**（不是嵌套 JSON 对象）：

```bash
curl -G 'http://localhost:8080/sampleUser' \
  --data-urlencode 'filter=[[name,=,john]]' \
  --data-urlencode 'sorts=[{"columns":["createTime"],"direction":"DESC"}]' \
  --data-urlencode 'page=1' \
  --data-urlencode 'limit=20'
```

后端接收模型（字段名固定）：

```java
public class DevQueryVO implements Serializable {
    private String filter;   // filter DSL 字符串
    private String sorts;    // SortVO 列表的 JSON 字符串
    private Long page = 1L;  // 默认 1
    private Long limit = 20L;// 默认 20
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `filter` | String | 过滤条件 DSL（见 §2）。为空/不传表示**不过滤** |
| `sorts` | String | 排序，JSON 数组字符串（见 §5）。为空/不传表示**不排序** |
| `page` | Long | 页码，从 1 开始，默认 1 |
| `limit` | Long | 每页条数，默认 20；MyBatis 侧分页插件上限 `10000`（超出部分被截为 10000） |

> 典型端点：`GET /{资源}`（由 `AbstractBaseController` 暴露），详见 `web/README.md`。

---

## 2. filter 语法（核心）

filter 是一段**类 Lisp 的嵌套括号表达式**，用 `[ ]` 分组、`,` 分隔元素。
**必须遵守的语法约束**：整个表达式必须以 `[` 开头、以 `]` 结尾；嵌套分组各自成对；逻辑大小写不敏感。

### 2.1 一个条件（三元组）

```
[字段名, 操作符, 值1, 值2, ...]
```

- **字段名**：实体的 **Java 属性名（camelCase）**，不是数据库列名。
  - 例：`userName`、`createTime`、`age`
  - 关联/表别名字段：`表别名.属性名`，例：`u.userName`
- **操作符**：见 §3（大小写不敏感）。
- **值**：字符串形式，多个值（IN/BETWEEN）依次罗列。

示例：
```
[name,=,john]
[age,>=,18]
[status,in,ACTIVE,CLOSED]
[createTime,between,2023-01-01 00:00:00,2023-12-31 23:59:59]
```

> **值内禁止出现 `,` 与 `]`**：词法层以 `,` 与 `]` 作为分隔符/结束符，这两个字符无法转义，
> 含逗号的值（如包含逗号的枚举列表）会被拆成多个值。值内部的**空格可以保留**（词法层只跳过元素前的空白），
> 因此 `2023-01-01 00:00:00` 是合法的。

### 2.2 逻辑组合（AND / OR）

条件之间用 `AND` / `OR` 连接，写在两个条件之间：

```
[条件A, AND, 条件B]
[条件A, OR, 条件B]
```

可链式多条件：
```
[[sex,=,man], OR, [work,in,it,kj], OR, [bbb,=,ddd]]
```

逻辑符大小写不敏感：`AND` / `and` / `And` 等价。

### 2.3 分组 / 优先级

用**额外一层 `[]`** 表示括号，提升优先级或改变结合顺序：

```
[[f1,=,v1], AND, [[f2,=,v2], OR, [f3,=,v3]]]
```
→ SQL：`f1 = v1 AND (f2 = v2 OR f3 = v3)`

> ⚠️ 内层分组**必须自带一层 `[]`**。写成 `[[f1,=,v1], AND, [f2,=,v2], OR, [f3,=,v3]]` 时，
> 括号内三个元素不在同一层，`[f2,=,v2]` 会被当作普通条件参与外层运算（见 §2.4 的优先级规则），
> 结果通常不是你想要的。

### 2.4 省略连接符 = AND

同一层里**连续罗列多个条件而不写连接符**时，按 **AND** 合并：

```
[[f1,=,v1], [f2,=,v2]]
```
→ 等价于 `[[f1,=,v1], AND, [f2,=,v2]]`

### 2.5 同一层混用 AND / OR：**AND 优先**

解析器对同一层的直接与间接逻辑按「先折叠 AND、再左结合折叠 OR」处理，因此：

```
[[a,=,1], OR, [b,=,2], AND, [c,=,3]]
```
→ SQL：`a = 1 OR (b = 2 AND c = 3)`  （**不是** `(a=1 OR b=2) AND c=3`）

> ⚠️ **重要约定**：不要依赖上述优先级做业务表达。凡是涉及不同优先级，**务必用嵌套 `[]` 显式分组**，
> 例如 `[[[a,=,1], OR, [b,=,2]], AND, [c,=,3]]`。

### 2.6 最外层必须是一对 `[ ]`

整个 filter 必须被一层 `[ ]` 包裹。即使只有一个条件也要包：

✅ `[[name,=,john]]`
❌ `[name,=,john]`（词法层抛错：`Invalid filter format: must start with '[' and end with ']'`）

### 2.7 完整嵌套示例

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

等价 SQL（逻辑语义，列名形态见 §4.4）：
```sql
WHERE name = 'john'
  AND ( (age >= 18 AND createTime BETWEEN '2023-01-01 00:00:00' AND '2024-01-01 00:00:00')
     OR (sex = 'man' OR work IN ('it','kj') OR dept = 'dev') )
```

注意第二组左侧 `[ [age,>=,18], [createTime,between,...] ]` 内部**没有写连接符**，按 §2.4 规则合并为 AND。

---

## 3. 操作符一览

全部操作符定义在 `org.wymix.framework.common.query.OperateSymbol`（含别名）。

| 语义 | filter 中写法（别名用 `/` 分隔） | MyBatis 翻译结果 | JPA 翻译结果 | 值个数 |
|------|------------------|------------------|--------------|--------|
| 等于 | `=` | `eq` | `equal` | 1 |
| 不等于 | `!=` / `<>` | `ne` | `notEqual` | 1 |
| 大于 | `>` | `gt` | `greaterThan` | 1 |
| 大于等于 | `>=` | `ge` | `greaterThanOrEqualTo` | 1 |
| 小于 | `<` | `lt` | `lessThan` | 1 |
| 小于等于 | `<=` | `le` | `lessThanOrEqualTo` | 1 |
| 模糊包含（前后%） | `CONTAINS` / `LIKE` | `like`（`%v%`） | `like`（`%v%`） | 1 |
| 左模糊（`%xxx`） | `LEFT` / `LIKE_LEFT` | `likeLeft`（`%v`） | `like`（`v%`）⚠️ | 1 |
| 右模糊（`xxx%`） | `RIGHT` / `LIKE_RIGHT` | `likeRight`（`v%`） | `like`（`%v`）⚠️ | 1 |
| 区间 | `BETWEEN` | `between`（值个数不为 2 时**条件被静默丢弃**）⚠️ | `between`（值个数不为 2 时抛异常） | 2 |
| 在集合中 | `IN` | `in` | `in` | ≥1 |
| 不在集合中 | `NOT_IN` / `NOTIN` | `notIn` | `not(in)` | ≥1 |
| 为空 | `NULL` / `ISNULL` | `isNull` | `isNull` | 0 |
| 不为空 | `NOT_NULL` / `NOTNULL` | `isNotNull` | `isNotNull` | 0 |
| 逻辑与 | `AND` | 见 §2.5 | 同左 | — |
| 逻辑或 | `OR` | 见 §2.5 | 同左 | — |

> 操作符大小写不敏感（`contains` 与 `CONTAINS` 等价）。
> `EXISTS` / `NOT_EXISTS` 在 `OperateSymbol` 中定义为**注释状态**，当前不可用。

### 3.1 ⚠️ 跨 ORM 差异：LEFT / RIGHT 语义相反

同一份 filter 在两条 ORM 链路上会翻出**不同的模糊匹配方向**（源码现状，非预期行为）：

| 写法 | MyBatis（`QueryParseHelper`） | JPA（`SpecificationHelper`） |
|------|-------------------------------|------------------------------|
| `LEFT` | `'%v'`（匹配以 v 结尾） | `'v%'`（匹配以 v 开头） |
| `RIGHT` | `'v%'`（匹配以 v 开头） | `'%v'`（匹配以 v 结尾） |

**规避建议**：前端统一只用 `CONTAINS`（两条链路一致），确需前缀/后缀匹配时先与后端确认走的是哪条链路。

### 3.2 一元操作符写法

`NULL` / `NOT_NULL` 不取值，以下两种写法等价（尾部空元素会被词法层丢弃）：

```
[[deletedAt, NULL]]
[[deletedAt, NOTNULL]]
```

---

## 4. 值的格式约定

值一律写成**字符串**，后端按实体字段的声明类型自动转换。

### 4.1 MyBatis 链路（`FieldHelper.translate`）

| 字段类型 | 值的写法 | 示例 | 转换失败时 |
|----------|----------|------|-----------|
| String | 原样 | `john` | 原样使用 |
| 枚举 `enum` | 枚举常量名 | `ACTIVE` | 原样使用（最终由 JDBC/DB 决定行为） |
| `java.util.Date` | `yyyy-MM-dd HH:mm:ss` | `2023-01-01 00:00:00` | 解析失败 → **null** |
| `Long` / `Integer` / `BigDecimal` | 数字字符串 | `18`、`3.5` | 解析失败 → **null** |
| 其他类型（Boolean、Double、LocalDate、LocalDateTime…） | — | — | **不转换**，原样字符串透传 |

> **关键陷阱**：数值/日期转换失败时返回 `null` 且**不报错**，最终拼出的条件通常匹配不到任何数据。
> 联调时拿不到结果，优先怀疑值格式而不是字段名。
> 字段名在实体中不存在时同样**不报错**：列名按字段名生成后直接拼进 SQL，通常由数据库报 `Unknown column`。

### 4.2 JPA 链路（`SpecificationHelper#convertValue`）

| 字段类型 | 值的写法 | 备注 |
|----------|----------|------|
| `Integer` / `Long` | `Integer.parseInt` / `Long.parseLong` | 失败抛 `IllegalArgumentException` |
| `LocalDate` | **ISO 格式 `yyyy-MM-dd`** | `LocalDate.parse`，不是 `yyyy-MM-dd HH:mm:ss` |
| `LocalDateTime` | **ISO 格式 `yyyy-MM-ddTHH:mm:ss`** | `LocalDateTime.parse`（注意字面量里的 `T`） |
| `String` 等 | 原样 `toString()` | — |
| 其他未覆盖类型 | — | 抛 `IllegalArgumentException: Unsupported value type` |

> ⚠️ 日期/时间类字段的推荐写法在两条链路上**不一致**：MyBatis 侧应写 `yyyy-MM-dd HH:mm:ss`（`java.util.Date`），
> JPA 侧的 `LocalDate` / `LocalDateTime` 必须写 ISO 格式。取值前请先确认实体字段的实际声明类型。

### 4.3 通用约定

| 字段类型 | 建议写法 | 示例 |
|----------|----------|------|
| String | 原样 | `john` |
| 数字 | 数字字符串 | `18`、`3.5` |
| 枚举 | 枚举名（通常大写） | `ACTIVE` |
| 布尔 | 与后端确认（`java.util.Date`? `Boolean`? `Integer`?），MyBatis 侧 Boolean 不会转换 | `true` / `1` |

### 4.4 字段名 → 数据库列名（MyBatis）

MyBatis 链路会把 camelCase 属性名转成**大写下划线**列名，默认开启：

| filter 中的字段名 | 生成的列名 |
|------------------|-----------|
| `userName` | `USER_NAME` |
| `u.userName` | `u.USER_NAME`（保留表别名） |
| `userName`（字段上有 `@TableAlias`） | 按注解的 `value()` / `columnAlias()` 覆盖 |

该行为由系统属性 `COLUMN_NAME_RULE` 控制，默认值为 `CAMEL_TO_UNDERLINE`（即开启转换）；
设为其它值则不转下划线，列名与属性名保持一致。

> JPA 链路**不做**这层转换，`SpecificationHelper` 直接用属性名（`root.get(field)`），并支持 `a.b.c` 形式的嵌套属性路径。

---

## 5. 排序（sorts）

`sorts` 是 `SortVO` 列表的 **JSON 数组字符串**，每个对象含 `columns`（字段数组）与 `direction`：

```json
[
  {"columns":["createTime"],"direction":"DESC"},
  {"columns":["userName"],"direction":"ASC"}
]
```

| 属性 | 说明 |
|------|------|
| `columns` | 字段名数组，**Java 属性名（camelCase）**；多个字段表示同序多列 |
| `direction` | `ASC` / `DESC`（必须大写，反序列化不做大小写兼容） |

多字段：同一对象 `columns` 放多个字段即多列同序；多个对象即多级排序（顺序以数组先后为准）。

> 写 `*?sorts=...` 时记得做 URL 编码（JSON 里的 `"` 需要转义/编码）。
> MyBatis 侧排序列同样经过 §4.4 的列名转换（含表别名与 `@TableAlias` 处理）。

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
| 复杂组合（显式分组） | `[[a, =, 1], AND, [[b, =, 2], OR, [c, =, 3]]]` |
| 空值筛选 | `[[deletedAt, NULL]]` |
| 分组内的隐式 AND | `[[[name, CONTAINS, 张], [status, =, ACTIVE]], OR, [[name, CONTAINS, 李], [status, =, VIP]]]` |

---

## 7. 给 AI / 低代码平台的「提示词」（可直接复制）

> 把下面这段文本作为 system/user 提示词，让 AI 帮你把自然语言查询需求转成正确的 `filter` 字符串。

```
你是一个帮助前端构造后端查询 filter 字符串的助手。后端使用如下 DSL（类 Lisp 嵌套括号表达式）：

【字段】用实体的 Java 属性名（camelCase），如 userName、createTime；表别名用 "别名.属性名"。
       注意：MyBatis 链路会把 camelCase 转成大写下划线列名（userName -> USER_NAME）。
【值】一律写成字符串；java.util.Date 日期用 "yyyy-MM-dd HH:mm:ss"（LocalDate/LocalDateTime 字段须用 ISO 格式）；
      数字直接写数字串；枚举写枚举名；值内不能出现英文逗号 "," 与 "]"。
【操作符】=, != (或<>), >, >=, <, <=, CONTAINS(模糊), LEFT(左模糊), RIGHT(右模糊),
          BETWEEN(2个值), IN(≥1个值), NOT_IN, NULL, NOT_NULL, 逻辑符 AND / OR。
          大小写不敏感。不要用 LEFT/RIGHT（两条 ORM 链路语义相反），统一用 CONTAINS。
【语法】
  1) 单条件: [字段, 操作符, 值1, 值2...]
  2) 组合:   [条件A, AND, 条件B] 或 [条件A, OR, 条件B]，可链式
  3) 省略连接符的多个条件按 AND 合并
  4) 同层混用 AND/OR 时 AND 优先；涉及不同优先级必须用嵌套 [] 显式分组
  5) 整个 filter 最外层必须是一对 []
【排序 sorts】JSON 数组字符串: [{"columns":["createTime"],"direction":"DESC"}]，direction 必须是大写的 ASC/DESC
【传输】filter / sorts 均为字符串，按 query 参数提交: GET /xxx?filter=...&sorts=...&page=1&limit=20

规则：
 - 涉及不同优先级时，必须显式用嵌套 [] 分组，禁止顶层平铺混用 AND/OR。
 - 只输出 filter 字符串与完整请求参数，不要编造不存在的字段。
 - 用户描述需求时，先澄清字段名；字段名未知时提示用户确认。

示例：用户说"用户名包含张三，且状态是 ACTIVE 或 VIP，按创建时间倒序"
输出：
filter = "[[userName, CONTAINS, 张三], AND, [[status, =, ACTIVE], OR, [status, =, VIP]]]"
sorts  = "[{\"columns\":[\"createTime\"],\"direction\":\"DESC\"}]"
page=1, limit=20
```

---

## 8. 调试技巧

### 8.1 常见报错与含义

| 报错 | 触发位置 | 排查方向 |
|------|----------|----------|
| `Failed to parse input: <filter>` | MyBatis `QueryParseHelper#parse`（对所有解析异常的包装） | 看 `cause`：词法/语法/操作符问题都包在它里面 |
| `Invalid filter format: must start with '[' and end with ']'` | `FilterStringParser#parse` | 最外层缺 `[` 或 `]` |
| `Invalid filter format: unmatched '['` | `FilterStringParser#findClosingBracket` | 有 `[` 没配对的 `]` |
| `Unexpected token in filter: <x>` | `FilterParser#build` | 该层出现了既不是条件也不是 AND/OR 的裸 token（常见：把条件写成了裸字符串） |
| `Logical operator 'AND' has no left operand` | `FilterParser#build` | 连接符左侧缺条件（如以 `AND` 开头） |
| `Logical operator has no right operand` | `FilterParser#build` | 连接符右侧缺条件（如以 `AND` 结尾） |
| `Unsupported operator: <OP>` | MyBatis `QueryParseHelper#addCondition` / JPA `buildPredicate` | 操作符写错，或把逻辑符写进了条件位 |
| `BusinessException`（信息为空） | `OperateSymbol#of` | 操作符未定义；空信息是源码现状，需结合调用栈定位 |

### 8.2 静默失败（不报错但没有结果）

优先级最高的排查项，按概率排序：

1. **值格式与字段类型不匹配**：MyBatis 侧 `Long`/`Integer`/`BigDecimal`/`Date` 转换失败会静默转 `null`（见 §4.1）；
2. **`BETWEEN` 值个数不是 2**：MyBatis 侧条件被**静默丢弃**；
3. **同层 AND/OR 优先级**（§2.5）导致语义与预期不符；
4. **字段名不是 Java 属性名**（写成了数据库列名）；MyBatis 侧会把错名直接拼进 SQL，由数据库报 `Unknown column`；
5. **LEFT/RIGHT 方向相反**（§3.1）导致模糊匹配侧反了；
6. 多值条件填了 1 个以上的值却用了单值操作符——多余值会被忽略（`=` 只取第一个值）。

### 8.3 通用检查清单

- 最外层是否成对 `[]`，内层分组是否各自带了 `[]`；
- 是否误用了中文全角标点（`，`、`【`、`】`）；
- 操作符拼写是否正确（参考 §3，大小写不敏感）；
- 联调时先在 §6 速查表挑最接近的场景做最小复现。

---

## 9. 实现说明（解析器上提后的分层）

filter DSL 的解析与执行已分层收敛：

| 层 | 模块 | 职责 |
|---|---|---|
| 词法解析 | `repository`（`FilterStringParser`） | filter 字符串 → 嵌套结构化列表；负责括号配对、逗号/空白分隔 |
| 语法归一律 | `repository`（`FilterParser`） | 结构化列表 → `FilterNode` AST（`ConditionNode` / `LogicalNode` / `GroupNode`）；AND/OR 优先级归一（AND 优先）、多条件默认 AND、`in`/`between` 值写法归一（`[f,in,[a,b]]` 与 `[f,in,a,b]` 等价） |
| MyBatis 翻译层 | `mybatis_repository`（`QueryParseHelper`） | `FilterNode` AST → MyBatis-Plus `QueryWrapper`；列名驼峰转大写下划线、`@TableAlias` 别名、排序翻译 |
| JPA 翻译层 | `jpa_repository`（`SpecificationHelper`） | `FilterNode` AST → JPA `Predicate`；支持 `a.b.c` 嵌套属性路径、按实体字段类型转换 |

新增操作符时：在 `OperateSymbol`（`common` 模块）定义符号，再在两个翻译层各补一个分支，
**解析层（`FilterParser` / `FilterStringParser`）无需改动**。

### 后端侧构造 filter

前端拼接之外的另一种方式（后端代码里避免手写字符串）：`DevQueryVO#addFilters(Triple<String, OperateSymbol, String>...)`
把若干三元组与已有 `filter` 按 AND 合并。详见 `common/README.md`。

### 相关文档

- 后端视角的 DevQueryVO 说明：`mybatis_repository/DevQueryVO用法.md`
- 操作符取值表：`common` 模块的 `OperateSymbol`
- 本文的校验依据：`repository/src/test/.../FilterParserTest.java`、`mybatis_repository/src/test/.../QueryParseHelperTest.java`
