# common 通用模型模块

> 定义全局统一的返回结构、分页查询模型、审计 VO 与安全上下文，是 Web / Repository 层共享的契约。

## 模块说明

`common` 提供跨层共享的通用数据结构与上下文工具，无自动配置。模块仅依赖 `utils`（安全上下文透传所需的 `TransmittableThreadLocal` 即来自该依赖链，透传需配合 TTL 包装的线程池生效）；引入 `core`、`web`、`repository` 等模块时会作为传递依赖间接引入。

## 依赖引入

```xml
<dependency>
    <groupId>org.wymix.framework</groupId>
    <artifactId>common</artifactId>
    <version>${revision}</version>
</dependency>
```

## 配置项

本模块没有外部配置项。

## 集成类

无自动配置，直接以普通类使用。

## 核心类清单（调用入口）

| 类 | 说明 |
|----|------|
| `ResultVO<T>` | 统一返回体：`statusCode` / `payload` / `errorMessage` / `invalidMessage`，静态工厂 `ResultVO.of(...)` |
| `DevQueryVO` | 统一分页查询模型：`filter` / `sorts` / `page` / `limit`；`addFilters(...)` 后端构造查询条件 |
| `OperateSymbol` | filter DSL 操作符号枚举（EQ、GE、CONTAINS 等） |
| `SortVO` | 排序模型（columns + direction） |
| `Page<T>` | 分页结果，支持 `map()` 做 VO 转换 |
| `UserContextHolder` / `Principal` | 静态方法读写当前登录人上下文（`setContext` / `getUserContext` / `clearContext`），基于 `TransmittableThreadLocal` 实现；需配合 TTL 包装的线程池提交任务才能完成异步透传（普通线程池不保证透传） |
| `IgnoreUrlContext` | 白名单 URL 上下文（security/monitor 等模块通过 Bean 汇集放行路径） |
| `BusinessException` / `LoginException` | 业务异常与登录异常基类 |
| `AbstractAuditVO` | VO 层审计字段基类（createdBy/createdTime/lastModifiedBy/lastModifiedTime） |
| `SystemConstant` | 系统常量 |

## 继承类（业务方可继承）

| 类 | 说明 |
|----|------|
| `AbstractAuditVO` | 需要审计字段的 VO 继承它，字段与 `AbstractAuditEntity`（repository 模块）对应 |
| `BusinessException` | 业务异常建议继承或直接抛出，Web 层统一响应Advice 会包装为 `ResultVO` |

## 扩展点

| 扩展点 | 说明 |
|--------|------|
| `IgnoreUrlContext` Bean | 各模块可通过声明该 Bean 追加免认证 URL（如 monitor 自动放行 `/actuator/**`） |
| `DevQueryVO.filter` DSL | 与前端约定的类 Lisp 查询表达式，语法见根目录 `FILTER_USAGE_GUIDE.md` |

## 使用示例

```java
// 1) 统一返回
return ResultVO.of(userVO);                 // 成功
return ResultVO.of(401, "未登录");           // 错误

// 2) 分页结果转换（Web / Service 基类内部使用）
Page<UserVO> voPage = userPage.map(user -> convertToVO(user));

// 3) 获取当前登录人（Principal<AUTHENTICATION> 为泛型类，样例以 String 认证信息为例）
Principal<String> principal = UserContextHolder.getUserContext();
String userId = principal.getPrincipal();

// 4) 构造查询条件（后端方式，避免手写 filter 字符串）
//    Triple 来自 org.apache.commons.lang3.tuple，经 utils 传递引入
DevQueryVO query = new DevQueryVO();
query.addFilters(Triple.of("status", OperateSymbol.EQ, "ACTIVE"),
                 Triple.of("age", OperateSymbol.GE, "18"));
```
